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

package org.prism_mc.prism.core.storage;

import com.zaxxer.hikari.HikariConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import lombok.experimental.UtilityClass;
import org.prism_mc.prism.loader.services.configuration.storage.SqlDataSourceConfiguration;
import org.prism_mc.prism.loader.services.configuration.storage.StorageConfiguration;
import org.prism_mc.prism.loader.storage.StorageType;

@UtilityClass
public class HikariConfigFactories {

    /**
     * Create a hikari configuration for H2 databases.
     *
     * @param storageConfiguration The storage configuration
     * @return The hikari configuration
     */
    public static HikariConfig h2(StorageConfiguration storageConfiguration, File h2File) {
        HikariConfig hikariConfig = createSharedConfig(storageConfiguration);

        hikariConfig.setJdbcUrl(
            "jdbc:" +
            (storageConfiguration.spy() ? "p6spy:" : "") +
            "h2:file:" +
            h2File.getAbsolutePath() +
            ";MODE=mysql;" +
            "DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;"
        );

        return hikariConfig;
    }

    /**
     * Load drivers for a given storage type.
     *
     * @param storageType The storage type
     */
    public static void loadDriver(StorageType storageType) {
        switch (storageType) {
            case H2 -> tryDriverClassNames("org.h2.Driver");
            case MARIADB -> tryDriverClassNames("org.mariadb.jdbc.Driver");
            case MYSQL -> tryDriverClassNames("org.mysql.jdbc.Driver");
            case POSTGRES -> tryDriverClassNames("org.postgresql.Driver");
            default -> {
                // ignored
            }
        }
    }

    /**
     * Create a hikari configuration for MariaDB databases.
     *
     * @param storageConfiguration The storage configuration
     * @return The hikari configuration
     */
    public static HikariConfig mariadb(StorageConfiguration storageConfiguration) {
        HikariConfig hikariConfig = createSharedConfig(storageConfiguration);

        String host = storageConfiguration.mariadb().host();
        String port = storageConfiguration.mariadb().port();
        String database = storageConfiguration.mariadb().database();
        boolean useSpy = storageConfiguration.spy();

        loadDriver(StorageType.MARIADB);

        hikariConfig.setJdbcUrl(
            "jdbc:" + (useSpy ? "p6spy:" : "") + String.format("mariadb://%s:%s/%s", host, port, database)
        );
        hikariConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        return hikariConfig;
    }

    /**
     * Create a hikari configuration for MySQL databases.
     *
     * @param storageConfiguration The storage configuration
     * @return The hikari configuration
     */
    public static HikariConfig mysql(StorageConfiguration storageConfiguration) {
        HikariConfig hikariConfig = createSharedConfig(storageConfiguration);

        String host = storageConfiguration.mysql().host();
        String port = storageConfiguration.mysql().port();
        String database = storageConfiguration.mysql().database();
        boolean useSpy = storageConfiguration.spy();

        hikariConfig.setJdbcUrl(
            "jdbc:" + (useSpy ? "p6spy:" : "") + String.format("mysql://%s:%s/%s", host, port, database)
        );

        if (storageConfiguration.mysql().useHikariOptimizations()) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", true);
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", 250);
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
            hikariConfig.addDataSourceProperty("useServerPrepStmts", true);
            hikariConfig.addDataSourceProperty("cacheCallableStmts", true);
            hikariConfig.addDataSourceProperty("cacheResultSetMetadata", true);
            hikariConfig.addDataSourceProperty("cacheServerConfiguration", true);
            hikariConfig.addDataSourceProperty("useLocalSessionState", true);
            hikariConfig.addDataSourceProperty("elideSetAutoCommits", true);
            hikariConfig.addDataSourceProperty("alwaysSendSetIsolation", false);
        }

        hikariConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        return hikariConfig;
    }

    /**
     * Create a hikari configuration for Postgres databases.
     *
     * @param storageConfiguration The storage configuration
     * @return The hikari configuration
     */
    public static HikariConfig postgres(StorageConfiguration storageConfiguration) {
        HikariConfig hikariConfig = createSharedConfig(storageConfiguration);

        String host = storageConfiguration.postgres().host();
        String port = storageConfiguration.postgres().port();
        String database = storageConfiguration.postgres().database();
        boolean useSpy = storageConfiguration.spy();

        hikariConfig.setJdbcUrl(
            "jdbc:" + (useSpy ? "p6spy:" : "") + String.format("postgresql://%s:%s/%s", host, port, database)
        );

        return hikariConfig;
    }

    /**
     * Create a hikari configuration for sqlite databases.
     *
     * @param storageConfiguration The storage configuration
     * @return The hikari configuration
     */
    public static HikariConfig sqlite(StorageConfiguration storageConfiguration, File sqliteFile) {
        HikariConfig hikariConfig = createSharedConfig(storageConfiguration);
        hikariConfig.setConnectionInitSql(
            String.format(
                "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=%d;",
                storageConfiguration.sqlite().busyTimeout()
            )
        );

        hikariConfig.setJdbcUrl(
            "jdbc:" + (storageConfiguration.spy() ? "p6spy:" : "") + "sqlite:file:" + sqliteFile.getAbsolutePath()
        );

        return hikariConfig;
    }

    /**
     * Create a hikari config with common settings.
     *
     * @param storageConfiguration The storage configuration
     * @return The hikari config
     */
    private static HikariConfig createSharedConfig(StorageConfiguration storageConfiguration) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("prism");

        if (storageConfiguration.primaryDataSource() instanceof SqlDataSourceConfiguration sqlConfig) {
            if (sqlConfig.username() != null) {
                hikariConfig.setUsername(sqlConfig.username());
            }

            if (sqlConfig.password() != null) {
                hikariConfig.setPassword(sqlConfig.password());
            }
        }

        if (storageConfiguration.spy()) {
            hikariConfig.setDriverClassName("com.p6spy.engine.spy.P6SpyDriver");
        }

        return hikariConfig;
    }

    /**
     * Try to find an available driver class name.
     *
     * @param driverClassNames The class names to try
     */
    private static void tryDriverClassNames(String... driverClassNames) {
        for (String driverClassName : driverClassNames) {
            try {
                Class.forName(driverClassName).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
