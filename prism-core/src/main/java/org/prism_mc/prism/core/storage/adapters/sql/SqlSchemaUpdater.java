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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_META;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.prism_mc.prism.core.storage.dbo.Indexes;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class SqlSchemaUpdater {

    /**
     * The current/latest schema version for fresh installs.
     */
    public static final String CURRENT_SCHEMA_VERSION = "401";

    /**
     * The logger.
     */
    private final LoggingService loggingService;

    /**
     * Construct the updater.
     *
     * @param loggingService The logging service
     */
    @Inject
    public SqlSchemaUpdater(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    /**
     * Apply all necessary schema updates based on the current schema version.
     *
     * @param dslContext The DSL context
     * @param schemaVersion The current schema version
     */
    public void update(DSLContext dslContext, String schemaVersion) {
        if ("400".equals(schemaVersion)) {
            update400To401(dslContext);
            schemaVersion = "401";
        }
    }

    /**
     * Update schema from 400 to 401.
     *
     * @param dslContext The DSL context
     */
    private void update400To401(DSLContext dslContext) {
        loggingService.info("Updating schema from 400 to 401...");

        dslContext.transaction(tx -> {
            DSLContext txCtx = tx.dsl();

            // Drop the incorrectly created index
            txCtx.dropIndex(Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID).on(PRISM_ACTIVITIES).execute();

            // Recreate the index on the correct column
            txCtx
                .createIndex(Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.REPLACED_BLOCK_ID)
                .execute();

            // Update the schema version
            txCtx.update(PRISM_META).set(PRISM_META.V, "401").where(PRISM_META.K.eq("schema_ver")).execute();
        });

        loggingService.info("Schema updated to 401.");
    }
}
