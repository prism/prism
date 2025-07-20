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

import static org.jooq.impl.DSL.avg;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.min;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.AFFECTED_PLAYERS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.CAUSE_BLOCKS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.CAUSE_BLOCKS_TRANSLATION_KEY;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.CAUSE_ENTITY_TYPES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.CAUSE_ENTITY_TYPES_TRANSLATION_KEY;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIONS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_BLOCKS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_CAUSES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ENTITY_TYPES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_WORLDS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.REPLACED_BLOCKS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.REPLACED_BLOCKS_TRANSLATION_KEY;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteQuery;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.jooq.types.UInteger;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.util.Pair;
import org.prism_mc.prism.core.storage.dbo.records.PrismActivitiesRecord;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.storage.StorageConfiguration;

public class SqlActivityQueryBuilder {

    /**
     * The configuration service.
     */
    protected final ConfigurationService configurationService;

    /**
     * The storage configuration.
     */
    protected final StorageConfiguration storageConfiguration;

    /**
     * The dsl context.
     */
    protected final DSLContext dslContext;

    /**
     * Construct a new query builder.
     *
     * @param configurationService The configuration service
     * @param dslContext The DSL context
     */
    @Inject
    public SqlActivityQueryBuilder(ConfigurationService configurationService, @Assisted DSLContext dslContext) {
        this.configurationService = configurationService;
        storageConfiguration = configurationService.storageConfig();
        this.dslContext = dslContext;
    }

    /**
     * Delete records from the activities table.
     *
     * @param query The query
     * @param cycleMinPrimaryKey The min primary key
     * @param cycleMaxPrimaryKey The max primary key
     * @return The number of rows deleted
     */
    public int deleteActivities(ActivityQuery query, int cycleMinPrimaryKey, int cycleMaxPrimaryKey) {
        DeleteQuery<PrismActivitiesRecord> queryBuilder = dslContext.deleteQuery(PRISM_ACTIVITIES);

        if (!query.actionTypes().isEmpty() || !query.actionTypeKeys().isEmpty()) {
            queryBuilder.addUsing(PRISM_ACTIONS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.ACTION_ID.equal(PRISM_ACTIONS.ACTION_ID));
        }

        // Items
        if (!query.affectedMaterials().isEmpty()) {
            queryBuilder.addUsing(PRISM_ITEMS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.AFFECTED_ITEM_ID.equal(PRISM_ITEMS.ITEM_ID));
        }

        // Affected Blocks
        if (!query.affectedBlocks().isEmpty()) {
            queryBuilder.addUsing(PRISM_BLOCKS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.AFFECTED_BLOCK_ID.equal(PRISM_BLOCKS.BLOCK_ID));
        }

        // Cause Blocks
        if (!query.causeBlocks().isEmpty()) {
            queryBuilder.addUsing(CAUSE_BLOCKS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.CAUSE_BLOCK_ID.equal(CAUSE_BLOCKS.BLOCK_ID));
        }

        // Affected Entity Types
        if (!query.affectedEntityTypes().isEmpty()) {
            queryBuilder.addUsing(PRISM_ENTITY_TYPES);
            queryBuilder.addConditions(
                PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID.equal(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
            );
        }

        // Cause Entity Types
        if (!query.causeEntityTypes().isEmpty()) {
            queryBuilder.addUsing(CAUSE_ENTITY_TYPES);
            queryBuilder.addConditions(PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID.equal(CAUSE_ENTITY_TYPES.ENTITY_TYPE_ID));
        }

        // Affected Players
        if (!query.affectedPlayerNames().isEmpty()) {
            queryBuilder.addUsing(AFFECTED_PLAYERS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.AFFECTED_PLAYER_ID.equal(AFFECTED_PLAYERS.PLAYER_ID));
        }

        // Cause Players
        if (!query.causePlayerNames().isEmpty()) {
            queryBuilder.addUsing(PRISM_PLAYERS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.CAUSE_PLAYER_ID.equal(PRISM_PLAYERS.PLAYER_ID));
        }

        if (query.namedCause() != null) {
            queryBuilder.addUsing(PRISM_CAUSES);
            queryBuilder.addConditions(PRISM_ACTIVITIES.CAUSE_ID.equal(PRISM_CAUSES.CAUSE_ID));
        }

        if (query.worldUuid() != null) {
            queryBuilder.addUsing(PRISM_WORLDS);
            queryBuilder.addConditions(PRISM_ACTIVITIES.WORLD_ID.equal(PRISM_WORLDS.WORLD_ID));
        }

        // Add conditions
        queryBuilder.addConditions(conditions(query));

        // Limit
        queryBuilder.addConditions(
            PRISM_ACTIVITIES.ACTIVITY_ID.between(
                UInteger.valueOf(cycleMinPrimaryKey),
                UInteger.valueOf(cycleMaxPrimaryKey)
            )
        );

        return queryBuilder.execute();
    }

    /**
     * Query the activities table with a given activity query.
     *
     * @param query The activity query
     * @return A list of DbRow results
     */
    public Result<Record> queryActivities(ActivityQuery query) {
        SelectQuery<Record> queryBuilder = dslContext.selectQuery();

        // Add fields useful for all query types
        queryBuilder.addSelect(
            PRISM_WORLDS.WORLD_UUID,
            PRISM_WORLDS.WORLD,
            PRISM_ITEMS.MATERIAL,
            coalesce(PRISM_ACTIVITIES.AFFECTED_ITEM_QUANTITY, DSL.val(0)),
            PRISM_ITEMS.DATA,
            PRISM_BLOCKS.NS,
            PRISM_BLOCKS.NAME,
            PRISM_BLOCKS.TRANSLATION_KEY,
            PRISM_ENTITY_TYPES.ENTITY_TYPE,
            PRISM_ACTIONS.ACTION,
            PRISM_PLAYERS.PLAYER_UUID,
            PRISM_PLAYERS.PLAYER,
            PRISM_ACTIVITIES.DESCRIPTOR
        );

        // Add fields useful only for lookups
        if (query.lookup()) {
            queryBuilder.addSelect(PRISM_ACTIVITIES.METADATA);
            queryBuilder.addSelect(AFFECTED_PLAYERS.PLAYER);
            queryBuilder.addSelect(AFFECTED_PLAYERS.PLAYER_UUID);
            queryBuilder.addSelect(CAUSE_ENTITY_TYPES_TRANSLATION_KEY);
            queryBuilder.addSelect(CAUSE_BLOCKS_TRANSLATION_KEY);
            queryBuilder.addSelect(PRISM_CAUSES.CAUSE);
            queryBuilder.addSelect(count().over().as("totalrows"));
        }

        if (query.grouped()) {
            // Add fields for grouped queries
            queryBuilder.addSelect(
                avg(PRISM_ACTIVITIES.X),
                avg(PRISM_ACTIVITIES.Y),
                avg(PRISM_ACTIVITIES.Z),
                avg(PRISM_ACTIVITIES.TIMESTAMP),
                count().as("groupcount")
            );
        } else {
            // Add fields for non-grouped queries
            queryBuilder.addSelect(
                PRISM_ACTIVITIES.ACTIVITY_ID,
                PRISM_ACTIVITIES.TIMESTAMP,
                PRISM_ACTIVITIES.X,
                PRISM_ACTIVITIES.Y,
                PRISM_ACTIVITIES.Z
            );
        }

        // Add fields only needed for modifications
        if (query.modification()) {
            queryBuilder.addSelect(
                PRISM_BLOCKS.DATA,
                PRISM_ACTIVITIES.SERIALIZED_DATA,
                coalesce(PRISM_ACTIVITIES.SERIALIZER_VERSION, 1).as("serializer_version"),
                REPLACED_BLOCKS.NS,
                REPLACED_BLOCKS.NAME,
                REPLACED_BLOCKS.DATA,
                REPLACED_BLOCKS_TRANSLATION_KEY
            );
        }

        queryBuilder.addFrom(PRISM_ACTIVITIES);

        joins(queryBuilder, query);

        if (query.modification()) {
            queryBuilder.addJoin(
                REPLACED_BLOCKS,
                JoinType.LEFT_OUTER_JOIN,
                REPLACED_BLOCKS.BLOCK_ID.equal(PRISM_ACTIVITIES.REPLACED_BLOCK_ID)
            );
        }

        queryBuilder.addConditions(conditions(query));

        if (query.grouped()) {
            queryBuilder.addGroupBy(
                PRISM_ACTIONS.ACTION,
                PRISM_WORLDS.WORLD_UUID,
                PRISM_WORLDS.WORLD,
                PRISM_ACTIVITIES.ACTION_ID,
                PRISM_ITEMS.MATERIAL,
                PRISM_ACTIVITIES.AFFECTED_ITEM_QUANTITY,
                PRISM_ITEMS.DATA,
                PRISM_BLOCKS.NS,
                PRISM_BLOCKS.NAME,
                PRISM_BLOCKS.TRANSLATION_KEY,
                PRISM_ENTITY_TYPES.ENTITY_TYPE,
                AFFECTED_PLAYERS.PLAYER,
                AFFECTED_PLAYERS.PLAYER_UUID,
                PRISM_CAUSES.CAUSE,
                PRISM_PLAYERS.PLAYER,
                PRISM_PLAYERS.PLAYER_UUID,
                PRISM_ACTIVITIES.DESCRIPTOR,
                PRISM_ACTIVITIES.METADATA,
                CAUSE_ENTITY_TYPES_TRANSLATION_KEY,
                CAUSE_BLOCKS_TRANSLATION_KEY
            );
        }

        // Order by
        if (query.lookup() && query.grouped()) {
            if (query.sort().equals(ActivityQuery.Sort.ASCENDING)) {
                queryBuilder.addOrderBy(avg(PRISM_ACTIVITIES.TIMESTAMP).asc());
            } else {
                queryBuilder.addOrderBy(avg(PRISM_ACTIVITIES.TIMESTAMP).desc());
            }
        } else {
            if (query.sort().equals(ActivityQuery.Sort.ASCENDING)) {
                queryBuilder.addOrderBy(PRISM_ACTIVITIES.TIMESTAMP.asc());
            } else {
                queryBuilder.addOrderBy(PRISM_ACTIVITIES.TIMESTAMP.desc());
            }
        }

        if (query.modification()) {
            // Most rollbacks "build up" but some hanging blocks need to be "built down" or they just break.
            // In order to do this, we tell hanging blocks to sort *after* everything else,
            // then we sort everything by `y asc` and sort these hanging blocks by `y desc`.
            // cave_vines are sorted to come after cave_vines_plant so the plant is rebuilt first.
            queryBuilder.addOrderBy(
                DSL.decode().when(PRISM_BLOCKS.NAME.in("cave_vines", "weeping_vines"), 1).else_(-1).asc()
            );
            queryBuilder.addOrderBy(
                DSL.decode().when(PRISM_BLOCKS.NAME.in("cave_vines_plant", "weeping_vines_plant"), 1).else_(-1).asc()
            );
            queryBuilder.addOrderBy(
                DSL.decode().when(PRISM_BLOCKS.NAME.in("vine", "pointed_dripstone"), 1).else_(-1).asc()
            );

            queryBuilder.addOrderBy(PRISM_ACTIVITIES.X.asc());
            queryBuilder.addOrderBy(PRISM_ACTIVITIES.Z.asc());

            List<String> blocksToBuildUp = List.of(
                "pointed_dripstone",
                "cave_vines_plant",
                "weeping_vines_plant",
                "vine"
            );

            queryBuilder.addOrderBy(
                DSL.decode().when(PRISM_BLOCKS.NAME.in(blocksToBuildUp), PRISM_ACTIVITIES.Y).desc()
            );

            queryBuilder.addOrderBy(
                DSL.decode().when(PRISM_BLOCKS.NAME.notIn(blocksToBuildUp), PRISM_ACTIVITIES.Y).asc()
            );
        }

        // Limits
        if (query.limit() > 0) {
            queryBuilder.addLimit(query.offset(), query.limit());
        }

        return queryBuilder.fetch();
    }

    /**
     * Query the primary key bounds for the given conditions.
     *
     * @param query The query
     * @return The min/max primary key
     */
    public Pair<Integer, Integer> queryActivitiesPkBounds(ActivityQuery query) {
        var queryBuilder = dslContext.selectQuery();

        queryBuilder.addSelect(
            coalesce(min(PRISM_ACTIVITIES.ACTIVITY_ID), DSL.val(0)),
            coalesce(max(PRISM_ACTIVITIES.ACTIVITY_ID), DSL.val(0))
        );

        queryBuilder.addFrom(PRISM_ACTIVITIES);

        joins(queryBuilder, query);

        queryBuilder.addConditions(conditions(query));

        var result = queryBuilder.fetchOne();
        int minPk = result != null ? result.get(0, UInteger.class).intValue() : 0;
        int maxPk = result != null ? result.get(1, UInteger.class).intValue() : 0;

        return new Pair<>(minPk, maxPk);
    }

    /**
     * A convenience method to add all joins needed for a lookup.
     *
     * @param queryBuilder Query builder
     * @param query Activity Query
     */
    protected void joins(SelectQuery<Record> queryBuilder, ActivityQuery query) {
        queryBuilder.addJoin(PRISM_ACTIONS, PRISM_ACTIONS.ACTION_ID.equal(PRISM_ACTIVITIES.ACTION_ID));

        queryBuilder.addJoin(PRISM_WORLDS, PRISM_WORLDS.WORLD_ID.equal(PRISM_ACTIVITIES.WORLD_ID));

        // Items
        queryBuilder.addJoin(
            PRISM_ITEMS,
            JoinType.LEFT_OUTER_JOIN,
            PRISM_ITEMS.ITEM_ID.equal(PRISM_ACTIVITIES.AFFECTED_ITEM_ID)
        );

        // Affected Entity Types
        queryBuilder.addJoin(
            PRISM_ENTITY_TYPES,
            JoinType.LEFT_OUTER_JOIN,
            PRISM_ENTITY_TYPES.ENTITY_TYPE_ID.equal(PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID)
        );

        // Cause Entity Types
        if (query.lookup() || !query.causeEntityTypes().isEmpty()) {
            queryBuilder.addJoin(
                CAUSE_ENTITY_TYPES,
                JoinType.LEFT_OUTER_JOIN,
                CAUSE_ENTITY_TYPES.ENTITY_TYPE_ID.equal(PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID)
            );
        }

        // Affected Blocks
        queryBuilder.addJoin(
            PRISM_BLOCKS,
            JoinType.LEFT_OUTER_JOIN,
            PRISM_BLOCKS.BLOCK_ID.equal(PRISM_ACTIVITIES.AFFECTED_BLOCK_ID)
        );

        // Cause Blocks
        if (query.lookup() || !query.causeBlocks().isEmpty()) {
            queryBuilder.addJoin(
                CAUSE_BLOCKS,
                JoinType.LEFT_OUTER_JOIN,
                CAUSE_BLOCKS.BLOCK_ID.equal(PRISM_ACTIVITIES.CAUSE_BLOCK_ID)
            );
        }

        // Affected Players
        if (query.lookup() || !query.affectedPlayerNames().isEmpty()) {
            queryBuilder.addJoin(
                AFFECTED_PLAYERS,
                JoinType.LEFT_OUTER_JOIN,
                AFFECTED_PLAYERS.PLAYER_ID.equal(PRISM_ACTIVITIES.AFFECTED_PLAYER_ID)
            );
        }

        // Cause Players
        queryBuilder.addJoin(
            PRISM_PLAYERS,
            JoinType.LEFT_OUTER_JOIN,
            PRISM_PLAYERS.PLAYER_ID.equal(PRISM_ACTIVITIES.CAUSE_PLAYER_ID)
        );

        // Named Causes
        if (query.lookup() || query.namedCause() != null) {
            queryBuilder.addJoin(
                PRISM_CAUSES,
                JoinType.LEFT_OUTER_JOIN,
                PRISM_CAUSES.CAUSE_ID.equal(PRISM_ACTIVITIES.CAUSE_ID)
            );
        }
    }

    /**
     * Get all conditions for the query.
     *
     * @param query The query
     * @return All conditions
     */
    protected List<Condition> conditions(ActivityQuery query) {
        List<Condition> conditions = new ArrayList<>();

        // Action Types
        if (!query.actionTypes().isEmpty()) {
            List<String> actionTypeKeys = new ArrayList<>();
            for (var actionType : query.actionTypes()) {
                actionTypeKeys.add(actionType.key());
            }

            conditions.add(PRISM_ACTIONS.ACTION.in(actionTypeKeys));
        }

        // Action Type Keys
        if (!query.actionTypeKeys().isEmpty()) {
            conditions.add(PRISM_ACTIONS.ACTION.in(query.actionTypeKeys()));
        }

        // Activity IDs
        if (query.activityIds() != null && !query.activityIds().isEmpty()) {
            conditions.add(PRISM_ACTIVITIES.ACTIVITY_ID.in(query.activityIds()));
        }

        // Affected Blocks
        if (!query.affectedBlocks().isEmpty()) {
            conditions.add(PRISM_BLOCKS.NAME.in(query.affectedBlocks()));
        }

        // Cause Blocks
        if (!query.causeBlocks().isEmpty()) {
            conditions.add(CAUSE_BLOCKS.NAME.in(query.causeBlocks()));
        }

        // Affected Entity Types
        if (!query.affectedEntityTypes().isEmpty()) {
            conditions.add(PRISM_ENTITY_TYPES.ENTITY_TYPE.in(query.affectedEntityTypes()));
        }

        // Cause Entity Types
        if (!query.causeEntityTypes().isEmpty()) {
            conditions.add(CAUSE_ENTITY_TYPES.ENTITY_TYPE.in(query.causeEntityTypes()));
        }

        // Named Causes
        if (query.namedCause() != null) {
            conditions.add(PRISM_CAUSES.CAUSE.equal(query.namedCause()));
        }

        // Locations
        if (query.coordinate() != null) {
            conditions.add(PRISM_ACTIVITIES.X.equal(query.coordinate().intX()));
            conditions.add(PRISM_ACTIVITIES.Y.equal(query.coordinate().intY()));
            conditions.add(PRISM_ACTIVITIES.Z.equal(query.coordinate().intZ()));
        } else if (query.minCoordinate() != null && query.maxCoordinate() != null) {
            conditions.add(PRISM_ACTIVITIES.X.between(query.minCoordinate().intX(), query.maxCoordinate().intX()));
            conditions.add(PRISM_ACTIVITIES.Y.between(query.minCoordinate().intY(), query.maxCoordinate().intY()));
            conditions.add(PRISM_ACTIVITIES.Z.between(query.minCoordinate().intZ(), query.maxCoordinate().intZ()));
        }

        // Materials
        if (!query.affectedMaterials().isEmpty()) {
            conditions.add(PRISM_ITEMS.MATERIAL.in(query.affectedMaterials()));
        }

        // Players
        if (!query.affectedPlayerNames().isEmpty() && !query.causePlayerNames().isEmpty()) {
            conditions.add(
                AFFECTED_PLAYERS.PLAYER.in(query.affectedPlayerNames()).or(
                    PRISM_PLAYERS.PLAYER.in(query.causePlayerNames())
                )
            );
        } else if (!query.affectedPlayerNames().isEmpty()) {
            conditions.add(AFFECTED_PLAYERS.PLAYER.in(query.affectedPlayerNames()));
        } else if (!query.causePlayerNames().isEmpty()) {
            conditions.add(PRISM_PLAYERS.PLAYER.in(query.causePlayerNames()));
        }

        // Query
        if (query.descriptor() != null) {
            conditions.add(PRISM_ACTIVITIES.DESCRIPTOR.likeIgnoreCase(String.format("%%%s%%", query.descriptor())));
        }

        // Reversed
        if (query.reversed() != null) {
            conditions.add(PRISM_ACTIVITIES.REVERSED.eq(query.reversed()));
        }

        // Timestamps
        if (query.after() != null && query.before() != null) {
            conditions.add(
                PRISM_ACTIVITIES.TIMESTAMP.between(UInteger.valueOf(query.after()), UInteger.valueOf(query.before()))
            );
        } else if (query.after() != null) {
            conditions.add(PRISM_ACTIVITIES.TIMESTAMP.greaterThan(UInteger.valueOf(query.after())));
        } else if (query.before() != null) {
            conditions.add(PRISM_ACTIVITIES.TIMESTAMP.lessThan(UInteger.valueOf(query.before())));
        }

        // World
        if (query.worldUuid() != null) {
            conditions.add(PRISM_WORLDS.WORLD_UUID.equal(query.worldUuid().toString()));
        }

        return conditions;
    }
}
