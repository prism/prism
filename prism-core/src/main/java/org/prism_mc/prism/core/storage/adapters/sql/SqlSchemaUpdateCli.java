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

package org.prism_mc.prism.core.storage.adapters.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.prism_mc.prism.core.storage.HikariConfigFactories;
import org.prism_mc.prism.core.storage.adapters.mysql.MysqlSchemaUpdater;
import org.prism_mc.prism.core.storage.dbo.DefaultCatalog;
import org.prism_mc.prism.core.storage.dbo.PrismDatabase;
import org.prism_mc.prism.core.storage.dbo.tables.PrismActivities;
import org.prism_mc.prism.core.storage.dbo.tables.PrismMeta;
import org.prism_mc.prism.core.storage.dbo.tables.PrismPlayers;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.storage.StorageConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.loader.services.schema.SchemaUpdateCli;
import org.prism_mc.prism.loader.storage.StorageType;

/**
 * Standalone CLI runner for database schema updates.
 */
public class SqlSchemaUpdateCli implements SchemaUpdateCli {

    @Override
    public void run(ConfigurationService configService, LoggingService loggingService, Path dataPath) throws Exception {
        StorageConfiguration storageConfig = configService.storageConfig();
        StorageType storageType = storageConfig.primaryStorageType();
        String prefix = storageConfig.primaryDataSource().prefix();

        loggingService.info("Storage type: {0}", storageType);
        loggingService.info("Table prefix: {0}", prefix);

        // Initialize DBO static fields required by SqlSchemaUpdater
        initializeDbo(storageConfig, prefix);

        // Suppress jOOQ startup messages
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        // Set the context classloader so JDBC DriverManager can find
        // relocated drivers loaded via JarInJarClassLoader
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        // Load JDBC drivers
        HikariConfigFactories.loadDriver(storageType);

        // Build HikariCP config (with P6Spy disabled — it requires Log4J
        // which is only available on the Minecraft server)
        HikariConfig hikariConfig = buildHikariConfig(storageConfig, storageType, dataPath);

        // Connect
        loggingService.info("Connecting to database...");
        SQLDialect dialect = resolveDialect(storageType);

        try (HikariDataSource dataSource = new HikariDataSource(hikariConfig)) {
            DSLContext dslContext = DSL.using(dataSource, dialect);

            // For Postgres, set the schema
            if (storageType == StorageType.POSTGRES) {
                String schema = storageConfig.postgres().schema();
                dslContext.setSchema(schema).execute();
            }

            // Query current schema version
            String schemaVersion = dslContext
                .select(AbstractSqlStorageAdapter.PRISM_META.V)
                .from(AbstractSqlStorageAdapter.PRISM_META)
                .where(AbstractSqlStorageAdapter.PRISM_META.K.eq("schema_ver"))
                .fetchOne(AbstractSqlStorageAdapter.PRISM_META.V);

            if (schemaVersion == null) {
                loggingService.info("No existing schema found. The schema will be created on first server startup.");

                return;
            }

            loggingService.info("Current schema version: {0}", schemaVersion);

            if (schemaVersion.equals(SqlSchemaUpdater.CURRENT_SCHEMA_VERSION)) {
                loggingService.info("Schema is already up to date.");

                return;
            }

            // Run updates (use DB-specific updater when available)
            SqlSchemaUpdater updater = (storageType == StorageType.MYSQL || storageType == StorageType.MARIADB)
                ? new MysqlSchemaUpdater(loggingService)
                : new SqlSchemaUpdater(loggingService);
            updater.update(dslContext, schemaVersion);

            loggingService.info("Schema updated to {0}.", SqlSchemaUpdater.CURRENT_SCHEMA_VERSION);
        }
    }

    /**
     * Initialize the static DBO fields on AbstractSqlStorageAdapter.
     *
     * @param storageConfig The storage configuration
     * @param prefix The table prefix
     */
    private void initializeDbo(StorageConfiguration storageConfig, String prefix) {
        AbstractSqlStorageAdapter.PRISM_META = new PrismMeta(prefix);
        AbstractSqlStorageAdapter.PRISM_ACTIVITIES = new PrismActivities(prefix);
        AbstractSqlStorageAdapter.PRISM_PLAYERS = new PrismPlayers(prefix);

        var catalog = new DefaultCatalog(storageConfig.primaryDataSource().catalog());
        AbstractSqlStorageAdapter.PRISM_DATABASE = new PrismDatabase(
            catalog,
            storageConfig.primaryDataSource().schema(),
            java.util.List.of(AbstractSqlStorageAdapter.PRISM_META, AbstractSqlStorageAdapter.PRISM_ACTIVITIES)
        );
    }

    /**
     * Build a HikariConfig for the given storage type using the shared
     * factories with P6Spy disabled.
     *
     * @param storageConfig The storage configuration
     * @param storageType The storage type
     * @param dataPath The data path
     * @return The HikariConfig
     */
    private HikariConfig buildHikariConfig(StorageConfiguration storageConfig, StorageType storageType, Path dataPath) {
        return switch (storageType) {
            case SQLITE -> {
                var configuredPath = storageConfig.sqlite().path();
                var databaseFilename = storageConfig.sqlite().database() + ".db";
                var dbFile = dataPath.resolve(Paths.get(configuredPath)).normalize().resolve(databaseFilename).toFile();
                yield HikariConfigFactories.sqlite(storageConfig, dbFile, false);
            }
            case H2 -> {
                var configuredPath = storageConfig.h2().path();
                var databaseFilename = storageConfig.h2().database() + ".db";
                var dbFile = dataPath.resolve(Paths.get(configuredPath)).normalize().resolve(databaseFilename).toFile();
                yield HikariConfigFactories.h2(storageConfig, dbFile, false);
            }
            case MYSQL -> HikariConfigFactories.mysql(storageConfig, false);
            case MARIADB -> HikariConfigFactories.mariadb(storageConfig, false);
            case POSTGRES -> HikariConfigFactories.postgres(storageConfig, false);
        };
    }

    /**
     * Resolve the jOOQ SQLDialect for a storage type.
     *
     * @param storageType The storage type
     * @return The SQLDialect
     */
    private SQLDialect resolveDialect(StorageType storageType) {
        return switch (storageType) {
            case SQLITE -> SQLDialect.SQLITE;
            case H2 -> SQLDialect.H2;
            case MYSQL -> SQLDialect.MYSQL;
            case MARIADB -> SQLDialect.MARIADB;
            case POSTGRES -> SQLDialect.POSTGRES;
        };
    }
}
