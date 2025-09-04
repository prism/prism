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

package org.prism_mc.prism.paper.actions;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.BlockAction;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.ModificationSkipReason;
import org.prism_mc.prism.api.services.modifications.StateChange;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.paper.api.containers.PaperBlockContainer;
import org.prism_mc.prism.paper.services.modifications.state.BlockStateChange;

public class PaperBlockAction extends PaperAction implements BlockAction {

    @Getter
    private final PaperBlockContainer blockContainer;

    @Getter
    private final PaperBlockContainer replacedBlockContainer;

    /**
     * The read/write nbt.
     */
    private ReadWriteNBT readWriteNbt;

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param blockState The block state
     */
    public PaperBlockAction(ActionType type, BlockState blockState) {
        this(type, blockState, null);
    }

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param blockState The block state
     * @param replacedBlockState The replaced block state
     * @param metadata The metadata
     */
    public PaperBlockAction(
        ActionType type,
        BlockState blockState,
        @Nullable BlockState replacedBlockState,
        Metadata metadata
    ) {
        this(type, blockState, replacedBlockState);
        this.metadata = metadata;
    }

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param blockState The block state
     * @param replacedBlockState The replaced block state
     */
    public PaperBlockAction(ActionType type, BlockState blockState, @Nullable BlockState replacedBlockState) {
        this(
            type,
            blockState,
            blockState.getType().getBlockTranslationKey(),
            replacedBlockState,
            replacedBlockState != null ? replacedBlockState.getType().getBlockTranslationKey() : null
        );
    }

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param blockState The block state
     * @param replacedBlockState The replaced block state
     */
    public PaperBlockAction(
        ActionType type,
        BlockState blockState,
        String translationKey,
        @Nullable BlockState replacedBlockState,
        @Nullable String replacedBlockTranslationKey
    ) {
        this(
            type,
            blockState.getBlockData(),
            translationKey,
            replacedBlockState != null ? replacedBlockState.getBlockData() : null,
            replacedBlockTranslationKey
        );
        if (blockState instanceof TileState) {
            readWriteNbt = NBT.createNBTObject();
            NBT.get(blockState, readWriteNbt::mergeCompound);
        }
    }

    /**
     * Construct a block action.
     *
     * @param type The action type
     * @param blockData The block data
     * @param translationKey The translation key
     * @param replacedBlockData The replaced block data
     * @param replacedBlockTranslationKey The replaced block translation key
     */
    public PaperBlockAction(
        ActionType type,
        BlockData blockData,
        String translationKey,
        @Nullable BlockData replacedBlockData,
        @Nullable String replacedBlockTranslationKey
    ) {
        super(type);
        this.blockContainer = new PaperBlockContainer(blockData, translationKey);

        if (replacedBlockData != null) {
            this.replacedBlockContainer = new PaperBlockContainer(replacedBlockData, replacedBlockTranslationKey);
        } else {
            this.replacedBlockContainer = null;
        }
    }

    /**
     * Construct a block state action.
     *
     * @param type The action type
     * @param blockNamespace The namespace
     * @param blockName The name
     * @param blockData The block data
     * @param teData The custom data
     * @param replacedBlockNamespace The replaced block namespace
     * @param replacedBlockName The replaced block name
     * @param replacedBlockData The replaced block data
     * @param translationKey The translation key
     * @param replacedBlockTranslationKey The replaced block translation key
     * @param metadata The metadata
     */
    public PaperBlockAction(
        ActionType type,
        String blockNamespace,
        String blockName,
        BlockData blockData,
        ReadWriteNBT teData,
        String replacedBlockNamespace,
        String replacedBlockName,
        BlockData replacedBlockData,
        String translationKey,
        String replacedBlockTranslationKey,
        Metadata metadata
    ) {
        super(type);
        this.blockContainer = new PaperBlockContainer(blockNamespace, blockName, blockData, translationKey);
        this.readWriteNbt = teData;
        this.metadata = metadata;

        if (replacedBlockData != null) {
            this.replacedBlockContainer = new PaperBlockContainer(
                replacedBlockNamespace,
                replacedBlockName,
                replacedBlockData,
                replacedBlockTranslationKey
            );
        } else {
            this.replacedBlockContainer = null;
        }
    }

    @Override
    public Component descriptorComponent() {
        return Component.translatable(blockContainer.translationKey());
    }

    @Override
    public boolean hasCustomData() {
        return this.readWriteNbt != null;
    }

    /**
     * Allow merging in custom NBT data if we need to override something.
     *
     * @param nbtString The nbt string
     */
    public void mergeCompound(String nbtString) {
        if (readWriteNbt != null) {
            readWriteNbt.mergeCompound(NBT.parseNBT(nbtString));
        }
    }

    @Override
    public @Nullable String serializeCustomData() {
        if (this.readWriteNbt != null) {
            return this.readWriteNbt.toString();
        }

        return null;
    }

    @Override
    public ModificationResult applyRollback(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        var resultBuilder = ModificationResult.builder().activity(activityContext).statusFromMode(mode);

        // Skip if either material is in the blacklist
        BlockData finalBlockData = blockContainer.blockData();
        if (modificationRuleset.blockBlacklistContainsAny(blockContainer.blockName())) {
            finalBlockData = Bukkit.createBlockData(Material.AIR);
            resultBuilder.partial().target(blockContainer.translationKey());
        }

        BlockData finalReplacedBlockData;
        if (replacedBlockContainer != null) {
            finalReplacedBlockData = replacedBlockContainer.blockData();

            if (modificationRuleset.blockBlacklistContainsAny(replacedBlockContainer.blockName())) {
                finalReplacedBlockData = Bukkit.createBlockData(Material.AIR);
                resultBuilder.partial().target(replacedBlockContainer.translationKey());
            }
        } else {
            finalReplacedBlockData = Bukkit.createBlockData(Material.AIR);
        }

        var location = location(activityContext.worldUuid(), activityContext.coordinate());
        var block = location.getWorld().getBlockAt(location);

        StateChange<BlockState> stateChange = null;
        if (type().resultType().equals(ActionResultType.REMOVES)) {
            var canSet = canSet(block, finalBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            if (mode.equals(ModificationQueueMode.COMPLETING)) {
                // If rolling back a removal, we need to place the top half of a bisected block
                // This happens first otherwise the block will break again
                if (finalBlockData instanceof Bisected bisected) {
                    setBisectedTop(block, bisected, bisected.getMaterial());
                }
            }

            // If the action type removes a block, rollback means we re-set it
            stateChange = setBlock(block, location, finalBlockData, finalReplacedBlockData, readWriteNbt, owner, mode);
        } else if (type().resultType().equals(ActionResultType.CREATES)) {
            var canSet = canSet(block, finalReplacedBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            // If the action type creates a block, rollback means we remove it
            stateChange = setBlock(block, location, finalReplacedBlockData, finalBlockData, null, owner, mode);
        }

        return resultBuilder.stateChange(stateChange).build();
    }

    @Override
    public ModificationResult applyRestore(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    ) {
        var resultBuilder = ModificationResult.builder().activity(activityContext).statusFromMode(mode);

        // Skip if either material is in the blacklist
        BlockData finalBlockData = blockContainer.blockData();
        if (modificationRuleset.blockBlacklistContainsAny(blockContainer.blockName())) {
            finalBlockData = Bukkit.createBlockData(Material.AIR);
            resultBuilder.partial().target(blockContainer.translationKey());
        }

        BlockData finalReplacedBlockData = null;
        if (replacedBlockContainer != null) {
            finalReplacedBlockData = replacedBlockContainer.blockData();

            if (modificationRuleset.blockBlacklistContainsAny(replacedBlockContainer.blockName())) {
                finalReplacedBlockData = Bukkit.createBlockData(Material.AIR);
                resultBuilder.partial().target(replacedBlockContainer.translationKey());
            }
        }

        var location = location(activityContext.worldUuid(), activityContext.coordinate());
        var block = location.getWorld().getBlockAt(location);

        StateChange<BlockState> stateChange = null;
        if (type().resultType().equals(ActionResultType.CREATES)) {
            var canSet = canSet(block, finalBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            if (mode.equals(ModificationQueueMode.COMPLETING)) {
                // If rolling back a removal, we need to place the top half of a bisected block
                // This happens first otherwise the block will break again
                if (finalBlockData instanceof Bisected bisected) {
                    setBisectedTop(block, bisected, bisected.getMaterial());
                }
            }

            // If the action type creates a block, restore means we re-set it
            stateChange = setBlock(block, location, finalBlockData, finalReplacedBlockData, readWriteNbt, owner, mode);
        } else if (type().resultType().equals(ActionResultType.REMOVES)) {
            var canSet = canSet(block, finalReplacedBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            // If the action type removes a block, restore means we remove it again
            stateChange = setBlock(block, location, finalReplacedBlockData, finalBlockData, null, owner, mode);
        }

        return resultBuilder.stateChange(stateChange).build();
    }

    /**
     * A convenience method for getting a location.
     *
     * @param worldUuid The world uuid
     * @param coordinate The coordinate
     * @return The location
     */
    protected Location location(UUID worldUuid, Coordinate coordinate) {
        World world = Bukkit.getWorld(worldUuid);
        return new Location(world, coordinate.x(), coordinate.y(), coordinate.z());
    }

    /**
     * Compare the current block to our planned change and determine if a change is warranted.
     *
     * @param block The block
     * @param newBlockData The new block data
     * @param modificationRuleset The modification ruleset
     * @param activityContext The activity context
     * @return True if already set
     */
    protected ModificationResult canSet(
        Block block,
        BlockData newBlockData,
        ModificationRuleset modificationRuleset,
        Activity activityContext
    ) {
        if (!modificationRuleset.overwrite() && block.getBlockData().matches(newBlockData)) {
            return ModificationResult.builder()
                .activity(activityContext)
                .skipped()
                .target(block.translationKey())
                .skipReason(ModificationSkipReason.ALREADY_SET)
                .build();
        }

        return null;
    }

    /**
     * Sets an in-world block to this block data.
     */
    protected StateChange<BlockState> setBlock(
        Block block,
        Location location,
        BlockData newBlockData,
        BlockData oldBlockData,
        ReadWriteNBT readWriteNbt,
        Object owner,
        ModificationQueueMode mode
    ) {
        // Capture existing state for reporting/reversing needs
        final BlockState oldState = block.getState();

        if (mode.equals(ModificationQueueMode.COMPLETING)) {
            // Set the bed head part before applying the root block change
            // otherwise the bed will just re-break.
            if (newBlockData instanceof Bed bed) {
                setBedHead(block, bed);
            } else if (oldBlockData instanceof Bed bed) {
                setBedHead(block, bed);
            }
        }

        // Send block change or change world
        if (mode.equals(ModificationQueueMode.PLANNING) && owner instanceof Player player) {
            player.sendBlockChange(location, newBlockData);
        } else if (mode.equals(ModificationQueueMode.COMPLETING)) {
            block.setBlockData(newBlockData);
        }

        // Set NBT
        if (block.getType() != Material.AIR && mode.equals(ModificationQueueMode.COMPLETING) && readWriteNbt != null) {
            NBT.modify(block.getState(), nbt -> {
                nbt.mergeCompound(readWriteNbt);
            });
        }

        return new BlockStateChange(oldState, block.getState());
    }

    /**
     * Set the HEAD part of a bed.
     *
     * @param block The block being changed
     * @param bed The bed block data
     */
    protected void setBedHead(Block block, Bed bed) {
        // Bed activities will always be the FOOT part
        Block relative = block.getRelative(bed.getFacing());

        if (type().resultType().equals(ActionResultType.CREATES)) {
            relative.setType(Material.AIR);
        } else {
            relative.setType(bed.getMaterial());

            if (bed.clone() instanceof Bed siblingBed) {
                siblingBed.setPart(Bed.Part.HEAD);
                relative.setBlockData(siblingBed);
            }
        }
    }

    /**
     * Set the TOP part of a bisected block.
     *
     * @param block The block being changed
     * @param bisected The bisected block data
     * @param material The material
     */
    protected void setBisectedTop(Block block, Bisected bisected, Material material) {
        // Some bisected blocks don't need help
        if (bisected instanceof Stairs || bisected instanceof TrapDoor) {
            return;
        }

        // Bisected activities will always be the BOTTOM part
        Block relative = block.getRelative(BlockFace.UP);

        relative.setType(material);
        if (relative.getBlockData().clone() instanceof Bisected siblingBisected) {
            siblingBisected.setHalf(Bisected.Half.TOP);
            relative.setBlockData(siblingBisected);
        }
    }

    @Override
    public String toString() {
        return String.format(
            "BlockAction{type=%s,blockContainer=%s,replacedBlockContainer=%s}",
            type,
            blockContainer,
            replacedBlockContainer
        );
    }
}
