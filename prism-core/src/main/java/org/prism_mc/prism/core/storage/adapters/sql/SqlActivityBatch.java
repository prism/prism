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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIONS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_BLOCKS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_CAUSES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ENTITY_TYPES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_WORLDS;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;
import org.prism_mc.prism.api.actions.BlockAction;
import org.prism_mc.prism.api.actions.CustomData;
import org.prism_mc.prism.api.actions.EntityAction;
import org.prism_mc.prism.api.actions.ItemAction;
import org.prism_mc.prism.api.actions.PlayerAction;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.containers.BlockContainer;
import org.prism_mc.prism.api.containers.EntityContainer;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.api.storage.ActivityBatch;
import org.prism_mc.prism.api.util.TextUtils;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.core.storage.dbo.records.PrismActivitiesRecord;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class SqlActivityBatch implements ActivityBatch {

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The serializer version.
     */
    protected final short serializerVersion;

    /**
     * The cache service.
     */
    protected final CacheService cacheService;

    /**
     * The dsl context.
     */
    protected final DSLContext dslContext;

    /**
     * An array of records to batch insert.
     */
    private List<PrismActivitiesRecord> records = new ArrayList<>();

    /**
     * Construct a new batch handler.
     *
     * @param loggingService The logging service
     * @param dslContext The DSL context
     * @param serializerVersion The serializer version
     * @param cacheService The cache service
     */
    public SqlActivityBatch(
        LoggingService loggingService,
        DSLContext dslContext,
        short serializerVersion,
        CacheService cacheService
    ) {
        this.loggingService = loggingService;
        this.dslContext = dslContext;
        this.serializerVersion = serializerVersion;
        this.cacheService = cacheService;
    }

    @Override
    public void startBatch() {
        records = new ArrayList<>();
    }

    @Override
    public void add(Activity activity) throws SQLException {
        var record = dslContext.newRecord(PRISM_ACTIVITIES);

        record.setTimestamp(UInteger.valueOf(activity.timestamp() / 1000));
        record.setX(activity.coordinate().intX());
        record.setY(activity.coordinate().intY());
        record.setZ(activity.coordinate().intZ());

        // Set the action relationship
        record.setActionId(UInteger.valueOf(getOrCreateActionId(activity.action().type().key())));

        // Set the entity relationship
        if (activity.action() instanceof EntityAction entityAction) {
            var entityTypeId = UInteger.valueOf(
                getOrCreateEntityTypeId(
                    entityAction.entityContainer().serializeEntityType(),
                    entityAction.entityContainer().translationKey()
                )
            );
            record.setEntityTypeId(entityTypeId);
        }

        // Set the item relationship
        if (activity.action() instanceof ItemAction itemAction) {
            record.setItemId(
                UInteger.valueOf(getOrCreateItemId(itemAction.serializeMaterial(), itemAction.serializeItemData()))
            );
            record.setItemQuantity(UShort.valueOf(itemAction.quantity()));
        }

        // Set the block relationship
        UInteger blockId = null;
        if (activity.action() instanceof BlockAction blockAction) {
            blockId = UInteger.valueOf(
                getOrCreateBlockId(
                    blockAction.blockContainer().blockNamespace(),
                    blockAction.blockContainer().blockName(),
                    blockAction.blockContainer().serializeBlockData(),
                    blockAction.blockContainer().translationKey()
                )
            );

            record.setBlockId(blockId);

            if (blockAction.replacedBlockContainer() != null) {
                record.setReplacedBlockId(
                    UInteger.valueOf(
                        getOrCreateBlockId(
                            blockAction.replacedBlockContainer().blockNamespace(),
                            blockAction.replacedBlockContainer().blockName(),
                            blockAction.replacedBlockContainer().serializeBlockData(),
                            blockAction.replacedBlockContainer().translationKey()
                        )
                    )
                );
            }
        }

        // Set the world relationship
        record.setWorldId(UInteger.valueOf(getOrCreateWorldId(activity.world().key(), activity.world().value())));

        // Set the affected player relationship
        if (activity.action() instanceof PlayerAction playerAction) {
            record.setAffectedPlayerId(
                UInteger.valueOf(
                    getOrCreatePlayerId(playerAction.playerContainer().uuid(), playerAction.playerContainer().name())
                )
            );
        }

        // Set the cause
        if (activity.cause().container() instanceof PlayerContainer playerContainer) {
            record.setCausePlayerId(
                UInteger.valueOf(getOrCreatePlayerId(playerContainer.uuid(), playerContainer.name()))
            );
        } else if (activity.cause().container() instanceof BlockContainer blockContainer) {
            record.setCauseBlockId(
                UInteger.valueOf(
                    getOrCreateBlockId(
                        blockContainer.blockNamespace(),
                        blockContainer.blockName(),
                        blockContainer.serializeBlockData(),
                        blockContainer.translationKey()
                    )
                )
            );
        } else if (activity.cause().container() instanceof EntityContainer entityContainer) {
            record.setCauseEntityTypeId(
                UInteger.valueOf(
                    getOrCreateEntityTypeId(entityContainer.serializeEntityType(), entityContainer.translationKey())
                )
            );
        } else if (activity.cause().container() instanceof StringContainer stringContainer) {
            record.setCauseId(UInteger.valueOf(getOrCreateCauseId(stringContainer.value())));
        }

        // Set the descriptor
        record.setDescriptor(TextUtils.truncateWithEllipsis(activity.action().descriptor(), 255));

        // Serialize the metadata
        if (activity.action().metadata() != null) {
            try {
                record.setMetadata(activity.action().serializeMetadata());
            } catch (Exception e) {
                loggingService.handleException(e);
            }
        }

        if (activity.action() instanceof CustomData customDataAction) {
            if (customDataAction.hasCustomData()) {
                record.setSerializerVersion(UShort.valueOf(serializerVersion));
                record.setSerializedData(customDataAction.serializeCustomData());
            }
        }

        records.add(record);
    }

    /**
     * Get or create the action record and return the primary key.
     *
     * @param actionKey The action key
     * @return The primary key
     * @throws SQLException The database exception
     */
    private int getOrCreateActionId(String actionKey) throws SQLException {
        Integer actionKeyPk = cacheService.actionKeyPkMap().getIfPresent(actionKey);
        if (actionKeyPk != null) {
            return actionKeyPk;
        }

        int primaryKey;

        // Select any existing record
        UInteger intPk = dslContext
            .select(PRISM_ACTIONS.ACTION_ID)
            .from(PRISM_ACTIONS)
            .where(PRISM_ACTIONS.ACTION.equal(actionKey))
            .fetchOne(PRISM_ACTIONS.ACTION_ID);

        if (intPk != null) {
            primaryKey = intPk.intValue();
        } else {
            // Create the record
            intPk = dslContext
                .insertInto(PRISM_ACTIONS, PRISM_ACTIONS.ACTION)
                .values(actionKey)
                .returningResult(PRISM_ACTIONS.ACTION_ID)
                .fetchOne(PRISM_ACTIONS.ACTION_ID);

            if (intPk != null) {
                primaryKey = intPk.intValue();
            } else {
                throw new SQLException(
                    String.format("Failed to get or create an action record. Action: %s", actionKey)
                );
            }
        }

        cacheService.actionKeyPkMap().put(actionKey, primaryKey);

        return primaryKey;
    }

    /**
     * Get or create the block data record and return the primary key.
     *
     * @param blockData The block data
     * @return The primary key
     * @throws SQLException The database exception
     */
    private int getOrCreateBlockId(String namespace, String name, String blockData, String translationKey)
        throws SQLException {
        String blockKey = namespace + ":" + name + (blockData == null ? "" : blockData);
        Integer blockPk = cacheService.blockDataPkMap().getIfPresent(blockKey);
        if (blockPk != null) {
            return blockPk;
        }

        int primaryKey;

        // Select the existing block
        UInteger intPk = dslContext
            .select(PRISM_BLOCKS.BLOCK_ID)
            .from(PRISM_BLOCKS)
            .where(PRISM_BLOCKS.NS.equal(namespace), PRISM_BLOCKS.NAME.equal(name), PRISM_BLOCKS.DATA.equal(blockData))
            .fetchOne(PRISM_BLOCKS.BLOCK_ID);

        if (intPk != null) {
            primaryKey = intPk.intValue();
        } else {
            // Create the record
            intPk = dslContext
                .insertInto(
                    PRISM_BLOCKS,
                    PRISM_BLOCKS.NS,
                    PRISM_BLOCKS.NAME,
                    PRISM_BLOCKS.DATA,
                    PRISM_BLOCKS.TRANSLATION_KEY
                )
                .values(namespace, name, blockData, translationKey)
                .returningResult(PRISM_BLOCKS.BLOCK_ID)
                .fetchOne(PRISM_BLOCKS.BLOCK_ID);

            if (intPk != null) {
                primaryKey = intPk.intValue();
            } else {
                throw new SQLException(
                    String.format("Failed to get or create a block record. Block: %s:%s %s", namespace, name, blockData)
                );
            }
        }

        cacheService.blockDataPkMap().put(blockKey, primaryKey);

        return primaryKey;
    }

    /**
     * Get or create the cause record and return the primary key.
     *
     * @param causeName The cause name
     * @return The primary key
     * @throws SQLException The database exception
     */
    private long getOrCreateCauseId(String causeName) throws SQLException {
        Long causePk = cacheService.namedCausePkMap().getIfPresent(causeName);
        if (causePk != null) {
            return causePk;
        }

        long primaryKey;

        UInteger intPk = dslContext
            .select(PRISM_CAUSES.CAUSE_ID)
            .from(PRISM_CAUSES)
            .where(PRISM_CAUSES.CAUSE.equal(causeName))
            .fetchOne(PRISM_CAUSES.CAUSE_ID);

        if (intPk != null) {
            primaryKey = intPk.longValue();
        } else {
            // Create the record
            intPk = dslContext
                .insertInto(PRISM_CAUSES)
                .set(PRISM_CAUSES.CAUSE, causeName)
                .returningResult(PRISM_CAUSES.CAUSE_ID)
                .fetchOne(PRISM_CAUSES.CAUSE_ID);

            if (intPk != null) {
                primaryKey = intPk.longValue();
            } else {
                throw new SQLException(
                    String.format("Failed to get or create a named cause record. Cause name: %s", causeName)
                );
            }
        }

        cacheService.namedCausePkMap().put(causeName, primaryKey);

        return primaryKey;
    }

    /**
     * Get or create the entity type record and return the primary key.
     *
     * @param entityType The entity type
     * @param translationKey The translation key
     * @return The primary key
     * @throws SQLException The database exception
     */
    private int getOrCreateEntityTypeId(String entityType, String translationKey) throws SQLException {
        Integer entityPk = cacheService.entityTypePkMap().getIfPresent(entityType);
        if (entityPk != null) {
            return entityPk;
        }

        int primaryKey;

        // Select the existing record
        UInteger intPk = dslContext
            .select(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
            .from(PRISM_ENTITY_TYPES)
            .where(PRISM_ENTITY_TYPES.ENTITY_TYPE.equal(entityType))
            .fetchOne(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID);

        if (intPk != null) {
            primaryKey = intPk.intValue();
        } else {
            // Create the record
            intPk = dslContext
                .insertInto(PRISM_ENTITY_TYPES, PRISM_ENTITY_TYPES.ENTITY_TYPE, PRISM_ENTITY_TYPES.TRANSLATION_KEY)
                .values(entityType, translationKey)
                .returningResult(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
                .fetchOne(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID);

            if (intPk != null) {
                primaryKey = intPk.intValue();
            } else {
                throw new SQLException(
                    String.format("Failed to get or create a entity type record. Material: %s", entityType)
                );
            }
        }

        cacheService.entityTypePkMap().put(entityType, primaryKey);

        return primaryKey;
    }

    /**
     * Get or create the item record and return the primary key.
     *
     * @param material The material
     * @param data The item data
     * @return The primary key
     * @throws SQLException The database exception
     */
    private int getOrCreateItemId(String material, String data) throws SQLException {
        Integer itemPk = cacheService.itemDataPkMap().getIfPresent(data);
        if (itemPk != null) {
            return itemPk;
        }

        int primaryKey;

        // Select the existing item
        UInteger intPk = dslContext
            .select(PRISM_ITEMS.ITEM_ID)
            .from(PRISM_ITEMS)
            .where(PRISM_ITEMS.MATERIAL.equal(material), PRISM_ITEMS.DATA.equal(data))
            .fetchOne(PRISM_ITEMS.ITEM_ID);

        if (intPk != null) {
            primaryKey = intPk.intValue();
        } else {
            // Create the record
            intPk = dslContext
                .insertInto(PRISM_ITEMS, PRISM_ITEMS.MATERIAL, PRISM_ITEMS.DATA)
                .values(material, data)
                .returningResult(PRISM_ITEMS.ITEM_ID)
                .fetchOne(PRISM_ITEMS.ITEM_ID);

            if (intPk != null) {
                primaryKey = intPk.intValue();
            } else {
                throw new SQLException(String.format("Failed to get or create an item record. Material: %s", material));
            }
        }

        cacheService.itemDataPkMap().put(data, primaryKey);

        return primaryKey;
    }

    /**
     * Get or create the player record and return the primary key.
     *
     * <p>Note: This will update the player name.</p>
     *
     * @param playerUuid The player uuid
     * @param playerName The player name
     * @return The primary key
     * @throws SQLException The database exception
     */
    private long getOrCreatePlayerId(UUID playerUuid, String playerName) throws SQLException {
        Long playerPk = cacheService.playerUuidPkMap().getIfPresent(playerUuid);
        if (playerPk != null) {
            return playerPk;
        }

        long primaryKey;

        // Create the player or update the name
        dslContext
            .insertInto(PRISM_PLAYERS, PRISM_PLAYERS.PLAYER_UUID, PRISM_PLAYERS.PLAYER)
            .values(playerUuid.toString(), playerName)
            .onConflict(PRISM_PLAYERS.PLAYER_UUID)
            .doUpdate()
            .set(PRISM_PLAYERS.PLAYER, playerName)
            .execute();

        // Get the primary key.
        // Every but postgres needs a second query to get the pk, so we just do this for everyone.
        var result = dslContext
            .select(PRISM_PLAYERS.PLAYER_ID)
            .from(PRISM_PLAYERS)
            .where(PRISM_PLAYERS.PLAYER_UUID.eq(playerUuid.toString()))
            .fetchOne();

        if (result != null) {
            primaryKey = result.value1().longValue();
        } else {
            throw new SQLException(String.format("Failed to get or create a player record. Player: %s", playerUuid));
        }

        cacheService.playerUuidPkMap().put(playerUuid, primaryKey);

        return primaryKey;
    }

    /**
     * Get or create the world record and return the primary key.
     *
     * <p>Note: This will update the world name.</p>
     *
     * @param worldUuid The world uuid
     * @param worldName The world name
     * @return The primary key
     * @throws SQLException The database exception
     */
    private int getOrCreateWorldId(UUID worldUuid, String worldName) throws SQLException {
        Integer worldPk = cacheService.worldUuidPkMap().getIfPresent(worldUuid);
        if (worldPk != null) {
            return worldPk;
        }

        int primaryKey;

        // Select any existing record
        // Note: We check *then* insert instead of using on duplicate key because ODK would
        // generate a new auto-increment primary key and update it every time, leading to ballooning PKs
        UInteger intPk = dslContext
            .select(PRISM_WORLDS.WORLD_ID)
            .from(PRISM_WORLDS)
            .where(PRISM_WORLDS.WORLD_UUID.equal(worldUuid.toString()))
            .fetchOne(PRISM_WORLDS.WORLD_ID);

        if (intPk != null) {
            primaryKey = intPk.intValue();
        } else {
            // Create the record
            intPk = dslContext
                .insertInto(PRISM_WORLDS, PRISM_WORLDS.WORLD_UUID, PRISM_WORLDS.WORLD)
                .values(worldUuid.toString(), worldName)
                .returningResult(PRISM_WORLDS.WORLD_ID)
                .fetchOne(PRISM_WORLDS.WORLD_ID);

            if (intPk != null) {
                primaryKey = intPk.intValue();
            } else {
                throw new SQLException(String.format("Failed to get or create a world record. World: %s", worldUuid));
            }
        }

        cacheService.worldUuidPkMap().put(worldUuid, primaryKey);

        return primaryKey;
    }

    @Override
    public void commitBatch() {
        dslContext.batchInsert(records).execute();
    }
}
