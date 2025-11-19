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

import com.zaxxer.hikari.HikariDataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
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
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class SqlActivityProcedureBatch implements ActivityBatch {

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
     * The connection.
     */
    private Connection connection;

    /**
     * The schema/table prefix.
     */
    private final String prefix;

    /**
     * The statement.
     */
    private CallableStatement statement;

    /**
     * Construct a new batch handler.
     *
     * @param loggingService The logging service
     * @param hikariDataSource The hikari datasource
     * @param serializerVersion The serializer version
     * @param prefix The schema/table prefix
     */
    public SqlActivityProcedureBatch(
        LoggingService loggingService,
        HikariDataSource hikariDataSource,
        short serializerVersion,
        String prefix
    ) {
        this.loggingService = loggingService;
        this.hikariDataSource = hikariDataSource;
        this.serializerVersion = serializerVersion;
        this.prefix = prefix;
    }

    @Override
    public void startBatch() throws SQLException {
        connection = hikariDataSource.getConnection();

        statement = connection.prepareCall(
            String.format(
                "{ CALL %screate_activity(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }",
                prefix
            )
        );
    }

    @Override
    public void add(Activity activity) throws SQLException {
        int timestampIndex = 1;
        int worldNameIndex = 2;
        int worldUuidIndex = 3;
        int xIndex = 4;
        int yIndex = 5;
        int zIndex = 6;
        int actionIndex = 7;
        int affectedItemMaterialIndex = 8;
        int affectedItemQuantityIndex = 9;
        int affectedItemDataIndex = 10;
        int affectedBlockNamespaceIndex = 11;
        int affectedBlockNameIndex = 12;
        int affectedBlockDataIndex = 13;
        int affectedBlockTranslationKeyIndex = 14;
        int replacedBlockNamespaceIndex = 15;
        int replacedBlockNameIndex = 16;
        int replacedBlockDataIndex = 17;
        int replacedBlockTranslationKeyIndex = 18;
        int affectedEntityTypeIndex = 19;
        int affectedEntityTypeTranslationKeyIndex = 20;
        int affectedPlayerNameIndex = 21;
        int affectedPlayerUuidIndex = 22;
        int causeNameIndex = 23;
        int causePlayerNameIndex = 24;
        int causePlayerUuidIndex = 25;
        int causeEntityTypeIndex = 26;
        int causeEntityTypeTranslationKeyIndex = 27;
        int causeBlockNamespaceIndex = 28;
        int causeBlockNameIndex = 29;
        int causeBlockDataIndex = 30;
        int causeBlockTranslationKeyIndex = 31;
        int serializerVersionIndex = 32;
        int serializedDataIndex = 33;
        int descriptorIndex = 34;
        int metadataIndex = 35;

        statement.setLong(timestampIndex, activity.timestamp() / 1000);
        statement.setInt(xIndex, activity.coordinate().intX());
        statement.setInt(yIndex, activity.coordinate().intY());
        statement.setInt(zIndex, activity.coordinate().intZ());
        statement.setString(actionIndex, activity.action().type().key());

        // Affected player
        if (activity.action() instanceof PlayerAction playerAction) {
            statement.setString(affectedPlayerNameIndex, playerAction.playerContainer().name());
            statement.setString(affectedPlayerUuidIndex, playerAction.playerContainer().uuid().toString());
        } else {
            statement.setNull(affectedPlayerNameIndex, Types.VARCHAR);
            statement.setNull(affectedPlayerUuidIndex, Types.VARCHAR);
        }

        // Causes
        statement.setNull(causeNameIndex, Types.VARCHAR);
        statement.setNull(causePlayerNameIndex, Types.VARCHAR);
        statement.setNull(causePlayerUuidIndex, Types.VARCHAR);
        statement.setNull(causeEntityTypeIndex, Types.VARCHAR);
        statement.setNull(causeEntityTypeTranslationKeyIndex, Types.VARCHAR);
        statement.setNull(causeBlockNamespaceIndex, Types.VARCHAR);
        statement.setNull(causeBlockNameIndex, Types.VARCHAR);
        statement.setNull(causeBlockDataIndex, Types.VARCHAR);
        statement.setNull(causeBlockTranslationKeyIndex, Types.VARCHAR);

        if (activity.cause().container() instanceof StringContainer stringContainer) {
            statement.setString(causeNameIndex, stringContainer.value());
        } else if (activity.cause().container() instanceof PlayerContainer playerContainer) {
            statement.setString(causePlayerNameIndex, playerContainer.name());
            statement.setString(causePlayerUuidIndex, playerContainer.uuid().toString());
        } else if (activity.cause().container() instanceof EntityContainer entityContainer) {
            statement.setString(causeEntityTypeIndex, entityContainer.serializeEntityType());
            statement.setString(causeEntityTypeTranslationKeyIndex, entityContainer.translationKey());
        } else if (activity.cause().container() instanceof BlockContainer blockContainer) {
            statement.setString(causeBlockNamespaceIndex, blockContainer.blockNamespace());
            statement.setString(causeBlockNameIndex, blockContainer.blockName());
            statement.setString(causeBlockDataIndex, blockContainer.serializeBlockData());
            statement.setString(causeBlockTranslationKeyIndex, blockContainer.translationKey());
        }

        // Entity
        if (activity.action() instanceof EntityAction entityAction) {
            statement.setString(affectedEntityTypeIndex, entityAction.entityContainer().serializeEntityType());
            statement.setString(affectedEntityTypeTranslationKeyIndex, entityAction.entityContainer().translationKey());
        } else {
            statement.setNull(affectedEntityTypeIndex, Types.VARCHAR);
            statement.setNull(affectedEntityTypeTranslationKeyIndex, Types.VARCHAR);
        }

        // Material
        if (activity.action() instanceof ItemAction itemAction) {
            statement.setString(affectedItemMaterialIndex, itemAction.serializeMaterial());
            statement.setShort(affectedItemQuantityIndex, (short) itemAction.quantity());
            statement.setString(affectedItemDataIndex, itemAction.serializeItemData());
        } else {
            statement.setNull(affectedItemMaterialIndex, Types.VARCHAR);
            statement.setNull(affectedItemQuantityIndex, Types.SMALLINT);
            statement.setNull(affectedItemDataIndex, Types.VARCHAR);
        }

        // Block data
        if (activity.action() instanceof BlockAction blockAction) {
            statement.setString(affectedBlockNamespaceIndex, blockAction.blockContainer().blockNamespace());
            statement.setString(affectedBlockNameIndex, blockAction.blockContainer().blockName());
            statement.setString(affectedBlockDataIndex, blockAction.blockContainer().serializeBlockData());
            statement.setString(affectedBlockTranslationKeyIndex, blockAction.blockContainer().translationKey());
        } else {
            statement.setNull(affectedBlockNamespaceIndex, Types.VARCHAR);
            statement.setNull(affectedBlockNameIndex, Types.VARCHAR);
            statement.setNull(affectedBlockDataIndex, Types.VARCHAR);
            statement.setNull(affectedBlockTranslationKeyIndex, Types.VARCHAR);
        }

        // Replaced block data
        if (activity.action() instanceof BlockAction blockAction && blockAction.replacedBlockContainer() != null) {
            statement.setString(replacedBlockNamespaceIndex, blockAction.replacedBlockContainer().blockNamespace());
            statement.setString(replacedBlockNameIndex, blockAction.replacedBlockContainer().blockName());
            statement.setString(replacedBlockDataIndex, blockAction.replacedBlockContainer().serializeBlockData());
            statement.setString(
                replacedBlockTranslationKeyIndex,
                blockAction.replacedBlockContainer().translationKey()
            );
        } else {
            statement.setNull(replacedBlockNamespaceIndex, Types.VARCHAR);
            statement.setNull(replacedBlockNameIndex, Types.VARCHAR);
            statement.setNull(replacedBlockDataIndex, Types.VARCHAR);
            statement.setNull(replacedBlockTranslationKeyIndex, Types.VARCHAR);
        }

        // World
        statement.setString(worldNameIndex, activity.world().value());
        statement.setString(worldUuidIndex, activity.world().key().toString());

        // Custom data
        if (activity.action() instanceof CustomData) {
            statement.setInt(serializerVersionIndex, serializerVersion);

            String customData = ((CustomData) activity.action()).serializeCustomData();
            statement.setString(serializedDataIndex, customData);
        } else {
            statement.setNull(serializerVersionIndex, Types.SMALLINT);
            statement.setNull(serializedDataIndex, Types.VARCHAR);
        }

        // Descriptor
        if (activity.action().descriptor() != null) {
            statement.setString(descriptorIndex, TextUtils.truncateWithEllipsis(activity.action().descriptor(), 255));
        } else {
            statement.setNull(descriptorIndex, Types.VARCHAR);
        }

        // Serialize the metadata
        statement.setNull(metadataIndex, Types.VARCHAR);
        if (activity.action().metadata() != null) {
            try {
                statement.setString(metadataIndex, activity.action().serializeMetadata());
            } catch (Exception e) {
                loggingService.handleException(e);
            }
        }

        statement.addBatch();
    }

    @Override
    public void commitBatch() throws SQLException {
        try {
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            statement.close();
            connection.close();
        }
    }
}
