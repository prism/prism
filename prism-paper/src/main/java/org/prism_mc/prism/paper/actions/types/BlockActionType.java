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

package org.prism_mc.prism.paper.actions.types;

import static org.prism_mc.prism.paper.actions.PaperAction.ObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ActionData;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.paper.actions.PaperBlockAction;

public class BlockActionType extends ActionType {

    /**
     * Construct a new block action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public BlockActionType(String key, ActionResultType resultType, boolean reversible) {
        super(key, resultType, reversible);
    }

    @Override
    public Action createAction(ActionData actionData) {
        BlockData blockData = createBlockData(
            actionData.blockNamespace(),
            actionData.blockName(),
            actionData.blockData()
        );

        ReadWriteNBT readWriteNbt = null;
        if (actionData.customData() != null && actionData.customDataVersion() > 0) {
            readWriteNbt = NBT.parseNBT(actionData.customData());
        }

        BlockData replacedBlockData = null;
        if (actionData.replacedBlockData() != null) {
            replacedBlockData = createBlockData(
                actionData.replacedBlockNamespace(),
                actionData.replacedBlockName(),
                actionData.replacedBlockData()
            );
        }

        Metadata metadata = null;
        if (actionData.metadata() != null) {
            try {
                metadata = ObjectMapper.readValue(actionData.metadata(), Metadata.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return new PaperBlockAction(
            this,
            actionData.blockNamespace(),
            actionData.blockName(),
            blockData,
            readWriteNbt,
            actionData.replacedBlockNamespace(),
            actionData.replacedBlockName(),
            replacedBlockData,
            actionData.translationKey(),
            actionData.replacedBlockTranslationKey(),
            metadata
        );
    }

    /**
     * Convert a namespace, name, and data into block data.
     *
     * @param namespace The namespace
     * @param name The name
     * @param data The data, if any
     * @return The block data
     */
    protected BlockData createBlockData(String namespace, String name, @Nullable String data) {
        var builder = new StringBuilder().append(namespace).append(":").append(name);

        if (data != null) {
            builder.append(data);
        }

        return Bukkit.createBlockData(builder.toString());
    }
}
