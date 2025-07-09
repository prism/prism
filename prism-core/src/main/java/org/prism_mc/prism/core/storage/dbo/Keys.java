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

package org.prism_mc.prism.core.storage.dbo;

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIONS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_BLOCKS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_CAUSES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ENTITY_TYPES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_META;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_WORLDS;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.prism_mc.prism.core.storage.dbo.records.PrismActionsRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismActivitiesRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismBlocksRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismCausesRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismEntityTypesRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismItemsRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismMetaRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismPlayersRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismWorldsRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    public static final UniqueKey<PrismActionsRecord> KEY_PRISM_ACTIONS_ACTION = Internal.createUniqueKey(
        PRISM_ACTIONS,
        DSL.name("KEY_prism_actions_action"),
        new TableField[] { PRISM_ACTIONS.ACTION },
        true
    );
    public static final UniqueKey<PrismActionsRecord> KEY_PRISM_ACTIONS_PRIMARY = Internal.createUniqueKey(
        PRISM_ACTIONS,
        DSL.name("KEY_prism_actions_PRIMARY"),
        new TableField[] { PRISM_ACTIONS.ACTION_ID },
        true
    );
    public static final UniqueKey<PrismActivitiesRecord> KEY_PRISM_ACTIVITIES_PRIMARY = Internal.createUniqueKey(
        PRISM_ACTIVITIES,
        DSL.name("KEY_prism_activities_PRIMARY"),
        new TableField[] { PRISM_ACTIVITIES.ACTIVITY_ID },
        true
    );
    public static final UniqueKey<PrismBlocksRecord> KEY_PRISM_BLOCKS_PRIMARY = Internal.createUniqueKey(
        PRISM_BLOCKS,
        DSL.name("KEY_prism_blocks_PRIMARY"),
        new TableField[] { PRISM_BLOCKS.BLOCK_ID },
        true
    );
    public static final UniqueKey<PrismBlocksRecord> KEY_PRISM_BLOCKS_BLOCK = Internal.createUniqueKey(
        PRISM_BLOCKS,
        DSL.name("KEY_prism_blocks_block"),
        new TableField[] { PRISM_BLOCKS.NS, PRISM_BLOCKS.NAME },
        true
    );
    public static final UniqueKey<PrismCausesRecord> KEY_PRISM_CAUSES_CAUSE = Internal.createUniqueKey(
        PRISM_CAUSES,
        DSL.name("KEY_prism_causes_cause"),
        new TableField[] { PRISM_CAUSES.CAUSE },
        true
    );
    public static final UniqueKey<PrismCausesRecord> KEY_PRISM_CAUSES_PRIMARY = Internal.createUniqueKey(
        PRISM_CAUSES,
        DSL.name("KEY_prism_causes_PRIMARY"),
        new TableField[] { PRISM_CAUSES.CAUSE_ID },
        true
    );
    public static final UniqueKey<PrismEntityTypesRecord> KEY_PRISM_ENTITY_TYPES_ENTITYTYPE = Internal.createUniqueKey(
        PRISM_ENTITY_TYPES,
        DSL.name("KEY_prism_entity_types_entityType"),
        new TableField[] { PRISM_ENTITY_TYPES.ENTITY_TYPE },
        true
    );
    public static final UniqueKey<PrismEntityTypesRecord> KEY_PRISM_ENTITY_TYPES_PRIMARY = Internal.createUniqueKey(
        PRISM_ENTITY_TYPES,
        DSL.name("KEY_prism_entity_types_PRIMARY"),
        new TableField[] { PRISM_ENTITY_TYPES.ENTITY_TYPE_ID },
        true
    );
    public static final UniqueKey<PrismItemsRecord> KEY_PRISM_ITEMS_PRIMARY = Internal.createUniqueKey(
        PRISM_ITEMS,
        DSL.name("KEY_prism_items_PRIMARY"),
        new TableField[] { PRISM_ITEMS.ITEM_ID },
        true
    );
    public static final UniqueKey<PrismMetaRecord> KEY_PRISM_META_K = Internal.createUniqueKey(
        PRISM_META,
        DSL.name("KEY_prism_meta_k"),
        new TableField[] { PRISM_META.K },
        true
    );
    public static final UniqueKey<PrismMetaRecord> KEY_PRISM_META_PRIMARY = Internal.createUniqueKey(
        PRISM_META,
        DSL.name("KEY_prism_meta_PRIMARY"),
        new TableField[] { PRISM_META.META_ID },
        true
    );
    public static final UniqueKey<PrismPlayersRecord> KEY_PRISM_PLAYERS_PLAYER_UUID = Internal.createUniqueKey(
        PRISM_PLAYERS,
        DSL.name("KEY_prism_players_player_uuid"),
        new TableField[] { PRISM_PLAYERS.PLAYER_UUID },
        true
    );
    public static final UniqueKey<PrismPlayersRecord> KEY_PRISM_PLAYERS_PRIMARY = Internal.createUniqueKey(
        PRISM_PLAYERS,
        DSL.name("KEY_prism_players_PRIMARY"),
        new TableField[] { PRISM_PLAYERS.PLAYER_ID },
        true
    );
    public static final UniqueKey<PrismWorldsRecord> KEY_PRISM_WORLDS_PRIMARY = Internal.createUniqueKey(
        PRISM_WORLDS,
        DSL.name("KEY_prism_worlds_PRIMARY"),
        new TableField[] { PRISM_WORLDS.WORLD_ID },
        true
    );
    public static final UniqueKey<PrismWorldsRecord> KEY_PRISM_WORLDS_WORLD_UUID = Internal.createUniqueKey(
        PRISM_WORLDS,
        DSL.name("KEY_prism_worlds_world_uuid"),
        new TableField[] { PRISM_WORLDS.WORLD_UUID },
        true
    );

    public static final ForeignKey<PrismActivitiesRecord, PrismActionsRecord> ACTIONID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("actionId"),
        new TableField[] { PRISM_ACTIVITIES.ACTION_ID },
        Keys.KEY_PRISM_ACTIONS_PRIMARY,
        new TableField[] { PRISM_ACTIONS.ACTION_ID },
        true
    );
    public static final ForeignKey<PrismActivitiesRecord, PrismCausesRecord> AFFECTEDPLAYERID =
        Internal.createForeignKey(
            PRISM_ACTIVITIES,
            DSL.name("affectedPlayerId"),
            new TableField[] { PRISM_ACTIVITIES.AFFECTED_PLAYER_ID },
            Keys.KEY_PRISM_PLAYERS_PRIMARY,
            new TableField[] { PRISM_PLAYERS.PLAYER_ID },
            true
        );
    public static final ForeignKey<PrismActivitiesRecord, PrismCausesRecord> CAUSEID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("causeId"),
        new TableField[] { PRISM_ACTIVITIES.CAUSE_ID },
        Keys.KEY_PRISM_CAUSES_PRIMARY,
        new TableField[] { PRISM_CAUSES.CAUSE_ID },
        true
    );
    public static final ForeignKey<PrismActivitiesRecord, PrismCausesRecord> CAUSEPLAYERID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("causePlayerId"),
        new TableField[] { PRISM_ACTIVITIES.CAUSE_PLAYER_ID },
        Keys.KEY_PRISM_PLAYERS_PRIMARY,
        new TableField[] { PRISM_PLAYERS.PLAYER_ID },
        true
    );
    public static final ForeignKey<PrismActivitiesRecord, PrismCausesRecord> CAUSEENTITYTYPEID =
        Internal.createForeignKey(
            PRISM_ACTIVITIES,
            DSL.name("causeEntityTypeId"),
            new TableField[] { PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID },
            Keys.KEY_PRISM_ENTITY_TYPES_PRIMARY,
            new TableField[] { PRISM_ENTITY_TYPES.ENTITY_TYPE_ID },
            true
        );
    public static final ForeignKey<PrismActivitiesRecord, PrismCausesRecord> CAUSEBLOCKID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("causeEntityTypeId"),
        new TableField[] { PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID },
        Keys.KEY_PRISM_ENTITY_TYPES_PRIMARY,
        new TableField[] { PRISM_ENTITY_TYPES.ENTITY_TYPE_ID },
        true
    );
    public static final ForeignKey<PrismActivitiesRecord, PrismEntityTypesRecord> ENTITYTYPEID =
        Internal.createForeignKey(
            PRISM_ACTIVITIES,
            DSL.name("entityTypeId"),
            new TableField[] { PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID },
            Keys.KEY_PRISM_ENTITY_TYPES_PRIMARY,
            new TableField[] { PRISM_ENTITY_TYPES.ENTITY_TYPE_ID },
            true
        );
    public static final ForeignKey<PrismActivitiesRecord, PrismItemsRecord> ITEMID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("itemId"),
        new TableField[] { PRISM_ACTIVITIES.AFFECTED_ITEM_ID },
        Keys.KEY_PRISM_ITEMS_PRIMARY,
        new TableField[] { PRISM_ITEMS.ITEM_ID },
        true
    );
    public static final ForeignKey<PrismActivitiesRecord, PrismBlocksRecord> BLOCKID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("blockId"),
        new TableField[] { PRISM_ACTIVITIES.AFFECTED_BLOCK_ID },
        Keys.KEY_PRISM_BLOCKS_PRIMARY,
        new TableField[] { PRISM_BLOCKS.BLOCK_ID },
        true
    );
    public static final ForeignKey<PrismActivitiesRecord, PrismBlocksRecord> REPLACEDBLOCKID =
        Internal.createForeignKey(
            PRISM_ACTIVITIES,
            DSL.name("replacedBlockId"),
            new TableField[] { PRISM_ACTIVITIES.REPLACED_BLOCK_ID },
            Keys.KEY_PRISM_BLOCKS_PRIMARY,
            new TableField[] { PRISM_BLOCKS.BLOCK_ID },
            true
        );
    public static final ForeignKey<PrismActivitiesRecord, PrismWorldsRecord> WORLDID = Internal.createForeignKey(
        PRISM_ACTIVITIES,
        DSL.name("worldId"),
        new TableField[] { PRISM_ACTIVITIES.WORLD_ID },
        Keys.KEY_PRISM_WORLDS_PRIMARY,
        new TableField[] { PRISM_WORLDS.WORLD_ID },
        true
    );
}
