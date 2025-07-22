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

package org.prism_mc.prism.core.storage.adapters.mysql;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariConfig;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
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
import org.prism_mc.prism.loader.services.configuration.storage.MysqlDataSourceConfiguration;
import org.prism_mc.prism.loader.services.configuration.storage.StorageConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class MysqlStorageAdapter extends AbstractSqlStorageAdapter {

    public interface HikariConfigFactory {
        HikariConfig create(StorageConfiguration storageConfiguration);
    }

    private final MysqlDataSourceConfiguration dataSourceConfiguration;

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
    public MysqlStorageAdapter(
        LoggingService loggingService,
        ConfigurationService configurationService,
        ActionTypeRegistry actionRegistry,
        SqlSchemaUpdater schemaUpdater,
        SqlActivityQueryBuilderFactory queryBuilderFactory,
        CacheService cacheService,
        @Named("serializerVersion") short serializerVersion,
        Path dataPath
    ) {
        this(
            loggingService,
            configurationService,
            actionRegistry,
            schemaUpdater,
            queryBuilderFactory,
            cacheService,
            serializerVersion,
            dataPath,
            configurationService.storageConfig().mysql(),
            SQLDialect.MYSQL,
            HikariConfigFactories::mysql
        );
    }

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
     * @param dataSourceConfiguration The data source configuration
     * @param dialect The sql dialect
     * @param factory The hikari config factory
     */
    public MysqlStorageAdapter(
        LoggingService loggingService,
        ConfigurationService configurationService,
        ActionTypeRegistry actionRegistry,
        SqlSchemaUpdater schemaUpdater,
        SqlActivityQueryBuilderFactory queryBuilderFactory,
        CacheService cacheService,
        short serializerVersion,
        Path dataPath,
        MysqlDataSourceConfiguration dataSourceConfiguration,
        SQLDialect dialect,
        HikariConfigFactory factory
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
        this.dataSourceConfiguration = dataSourceConfiguration;

        var hikariConfig = factory.create(configurationService.storageConfig());
        var usingHikariProperties = false;

        try {
            if (hikariPropertiesFile.exists()) {
                loggingService.info("Using hikari.properties");

                hikariConfig = new HikariConfig(hikariPropertiesFile.getPath());
                usingHikariProperties = true;
            }

            if (connect(hikariConfig, dialect)) {
                describeDatabase(hikariConfig, usingHikariProperties);
                prepareSchema();

                if (!dataSourceConfiguration.useStoredProcedures()) {
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

            versionCheck(databaseMetaData);

            var usingStoredProcedures = false;
            if (dataSourceConfiguration.useStoredProcedures()) {
                boolean supportsProcedures = databaseMetaData.supportsStoredProcedures();
                loggingService.info("supports procedures: {0}", supportsProcedures);

                List<String> grants = dslContext.fetch("SHOW GRANTS FOR CURRENT_USER();").into(String.class);
                boolean canCreateRoutines = false;

                for (var grant : grants) {
                    if (
                        grant.contains("CREATE ROUTINE") ||
                        grant.contains("GRANT ALL PRIVILEGES ON *.*") ||
                        grant.contains("GRANT ALL PRIVILEGES ON " + dataSourceConfiguration.database())
                    ) {
                        canCreateRoutines = true;
                        break;
                    }
                }

                loggingService.info("can create routines: {0}", canCreateRoutines);

                usingStoredProcedures =
                    supportsProcedures && canCreateRoutines && dataSourceConfiguration.useStoredProcedures();

                if (!usingStoredProcedures) {
                    dataSourceConfiguration.disallowStoredProcedures();
                }
            }

            loggingService.info("using stored procedures: {0}", usingStoredProcedures);

            Map<String, String> dbVars = dslContext
                .fetch("SHOW VARIABLES")
                .intoMap(r -> r.get(0, String.class), r -> r.get(1, String.class));

            long innodbSizeMb = Long.parseLong(dbVars.get("innodb_buffer_pool_size")) / 1024 / 1024;
            loggingService.info("innodb_buffer_pool_size: {0}MB", innodbSizeMb);
            if (innodbSizeMb < 1024) {
                loggingService.info(
                    "If you encounter row lock issues for very large databases, you should consider increasing innodb_buffer_pool_size."
                );
                loggingService.info("See: https://docs.prism-mc.org/features/purges/#purges-and-databases");
            }

            loggingService.info("sql_mode: {0}", dbVars.get("sql_mode"));

            var waitTimeout = Long.parseLong(dbVars.get("wait_timeout"));
            if (hikariConfig.getMaxLifetime() / 1000 >= waitTimeout) {
                loggingService.info(
                    "Your database server timeout of {0} seconds is shorter than Prism connection timeouts of {1} milliseconds.",
                    waitTimeout,
                    hikariConfig.getMaxLifetime()
                );
                loggingService.info(
                    "You MUST set the maxLifetime setting to a value at least several seconds less than {0} milliseconds.",
                    hikariConfig.getMaxLifetime()
                );
            }

            if (!usingHikariProperties) {
                boolean usrHikariOptimizations = dataSourceConfiguration.useHikariOptimizations();
                loggingService.info("use hikari optimizations: {0}", usrHikariOptimizations);
            }
        }
    }

    @Override
    protected void prepareSchema() throws Exception {
        super.prepareSchema();

        if (dataSourceConfiguration.useStoredProcedures()) {
            try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
                // Drop procedures first because MySQL doesn't support OR REPLACE in CREATE PROCEDURE
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %screate_activity", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_action", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_block", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_cause", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_entity_type", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_item", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_player", prefix));
                stmt.execute(String.format("DROP PROCEDURE IF EXISTS %sget_or_create_world", prefix));

                // Create all procedures
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_create_activity", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_action", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_block", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_cause", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_entity_type", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_item", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_player", prefix));
                stmt.execute(loadSqlFromResourceFile("mysql", "prism_get_or_create_world", prefix));
            }
        }
    }

    /**
     * Logs an error if the database version is unsupported.
     *
     * @param databaseMetaData Database metadata
     */
    protected void versionCheck(DatabaseMetaData databaseMetaData) throws SQLException {
        int majorVersion = databaseMetaData.getDatabaseMajorVersion();

        int patchVersion = 0;
        var segments = databaseMetaData.getDatabaseProductVersion().split("\\.");
        if (segments.length == 3) {
            patchVersion = Integer.parseInt(segments[2].replaceAll("\\D", ""));
        }

        if (majorVersion < 8 || (majorVersion == 8 && patchVersion < 20)) {
            loggingService.warn("Your database version appears to be older than prism supports.");
        }
    }

    @Override
    public ActivityBatch createActivityBatch() {
        if (dataSourceConfiguration.useStoredProcedures()) {
            return new SqlActivityProcedureBatch(loggingService, dataSource, serializerVersion, prefix);
        }

        return super.createActivityBatch();
    }
}
