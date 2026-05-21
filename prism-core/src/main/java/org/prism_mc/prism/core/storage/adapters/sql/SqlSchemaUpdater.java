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
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_META;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.Table;
import org.prism_mc.prism.core.storage.dbo.Indexes;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class SqlSchemaUpdater {

    /**
     * The current/latest schema version for fresh installations.
     */
    public static final String CURRENT_SCHEMA_VERSION = "402";

    /**
     * The logger.
     */
    protected final LoggingService loggingService;

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
     * @param existingIndexes Existing index names keyed by table name, used to keep migrations idempotent
     */
    public void update(DSLContext dslContext, String schemaVersion, Map<String, List<String>> existingIndexes) {
        if ("400".equals(schemaVersion)) {
            update400To401(dslContext, existingIndexes);
            schemaVersion = "401";
        }

        if ("401".equals(schemaVersion)) {
            update401To402(dslContext);
            schemaVersion = "402";
        }
    }

    /**
     * Update schema from 400 to 401.
     *
     * @param dslContext The DSL context
     * @param existingIndexes Existing index names keyed by table name
     */
    protected void update400To401(DSLContext dslContext, Map<String, List<String>> existingIndexes) {
        loggingService.info("Updating schema from 400 to 401...");

        List<String> activitiesIndexes = existingIndexes.getOrDefault(PRISM_ACTIVITIES.getName(), new ArrayList<>());

        // Drop the old indexes
        dropIndexIfExists(dslContext, activitiesIndexes, Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID, PRISM_ACTIVITIES);
        dropIndexIfExists(dslContext, activitiesIndexes, Indexes.PRISM_ACTIVITIES_COORDINATE_400, PRISM_ACTIVITIES);
        dropIndexIfExists(dslContext, activitiesIndexes, Indexes.PRISM_ACTIVITIES_WORLDID, PRISM_ACTIVITIES);

        // Recreate the replaced-block index on the correct column
        createIndexIfNotExists(
            dslContext,
            activitiesIndexes,
            Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID,
            PRISM_ACTIVITIES,
            PRISM_ACTIVITIES.REPLACED_BLOCK_ID
        );

        // Create the new composite index
        createIndexIfNotExists(
            dslContext,
            activitiesIndexes,
            Indexes.PRISM_ACTIVITIES_WORLD_ACTION_TIME_COORDS,
            PRISM_ACTIVITIES,
            PRISM_ACTIVITIES.WORLD_ID,
            PRISM_ACTIVITIES.ACTION_ID,
            PRISM_ACTIVITIES.X,
            PRISM_ACTIVITIES.Y,
            PRISM_ACTIVITIES.Z,
            PRISM_ACTIVITIES.TIMESTAMP
        );

        // Create the new composite index
        createIndexIfNotExists(
            dslContext,
            activitiesIndexes,
            Indexes.PRISM_ACTIVITIES_WORLD_TIME_COORDS,
            PRISM_ACTIVITIES,
            PRISM_ACTIVITIES.WORLD_ID,
            PRISM_ACTIVITIES.X,
            PRISM_ACTIVITIES.Y,
            PRISM_ACTIVITIES.Z,
            PRISM_ACTIVITIES.TIMESTAMP
        );

        update400To401Shared(dslContext, existingIndexes);
    }

    /**
     * Update schema from 401 to 402.
     *
     * @param dslContext The DSL context
     */
    protected void update401To402(DSLContext dslContext) {
        loggingService.info("Updating schema from 401 to 402...");
        update401To402Shared(dslContext);
    }

    /**
     * Shared logic updating the schema from 4oo to 401.
     *
     * @param dslContext - The DSL context
     * @param existingIndexes Existing index names keyed by table name
     */
    protected void update400To401Shared(DSLContext dslContext, Map<String, List<String>> existingIndexes) {
        // Create the new player name index
        createIndexIfNotExists(
            dslContext,
            existingIndexes.getOrDefault(PRISM_PLAYERS.getName(), new ArrayList<>()),
            Indexes.PRISM_PLAYERS_PLAYER,
            PRISM_PLAYERS,
            PRISM_PLAYERS.PLAYER
        );

        // Create the new item index
        createIndexIfNotExists(
            dslContext,
            existingIndexes.getOrDefault(PRISM_ITEMS.getName(), new ArrayList<>()),
            Indexes.PRISM_ITEMS_MATERIAL,
            PRISM_ITEMS,
            PRISM_ITEMS.MATERIAL
        );

        // Update the schema version
        dslContext.update(PRISM_META).set(PRISM_META.V, "401").where(PRISM_META.K.eq("schema_ver")).execute();

        loggingService.info("Schema updated to 401.");
    }

    /**
     * Drop an index only if the database currently reports it as present.
     *
     * <p>The provided index list is mutated to reflect the drop so that subsequent existence
     * checks in the same migration see the live state.</p>
     *
     * @param dslContext The DSL context
     * @param existingIndexes The mutable list of existing index names for the table
     * @param index The index to drop
     * @param table The table the index belongs to
     */
    protected void dropIndexIfExists(DSLContext dslContext, List<String> existingIndexes, Index index, Table<?> table) {
        if (existingIndexes.contains(index.getName())) {
            dslContext.dropIndex(index).on(table).execute();
            existingIndexes.remove(index.getName());
        }
    }

    /**
     * Create an index only if the database does not already report it as present.
     *
     * <p>The provided index list is mutated to reflect the creation so that subsequent existence
     * checks in the same migration see the live state.</p>
     *
     * @param dslContext The DSL context
     * @param existingIndexes The mutable list of existing index names for the table
     * @param index The index to create
     * @param table The table the index belongs to
     * @param fields The columns the index covers
     */
    protected void createIndexIfNotExists(
        DSLContext dslContext,
        List<String> existingIndexes,
        Index index,
        Table<?> table,
        Field<?>... fields
    ) {
        if (!existingIndexes.contains(index.getName())) {
            dslContext.createIndex(index).on(table, fields).execute();
            existingIndexes.add(index.getName());
        }
    }

    /**
     * Shared logic for the 401 to 402 update — just bumps the recorded schema version.
     *
     * @param dslContext The DSL context
     */
    protected void update401To402Shared(DSLContext dslContext) {
        dslContext.update(PRISM_META).set(PRISM_META.V, "402").where(PRISM_META.K.eq("schema_ver")).execute();

        loggingService.info("Schema updated to 402.");
    }
}
