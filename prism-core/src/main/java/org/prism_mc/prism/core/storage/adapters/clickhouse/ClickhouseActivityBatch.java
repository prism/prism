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

package org.prism_mc.prism.core.storage.adapters.clickhouse;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
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
import org.prism_mc.prism.api.storage.wal.WalRecord;
import org.prism_mc.prism.api.util.TextUtils;
import org.prism_mc.prism.core.storage.adapters.sql.SqlActivityBatch;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class ClickhouseActivityBatch implements ActivityBatch {

    /**
     * The parameterized insert statement for the activities table, with the prefix already resolved.
     */
    private final String insertSql;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The hikari data source.
     */
    private final HikariDataSource hikariDataSource;

    /**
     * The serializer version.
     */
    private final short serializerVersion;

    /**
     * The schema/table prefix.
     */
    private final String prefix;

    /**
     * The activity id sequence, shared with the adapter.
     */
    private final AtomicLong activityIdSequence;

    /**
     * The newest airtagged item seen in this batch, keyed by airtag id.
     */
    private final Map<String, ClickhouseAirtagPointer> pendingAirtagPointers = new HashMap<>();

    /**
     * The latest airtagged item's material and serialized data, with the activity's timestamp.
     *
     * @param material The affected item material key (empty string when absent)
     * @param data The affected item serialized data (empty string when absent)
     * @param timestampSeconds The activity timestamp, in epoch seconds
     */
    private record ClickhouseAirtagPointer(String material, String data, long timestampSeconds) {}

    /**
     * The connection.
     */
    private Connection connection;

    /**
     * The statement.
     */
    private PreparedStatement statement;

    /**
     * Construct a new batch handler.
     *
     * @param loggingService The logging service
     * @param hikariDataSource The hikari datasource
     * @param serializerVersion The serializer version
     * @param prefix The schema/table prefix
     * @param insertSql The parameterized activities insert statement, with the prefix already resolved
     * @param activityIdSequence The activity id sequence
     */
    public ClickhouseActivityBatch(
        LoggingService loggingService,
        HikariDataSource hikariDataSource,
        short serializerVersion,
        String prefix,
        String insertSql,
        AtomicLong activityIdSequence
    ) {
        this.loggingService = loggingService;
        this.hikariDataSource = hikariDataSource;
        this.serializerVersion = serializerVersion;
        this.prefix = prefix;
        this.insertSql = insertSql;
        this.activityIdSequence = activityIdSequence;
    }

    @Override
    public void startBatch() throws SQLException {
        connection = hikariDataSource.getConnection();
        statement = connection.prepareStatement(insertSql);
        pendingAirtagPointers.clear();
    }

    @Override
    public void add(Activity activity) throws SQLException {
        statement.setLong(1, activityIdSequence.incrementAndGet());
        statement.setLong(2, activity.timestamp() / 1000);
        statement.setString(3, activity.world().value());
        statement.setString(4, activity.world().key().toString());
        statement.setInt(5, activity.coordinate().intX());
        statement.setInt(6, activity.coordinate().intY());
        statement.setInt(7, activity.coordinate().intZ());
        statement.setString(8, activity.action().type().key());

        // Affected item
        if (activity.action() instanceof ItemAction itemAction) {
            String itemData = SqlActivityBatch.guardItemDataSize(
                itemAction.serializeItemData(),
                itemAction.serializeMaterial(),
                activity.action().type().key(),
                loggingService
            );

            setStringOrEmpty(9, itemAction.serializeMaterial());
            setStringOrEmpty(10, itemData);
            setStringOrEmpty(11, itemAction.itemAirtag());
            statement.setShort(12, (short) itemAction.quantity());

            trackAirtagPointer(
                itemAction.itemAirtag(),
                itemAction.serializeMaterial(),
                itemData,
                activity.timestamp() / 1000
            );
        } else {
            statement.setString(9, "");
            statement.setString(10, "");
            statement.setString(11, "");
            statement.setNull(12, Types.SMALLINT);
        }

        // Affected block
        if (activity.action() instanceof BlockAction blockAction) {
            setStringOrEmpty(13, blockAction.blockContainer().blockNamespace());
            setStringOrEmpty(14, blockAction.blockContainer().blockName());
            setStringOrEmpty(15, blockAction.blockContainer().serializeBlockData());
            setStringOrEmpty(16, blockAction.blockContainer().translationKey());
        } else {
            statement.setString(13, "");
            statement.setString(14, "");
            statement.setString(15, "");
            statement.setString(16, "");
        }

        // Replaced block
        if (activity.action() instanceof BlockAction blockAction && blockAction.replacedBlockContainer() != null) {
            setStringOrEmpty(17, blockAction.replacedBlockContainer().blockNamespace());
            setStringOrEmpty(18, blockAction.replacedBlockContainer().blockName());
            setStringOrEmpty(19, blockAction.replacedBlockContainer().serializeBlockData());
        } else {
            statement.setString(17, "");
            statement.setString(18, "");
            statement.setString(19, "");
        }

        // Affected entity
        if (activity.action() instanceof EntityAction entityAction) {
            setStringOrEmpty(20, entityAction.entityContainer().serializeEntityType());
        } else {
            statement.setString(20, "");
        }

        // Affected player
        if (activity.action() instanceof PlayerAction playerAction) {
            setStringOrEmpty(21, playerAction.playerContainer().name());
            setStringOrEmpty(22, playerAction.playerContainer().uuid().toString());
        } else {
            statement.setString(21, "");
            statement.setString(22, "");
        }

        // Cause (default all empty, then set the matching container)
        for (int i = 23; i <= 30; i++) {
            statement.setString(i, "");
        }
        if (activity.cause().container() instanceof StringContainer stringContainer) {
            setStringOrEmpty(23, stringContainer.value());
        } else if (activity.cause().container() instanceof PlayerContainer playerContainer) {
            setStringOrEmpty(24, playerContainer.name());
            setStringOrEmpty(25, playerContainer.uuid().toString());
        } else if (activity.cause().container() instanceof EntityContainer entityContainer) {
            setStringOrEmpty(26, entityContainer.serializeEntityType());
            setStringOrEmpty(27, entityContainer.translationKey());
        } else if (activity.cause().container() instanceof BlockContainer blockContainer) {
            setStringOrEmpty(28, blockContainer.blockNamespace());
            setStringOrEmpty(29, blockContainer.blockName());
            setStringOrEmpty(30, blockContainer.translationKey());
        }

        // Descriptor
        if (activity.action().descriptor() != null) {
            statement.setString(31, TextUtils.truncateWithEllipsis(activity.action().descriptor(), 255));
        } else {
            statement.setString(31, "");
        }

        // Metadata
        statement.setString(32, "");
        if (activity.action().metadata() != null) {
            try {
                String metadata = activity.action().serializeMetadata();
                if (metadata != null) {
                    statement.setString(32, metadata);
                }
            } catch (Exception e) {
                loggingService.handleException(e);
            }
        }

        // Custom data
        if (activity.action() instanceof CustomData customDataAction && customDataAction.hasCustomData()) {
            String customData = SqlActivityBatch.guardSerializedDataSize(
                customDataAction.serializeCustomData(),
                activity.action().type().key(),
                loggingService
            );

            if (customData != null) {
                statement.setInt(33, serializerVersion);
                statement.setString(34, customData);
            } else {
                statement.setNull(33, Types.SMALLINT);
                statement.setString(34, "");
            }
        } else {
            statement.setNull(33, Types.SMALLINT);
            statement.setString(34, "");
        }

        // Reversed (freshly recorded activities are never reversed)
        statement.setInt(35, activity.reversed() ? 1 : 0);

        statement.addBatch();
    }

    @Override
    public void addFromWalRecord(WalRecord walRecord) throws SQLException {
        statement.setLong(1, activityIdSequence.incrementAndGet());
        statement.setLong(2, walRecord.getTimestamp() / 1000);
        setStringOrEmpty(3, walRecord.getWorldName());
        setStringOrEmpty(4, walRecord.getWorldUuid());
        statement.setInt(5, walRecord.getX());
        statement.setInt(6, walRecord.getY());
        statement.setInt(7, walRecord.getZ());
        setStringOrEmpty(8, walRecord.getActionKey());

        // Affected item
        String itemData = SqlActivityBatch.guardItemDataSize(
            walRecord.getItemData(),
            walRecord.getItemMaterial(),
            walRecord.getActionKey(),
            loggingService
        );

        setStringOrEmpty(9, walRecord.getItemMaterial());
        setStringOrEmpty(10, itemData);
        setStringOrEmpty(11, walRecord.getItemAirtag());
        if (walRecord.getItemMaterial() != null) {
            statement.setShort(12, (short) walRecord.getItemQuantity());
        } else {
            statement.setNull(12, Types.SMALLINT);
        }

        trackAirtagPointer(
            walRecord.getItemAirtag(),
            walRecord.getItemMaterial(),
            itemData,
            walRecord.getTimestamp() / 1000
        );

        // Affected block
        setStringOrEmpty(13, walRecord.getBlockNamespace());
        setStringOrEmpty(14, walRecord.getBlockName());
        setStringOrEmpty(15, walRecord.getBlockData());
        setStringOrEmpty(16, walRecord.getBlockTranslationKey());

        // Replaced block
        setStringOrEmpty(17, walRecord.getReplacedBlockNamespace());
        setStringOrEmpty(18, walRecord.getReplacedBlockName());
        setStringOrEmpty(19, walRecord.getReplacedBlockData());

        // Affected entity
        setStringOrEmpty(20, walRecord.getEntityType());

        // Affected player
        setStringOrEmpty(21, walRecord.getAffectedPlayerName());
        setStringOrEmpty(22, walRecord.getAffectedPlayerUuid());

        // Cause (default all empty, then set the matching type)
        for (int i = 23; i <= 30; i++) {
            statement.setString(i, "");
        }
        String causeType = walRecord.getCauseType();
        if ("string".equals(causeType)) {
            setStringOrEmpty(23, walRecord.getCauseString());
        } else if ("player".equals(causeType)) {
            setStringOrEmpty(24, walRecord.getCausePlayerName());
            setStringOrEmpty(25, walRecord.getCausePlayerUuid());
        } else if ("entity".equals(causeType)) {
            setStringOrEmpty(26, walRecord.getCauseEntityType());
            setStringOrEmpty(27, walRecord.getCauseEntityTranslationKey());
        } else if ("block".equals(causeType)) {
            setStringOrEmpty(28, walRecord.getCauseBlockNamespace());
            setStringOrEmpty(29, walRecord.getCauseBlockName());
            setStringOrEmpty(30, walRecord.getCauseBlockTranslationKey());
        }

        // Descriptor
        setStringOrEmpty(31, TextUtils.truncateWithEllipsis(walRecord.getDescriptor(), 255));

        // Metadata
        setStringOrEmpty(32, walRecord.getMetadata());

        // Custom data
        String customData = SqlActivityBatch.guardSerializedDataSize(
            walRecord.getSerializedData(),
            walRecord.getActionKey(),
            loggingService
        );
        if (customData != null) {
            statement.setInt(33, walRecord.getSerializerVersion());
            statement.setString(34, customData);
        } else {
            statement.setNull(33, Types.SMALLINT);
            statement.setString(34, "");
        }

        // Reversed
        statement.setInt(35, 0);

        statement.addBatch();
    }

    @Override
    public void commitBatch() throws SQLException {
        try {
            statement.executeBatch();
            flushAirtagPointers();
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                loggingService.handleException(e);
            }
            connection.close();
        }
    }

    /**
     * Record the affected item of an airtagged activity so the airtag's latest-item pointer can be
     * advanced on commit. Keeps the newest activity per airtag.
     *
     * @param airtag The airtag, or null if the item isn't airtagged
     * @param material The affected item material key
     * @param data The affected item serialized data
     * @param timestampSeconds The activity timestamp, in epoch seconds
     */
    private void trackAirtagPointer(String airtag, String material, String data, long timestampSeconds) {
        if (airtag == null) {
            return;
        }

        ClickhouseAirtagPointer candidate = new ClickhouseAirtagPointer(
            material != null ? material : "",
            data != null ? data : "",
            timestampSeconds
        );

        pendingAirtagPointers.merge(airtag, candidate, (existing, incoming) ->
            incoming.timestampSeconds() >= existing.timestampSeconds() ? incoming : existing
        );
    }

    /**
     * Advance each airtag's latest-item pointer to the newest item seen in this batch.
     *
     * <p>Uses a ClickHouse lightweight {@code UPDATE} guarded by {@code latest_item_timestamp < ?}
     * so a pointer only advances when the batch's item is newer than the stored one. When no airtag
     * row exists yet the update simply matches nothing.</p>
     *
     * @throws SQLException On error
     */
    private void flushAirtagPointers() throws SQLException {
        if (pendingAirtagPointers.isEmpty()) {
            return;
        }

        String sql = String.format(
            "UPDATE %sairtags SET latest_item_material = ?, latest_item_data = ?, latest_item_timestamp = ? " +
            "WHERE airtag = ? AND latest_item_timestamp < ?",
            prefix
        );

        try (PreparedStatement updateStatement = connection.prepareStatement(sql)) {
            for (var entry : pendingAirtagPointers.entrySet()) {
                ClickhouseAirtagPointer pointer = entry.getValue();

                updateStatement.setString(1, pointer.material());
                updateStatement.setString(2, pointer.data());
                updateStatement.setLong(3, pointer.timestampSeconds());
                updateStatement.setString(4, entry.getKey());
                updateStatement.setLong(5, pointer.timestampSeconds());
                updateStatement.executeUpdate();
            }
        }
    }

    /**
     * Set a string parameter, substituting an empty string for null. The flat table's string
     * columns are non-nullable, so absent values are stored as empty strings.
     *
     * @param index The parameter index
     * @param value The value
     * @throws SQLException On error
     */
    private void setStringOrEmpty(int index, String value) throws SQLException {
        statement.setString(index, value != null ? value : "");
    }
}
