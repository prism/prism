/*
 * Prism (Refracted)
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

package network.darkhelmet.prism.actions;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTTileEntity;

import java.util.Locale;

import network.darkhelmet.prism.api.actions.IBlockAction;
import network.darkhelmet.prism.api.actions.types.ActionResultType;
import network.darkhelmet.prism.api.actions.types.ActionType;
import network.darkhelmet.prism.api.actions.types.IActionType;
import network.darkhelmet.prism.api.activities.IActivity;
import network.darkhelmet.prism.api.services.modifications.BlockStateChange;
import network.darkhelmet.prism.api.services.modifications.ModificationResult;
import network.darkhelmet.prism.api.services.modifications.ModificationResultStatus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class BlockStateAction extends MaterialAction implements IBlockAction {
    /**
     * The block data.
     */
    private final BlockData blockData;

    /**
     * The nbt container.
     */
    private final NBTContainer nbtContainer;

    /**
     * The replaced material.
     */
    private final Material replacedMaterial;

    /**
     * The replaced block data.
     */
    private final BlockData replacedBlockData;

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param blockState The block state
     * @param replacedBlockState The replaced block state
     */
    public BlockStateAction(IActionType type, BlockState blockState, @Nullable BlockState replacedBlockState) {
        super(type, blockState.getType());

        // Set new block data
        this.blockData = blockState.getBlockData();
        if (blockState instanceof TileState) {
            NBTTileEntity nbtTe = new NBTTileEntity(blockState);
            this.nbtContainer = new NBTContainer(nbtTe.getCompound());
        } else {
            this.nbtContainer = null;
        }

        // Set old block data
        if (replacedBlockState != null) {
            this.replacedBlockData = replacedBlockState.getBlockData();
            this.replacedMaterial = replacedBlockState.getType();
        } else {
            this.replacedBlockData = null;
            this.replacedMaterial = Material.AIR;
        }
    }

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param material The material
     * @param blockData The block data
     * @param teData The custom data
     * @param replacedMaterial The replaced material
     * @param replacedBlockData The replaced block data
     */
    public BlockStateAction(
            ActionType type,
            Material material,
            BlockData blockData,
            NBTContainer teData,
            Material replacedMaterial,
            BlockData replacedBlockData,
            String descriptor) {
        super(type, material, descriptor);

        this.blockData = blockData;
        this.nbtContainer = teData;
        this.replacedMaterial = replacedMaterial;
        this.replacedBlockData = replacedBlockData;
    }

    @Override
    public @Nullable String serializeBlockData() {
        return this.blockData.getAsString().replaceAll("^[^\\[]+", "");
    }

    @Override
    public boolean hasCustomData() {
        return this.nbtContainer != null;
    }

    @Override
    public @Nullable String serializeCustomData() {
        if (this.nbtContainer != null) {
            return this.nbtContainer.toString();
        }

        return null;
    }

    @Override
    public @Nullable String serializeReplacedMaterial() {
        if (replacedMaterial == null) {
            return null;
        }

        return replacedMaterial.toString().toLowerCase(Locale.ENGLISH);
    }

    @Override
    public @Nullable String serializeReplacedBlockData() {
        if (replacedBlockData == null) {
            return null;
        }

        return this.replacedBlockData.getAsString().replaceAll("^[^\\[]+", "");
    }

    @Override
    public ModificationResult applyRollback(CommandSender owner, IActivity activityContext, boolean isPreview) {
        if (!type().reversible()) {
            return new ModificationResult(ModificationResultStatus.SKIPPED, null);
        }

        BlockStateChange stateChange = null;
        if (type().resultType().equals(ActionResultType.REMOVES)) {
            // If the action type removes a block, rollback means we re-set it
            stateChange = setBlock(activityContext.location(), material, blockData, nbtContainer, owner, isPreview);
        } else if (type().resultType().equals(ActionResultType.CREATES)) {
            // If the action type creates a block, rollback means we remove it
            stateChange = setBlock(
                activityContext.location(), replacedMaterial, replacedBlockData, null, owner, isPreview);
        }

        return new ModificationResult(ModificationResultStatus.APPLIED, stateChange);
    }

    @Override
    public ModificationResult applyRestore(CommandSender owner, IActivity activityContext, boolean isPreview) {
        if (!type().reversible()) {
            return new ModificationResult(ModificationResultStatus.SKIPPED, null);
        }

        BlockStateChange stateChange = null;
        if (type().resultType().equals(ActionResultType.CREATES)) {
            // If the action type creates a block, restore means we re-set it
            stateChange = setBlock(activityContext.location(), material, blockData, nbtContainer, owner, isPreview);
        } else if (type().resultType().equals(ActionResultType.REMOVES)) {
            // If the action type removes a block, restore means we remove it again
            stateChange = setBlock(
                activityContext.location(), replacedMaterial, replacedBlockData, null, owner, isPreview);
        }

        return new ModificationResult(ModificationResultStatus.APPLIED, stateChange);
    }

    /**
     * Sets an in-world block to this block data.
     */
    protected BlockStateChange setBlock(
        Location location,
        Material newMaterial,
        BlockData newBlockData,
        NBTContainer newNbtContainer,
        CommandSender owner,
        boolean isPreview
    ) {
        final Block block = location.getWorld().getBlockAt(location);

        // Capture existing state for reporting/reversing needs
        final BlockState oldState = block.getState();

        // Set the new material
        if (!isPreview) {
            block.setType(newMaterial);
        }

        // Set the block data
        if (newBlockData != null) {
            if (isPreview && owner instanceof Player player) {
                player.sendBlockChange(location, newBlockData);
            } else {
                block.setBlockData(newBlockData, true);
            }
        }

        // Set NBT
        if (newNbtContainer != null) {
            new NBTTileEntity(block.getState()).mergeCompound(newNbtContainer);
        }

        return new BlockStateChange(oldState, block.getState());
    }
}
