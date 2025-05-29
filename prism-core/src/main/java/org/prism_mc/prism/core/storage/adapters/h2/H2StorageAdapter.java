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

package org.prism_mc.prism.core.storage.adapters.h2;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jooq.SQLDialect;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.core.injection.factories.FileSqlActivityQueryBuilderFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.core.storage.HikariConfigFactories;
import org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter;
import org.prism_mc.prism.core.storage.adapters.sql.SqlSchemaUpdater;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class H2StorageAdapter extends AbstractSqlStorageAdapter {

    /**
     * Constructor.
     *
     * @param loggingService The logging service
     * @param configurationService The configuration service
     * @param actionRegistry The action type registry
     * @param schemaUpdater The schema updater
     * @param queryBuilderFactory The query builder factory
     * @param serializerVersion The serializer version
     */
    @Inject
    public H2StorageAdapter(
        LoggingService loggingService,
        ConfigurationService configurationService,
        ActionTypeRegistry actionRegistry,
        SqlSchemaUpdater schemaUpdater,
        FileSqlActivityQueryBuilderFactory queryBuilderFactory,
        CacheService cacheService,
        @Named("serializerVersion") short serializerVersion,
        Path dataPath
    ) {
        super(
            loggingService,
            configurationService,
            actionRegistry,
            schemaUpdater,
            null,
            cacheService,
            serializerVersion,
            dataPath
        );
        try {
            var configuredPath = configurationService.storageConfig().h2().path();
            var databaseFilename = String.format("%s.db", configurationService.storageConfig().h2().database());
            var dbFilePath = dataPath.resolve(Paths.get(configuredPath)).normalize();

            dbFilePath.toFile().mkdirs();

            if (
                connect(
                    HikariConfigFactories.h2(
                        configurationService.storageConfig(),
                        dbFilePath.resolve(databaseFilename).toFile()
                    ),
                    SQLDialect.H2
                )
            ) {
                this.queryBuilder = queryBuilderFactory.create(dslContext);

                prepareSchema();

                ready = true;
            }
        } catch (Exception e) {
            loggingService.handleException(e);
        }
    }

    @Override
    protected void prepareSchema() throws Exception {
        dslContext.createSchemaIfNotExists(configurationService.storageConfig().h2().database()).execute();

        super.prepareSchema();
    }
}
