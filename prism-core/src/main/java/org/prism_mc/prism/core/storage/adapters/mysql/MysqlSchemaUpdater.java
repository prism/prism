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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIONS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_BLOCKS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_CAUSES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ENTITY_TYPES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_META;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_WORLDS;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Table;
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
    protected void update400To401(DSLContext dslContext, Map<String, List<String>> existingIndexes) {
        loggingService.info("Updating schema from 400 to 401...");

        List<String> activitiesIndexes = existingIndexes.getOrDefault(PRISM_ACTIVITIES.getName(), new ArrayList<>());

        // Combine drop and create index operations into a single ALTER TABLE
        // statement with ALGORITHM=INPLACE for better performance on large tables
        List<String> clauses = new ArrayList<>();

        if (activitiesIndexes.contains(Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID.getName())) {
            clauses.add(String.format("DROP INDEX `%s`", Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID.getName()));
        }

        if (activitiesIndexes.contains(Indexes.PRISM_ACTIVITIES_COORDINATE_400.getName())) {
            clauses.add(String.format("DROP INDEX `%s`", Indexes.PRISM_ACTIVITIES_COORDINATE_400.getName()));
        }

        clauses.add(
            String.format(
                "ADD INDEX `%s` (`%s`)",
                Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID.getName(),
                PRISM_ACTIVITIES.REPLACED_BLOCK_ID.getName()
            )
        );

        if (!activitiesIndexes.contains(Indexes.PRISM_ACTIVITIES_WORLD_ACTION_TIME_COORDS.getName())) {
            clauses.add(
                String.format(
                    "ADD INDEX `%s` (`%s`, `%s`, `%s`, `%s`, `%s`, `%s`)",
                    Indexes.PRISM_ACTIVITIES_WORLD_ACTION_TIME_COORDS.getName(),
                    PRISM_ACTIVITIES.WORLD_ID.getName(),
                    PRISM_ACTIVITIES.ACTION_ID.getName(),
                    PRISM_ACTIVITIES.X.getName(),
                    PRISM_ACTIVITIES.Y.getName(),
                    PRISM_ACTIVITIES.Z.getName(),
                    PRISM_ACTIVITIES.TIMESTAMP.getName()
                )
            );
        }

        if (!activitiesIndexes.contains(Indexes.PRISM_ACTIVITIES_WORLD_TIME_COORDS.getName())) {
            clauses.add(
                String.format(
                    "ADD INDEX `%s` (`%s`, `%s`, `%s`, `%s`, `%s`)",
                    Indexes.PRISM_ACTIVITIES_WORLD_TIME_COORDS.getName(),
                    PRISM_ACTIVITIES.WORLD_ID.getName(),
                    PRISM_ACTIVITIES.X.getName(),
                    PRISM_ACTIVITIES.Y.getName(),
                    PRISM_ACTIVITIES.Z.getName(),
                    PRISM_ACTIVITIES.TIMESTAMP.getName()
                )
            );
        }

        String sql = String.format(
            "ALTER TABLE `%s` %s, ALGORITHM=INPLACE",
            PRISM_ACTIVITIES.getName(),
            String.join(", ", clauses)
        );

        dslContext.execute(sql);

        // Drop the world id index. This can only be done when modifications to the composite are done
        // as world_id must be first in another index to satisfy mysql fk rules
        if (activitiesIndexes.contains(Indexes.PRISM_ACTIVITIES_WORLDID.getName())) {
            dslContext.dropIndex(Indexes.PRISM_ACTIVITIES_WORLDID).on(PRISM_ACTIVITIES).execute();
        }

        update400To401Shared(dslContext, existingIndexes);
    }

    @Override
    protected void update401To402(DSLContext dslContext) {
        loggingService.info("Updating schema from 401 to 402...");

        Table<?>[] tables = {
            PRISM_ACTIONS,
            PRISM_ACTIVITIES,
            PRISM_BLOCKS,
            PRISM_CAUSES,
            PRISM_ENTITY_TYPES,
            PRISM_ITEMS,
            PRISM_META,
            PRISM_PLAYERS,
            PRISM_WORLDS,
        };

        for (Table<?> table : tables) {
            dslContext.execute(
                String.format(
                    "ALTER TABLE `%s` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                    table.getName()
                )
            );
        }

        dslContext.execute(
            String.format("ALTER TABLE `%s` MODIFY COLUMN `serialized_data` LONGTEXT", PRISM_ACTIVITIES.getName())
        );

        update401To402Shared(dslContext);
    }
}
