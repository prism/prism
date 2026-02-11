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

package org.prism_mc.prism.core.storage.adapters.mariadb;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.jooq.SQLDialect;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.core.injection.factories.SqlActivityQueryBuilderFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.core.storage.HikariConfigFactories;
import org.prism_mc.prism.core.storage.adapters.mysql.MysqlStorageAdapter;
import org.prism_mc.prism.core.storage.adapters.sql.SqlSchemaUpdater;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class MariaDbStorageAdapter extends MysqlStorageAdapter {

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
    public MariaDbStorageAdapter(
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
            dataPath,
            configurationService.storageConfig().mariadb(),
            SQLDialect.MARIADB,
            HikariConfigFactories::mariadb
        );
    }

    @Override
    protected void versionCheck(DatabaseMetaData databaseMetaData) throws SQLException {
        int majorVersion = databaseMetaData.getDatabaseMajorVersion();
        int minorVersion = databaseMetaData.getDatabaseMinorVersion();

        if (majorVersion < 10 || (majorVersion == 10 && minorVersion < 7)) {
            loggingService.warn("Your database version appears to be older than prism supports.");
            loggingService.info("Reported database product version: {0}", databaseMetaData.getDatabaseProductVersion());
            loggingService.info("We think your database version is {0}.{1}", majorVersion, minorVersion);
        }
    }
}
