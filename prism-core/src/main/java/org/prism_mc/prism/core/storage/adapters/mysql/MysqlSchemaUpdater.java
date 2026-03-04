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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_META;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jooq.DSLContext;
import org.prism_mc.prism.core.storage.adapters.sql.SqlSchemaUpdater;
import org.prism_mc.prism.core.storage.dbo.Indexes;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class MysqlSchemaUpdater extends SqlSchemaUpdater {

    /**
     * Construct the updater.
     *
     * @param loggingService The logging service
     */
    @Inject
    public MysqlSchemaUpdater(LoggingService loggingService) {
        super(loggingService);
    }

    @Override
    protected void update400To401(DSLContext dslContext) {
        loggingService.info("Updating schema from 400 to 401...");

        // Combine drop and create index operations into a single ALTER TABLE
        // statement with ALGORITHM=INPLACE for better performance on large tables
        String sql = String.format(
            "ALTER TABLE `%s` " +
            "DROP INDEX `%s`, " +
            "DROP INDEX `%s`, " +
            "ADD INDEX `%s` (`%s`), " +
            "ADD INDEX `%s` (`%s`, `%s`, `%s`, `%s`, `%s`, `%s`), " +
            "ADD INDEX `%s` (`%s`, `%s`, `%s`, `%s`, `%s`), " +
            "ALGORITHM=INPLACE",
            PRISM_ACTIVITIES.getName(),
            // Drop indexes
            Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID.getName(),
            Indexes.PRISM_ACTIVITIES_COORDINATE_400.getName(),
            // Replaced block index
            Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID.getName(),
            PRISM_ACTIVITIES.REPLACED_BLOCK_ID.getName(),
            // New composite index with action
            Indexes.PRISM_ACTIVITIES_WORLD_ACTION_TIME_COORDS.getName(),
            PRISM_ACTIVITIES.WORLD_ID.getName(),
            PRISM_ACTIVITIES.ACTION_ID.getName(),
            PRISM_ACTIVITIES.TIMESTAMP.getName(),
            PRISM_ACTIVITIES.X.getName(),
            PRISM_ACTIVITIES.Y.getName(),
            PRISM_ACTIVITIES.Z.getName(),
            // New composite index without action
            Indexes.PRISM_ACTIVITIES_WORLD_TIME_COORDS.getName(),
            PRISM_ACTIVITIES.WORLD_ID.getName(),
            PRISM_ACTIVITIES.TIMESTAMP.getName(),
            PRISM_ACTIVITIES.X.getName(),
            PRISM_ACTIVITIES.Y.getName(),
            PRISM_ACTIVITIES.Z.getName()
        );

        dslContext.execute(sql);

        // Drop the world id index. This can only be done when modifications to the composite are done
        // as world_id must be first in another index to satisfy mysql fk rules
        dslContext.dropIndex(Indexes.PRISM_ACTIVITIES_WORLDID).on(PRISM_ACTIVITIES).execute();

        // Create the new player name index
        dslContext.createIndex(Indexes.PRISM_PLAYERS_PLAYER).on(PRISM_PLAYERS, PRISM_PLAYERS.PLAYER).execute();

        // Update the schema version
        dslContext.update(PRISM_META).set(PRISM_META.V, "401").where(PRISM_META.K.eq("schema_ver")).execute();

        loggingService.info("Schema updated to 401.");
    }
}
