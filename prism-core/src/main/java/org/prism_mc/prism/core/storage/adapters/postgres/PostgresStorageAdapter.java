/*
 * prism
 *
 * Copyright (c) 2022 M Botsko (viveleroi)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.prism_mc.prism.core.storage.adapters.postgres;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariConfig;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.jooq.SQLDialect;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.api.storage.ActivityBatch;
import org.prism_mc.prism.core.injection.factories.SqlActivityQueryBuilderFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.core.storage.HikariConfigFactories;
import org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter;
import org.prism_mc.prism.core.storage.adapters.sql.SqlActivityProcedureBatch;
import org.prism_mc.prism.core.storage.adapters.sql.SqlSchemaUpdater;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class PostgresStorageAdapter extends AbstractSqlStorageAdapter {

    /**
     * The schema/table prefix.
     */
    private String prefix;

    /**
     * Constructor.
     *
     * @param loggingService The logging service
     * @param configurationService The configuration service
     * @param actionRegistry The action type registry
     * @param schemaUpdater The schema updater
     * @param cacheService The cache service
     * @param queryBuilderFactory The query builder factory
     * @param serializerVersion The serializer version
     * @param dataPath The plugin file path
     */
    @Inject
    public PostgresStorageAdapter(
        LoggingService loggingService,
        ConfigurationService configurationService,
        ActionTypeRegistry actionRegistry,
        SqlSchemaUpdater schemaUpdater,
        SqlActivityQueryBuilderFactory queryBuilderFactory,
        CacheService cacheService,
        @Named("serializerVersion") short serializerVersion,
        Path dataPath
    ) {
        super(
            loggingService,
            configurationService,
            actionRegistry,
            schemaUpdater,
            queryBuilderFactory,
            cacheService,
            serializerVersion,
            dataPath
        );
        try {
            prefix = configurationService.storageConfig().postgres().prefix();

            var hikariConfig = HikariConfigFactories.postgres(configurationService.storageConfig());
            var usingHikariProperties = false;

            if (hikariPropertiesFile.exists()) {
                loggingService.info("Using hikari.properties");

                hikariConfig = new HikariConfig(hikariPropertiesFile.getPath());
                usingHikariProperties = true;
            }

            if (connect(hikariConfig, SQLDialect.POSTGRES)) {
                describeDatabase(hikariConfig, usingHikariProperties);
                prepareSchema();

                if (!configurationService.storageConfig().postgres().useStoredProcedures()) {
                    prepareCache();
                }

                ready = true;
            }
        } catch (Exception e) {
            loggingService.handleException(e);
        }
    }

    @Override
    protected void describeDatabase(HikariConfig hikariConfig, boolean usingHikariProperties) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            String databaseProduct = databaseMetaData.getDatabaseProductName();
            String databaseVersion = databaseMetaData.getDatabaseProductVersion();

            loggingService.info("Database: {0} {1}", databaseProduct, databaseVersion);
            loggingService.info(
                "JDBC Version: {0}.{1}",
                databaseMetaData.getJDBCMajorVersion(),
                databaseMetaData.getJDBCMinorVersion()
            );

            var usingStoredProcedures = false;
            if (configurationService.storageConfig().postgres().useStoredProcedures()) {
                boolean supportsProcedures = databaseMetaData.supportsStoredProcedures();
                loggingService.info("supports procedures: {0}", supportsProcedures);

                var canCreateFunctions = dslContext
                    .fetchSingle("SELECT bool_or(has_schema_privilege(oid, 'CREATE')) FROM pg_catalog.pg_namespace;")
                    .into(Boolean.class);
                loggingService.info("can create functions: {0}", canCreateFunctions);

                usingStoredProcedures =
                    supportsProcedures &&
                    canCreateFunctions &&
                    configurationService.storageConfig().postgres().useStoredProcedures();

                if (!usingStoredProcedures) {
                    configurationService.storageConfig().postgres().disallowStoredProcedures();
                }
            }

            loggingService.info("using stored procedures: {0}", usingStoredProcedures);
        }
    }

    @Override
    protected void prepareSchema() throws Exception {
        dslContext.setSchema(configurationService.storageConfig().postgres().schema()).execute();

        super.prepareSchema();

        if (configurationService.storageConfig().postgres().useStoredProcedures()) {
            try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
                // Drop procedures just in case the parameters change, if so OR REPLACE won't work
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %screate_activity", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_action", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_block", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_cause", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_entity_type", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_item", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_player", prefix));
                stmt.execute(String.format("DROP FUNCTION IF EXISTS %sget_or_create_world", prefix));

                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_action", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_block", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_cause", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_entity_type", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_item", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_player", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_get_or_create_world", prefix));
                stmt.execute(loadSqlFromResourceFile("postgres", "prism_create_activity", prefix));
            }
        }
    }

    @Override
    public ActivityBatch createActivityBatch() {
        if (configurationService.storageConfig().postgres().useStoredProcedures()) {
            return new SqlActivityProcedureBatch(loggingService, dataSource, serializerVersion, prefix);
        }

        return super.createActivityBatch();
    }
}
