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
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
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
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.paper.api.containers.PaperBlockContainer;
import org.prism_mc.prism.paper.services.modifications.BlockUndoEntry;
import org.prism_mc.prism.paper.utils.BlockUtils;
import org.prism_mc.prism.paper.utils.TagLib;

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
     * The serialized custom nbt string.
     */
    private String serializedCustomData;

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
        if (blockState instanceof TileState && type.reversible()) {
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

            // Mutating the container invalidates any cached serialization.
            serializedCustomData = null;
        }
    }

    @Override
    public @Nullable String serializeCustomData() {
        if (this.readWriteNbt == null) {
            return null;
        }

        if (serializedCustomData == null) {
            serializedCustomData = this.readWriteNbt.toString();
        }

        return serializedCustomData;
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

        finalReplacedBlockData = airIfTransient(finalReplacedBlockData);

        var location = location(activityContext.worldUuid(), activityContext.coordinate());
        var block = location.getWorld().getBlockAt(location);
        var applyPhysics = modificationRuleset.applyPhysics();

        BlockUndoEntry undoEntry = null;
        if (type().resultType().equals(ActionResultType.REMOVES)) {
            var canWrite = canWrite(finalBlockData, activityContext);
            if (canWrite != null) {
                return canWrite;
            }

            var canSet = canSet(block, finalBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            // If the action type removes a block, rollback means we re-set it
            undoEntry = setBlock(
                activityContext,
                block,
                location,
                finalBlockData,
                finalReplacedBlockData,
                readWriteNbt,
                owner,
                mode,
                applyPhysics
            );

            if (mode.equals(ModificationQueueMode.COMPLETING) && finalBlockData instanceof Chest chest) {
                BlockUtils.reconcileChestConnection(block, chest, applyPhysics);
            }
        } else if (type().resultType().equals(ActionResultType.CREATES)) {
            var canSet = canSet(block, finalReplacedBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            // If the action type creates a block, rollback means we remove it
            undoEntry = setBlock(
                activityContext,
                block,
                location,
                finalReplacedBlockData,
                finalBlockData,
                null,
                owner,
                mode,
                applyPhysics
            );

            if (mode.equals(ModificationQueueMode.COMPLETING) && finalBlockData instanceof Chest chest) {
                BlockUtils.downgradeChestPartner(block, chest, applyPhysics);
            }
        }

        return resultBuilder.undoEntry(undoEntry).build();
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

        finalReplacedBlockData = airIfTransient(finalReplacedBlockData);

        var location = location(activityContext.worldUuid(), activityContext.coordinate());
        var block = location.getWorld().getBlockAt(location);
        var applyPhysics = modificationRuleset.applyPhysics();

        BlockUndoEntry undoEntry = null;
        if (type().resultType().equals(ActionResultType.CREATES)) {
            var canWrite = canWrite(finalBlockData, activityContext);
            if (canWrite != null) {
                return canWrite;
            }

            var canSet = canSet(block, finalBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            // If the action type creates a block, restore means we re-set it
            undoEntry = setBlock(
                activityContext,
                block,
                location,
                finalBlockData,
                finalReplacedBlockData,
                readWriteNbt,
                owner,
                mode,
                applyPhysics
            );

            if (mode.equals(ModificationQueueMode.COMPLETING) && finalBlockData instanceof Chest chest) {
                BlockUtils.reconcileChestConnection(block, chest, applyPhysics);
            }
        } else if (type().resultType().equals(ActionResultType.REMOVES)) {
            var canSet = canSet(block, finalReplacedBlockData, modificationRuleset, activityContext);
            if (canSet != null) {
                return canSet;
            }

            // If the action type removes a block, restore means we remove it again
            undoEntry = setBlock(
                activityContext,
                block,
                location,
                finalReplacedBlockData,
                finalBlockData,
                null,
                owner,
                mode,
                applyPhysics
            );

            if (mode.equals(ModificationQueueMode.COMPLETING) && finalBlockData instanceof Chest chest) {
                BlockUtils.downgradeChestPartner(block, chest, applyPhysics);
            }
        }

        return resultBuilder.undoEntry(undoEntry).build();
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
        if (world == null) {
            throw new IllegalStateException("World " + worldUuid + " is not loaded");
        }

        return new Location(world, coordinate.x(), coordinate.y(), coordinate.z());
    }

    /**
     * Determine whether block data may be written into the world at all.
     *
     * @param newBlockData The new block data
     * @param activityContext The activity context
     * @return A skipped result if the block data cannot be written, otherwise null
     */
    protected ModificationResult canWrite(@Nullable BlockData newBlockData, Activity activityContext) {
        if (newBlockData != null && TagLib.TRANSIENT_BLOCKS.isTagged(newBlockData.getMaterial())) {
            return ModificationResult.builder()
                .activity(activityContext)
                .skipped()
                .target(blockContainer.translationKey())
                .skipReason(ModificationSkipReason.NOT_APPLICABLE)
                .build();
        }

        return null;
    }

    /**
     * Reduce block data to air when it names a transient block.
     *
     * @param blockData The block data
     * @return Air if the block data is transient, otherwise the block data
     */
    protected @Nullable BlockData airIfTransient(@Nullable BlockData blockData) {
        if (blockData != null && TagLib.TRANSIENT_BLOCKS.isTagged(blockData.getMaterial())) {
            return Bukkit.createBlockData(Material.AIR);
        }

        return blockData;
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
     * Sets an in-world block to this block data. In COMPLETING mode, captures
     * a lightweight {@link BlockUndoEntry} from the live block *before* the
     * write so {@code /pr undo} can replay world state without re-deriving it
     * from the activity log. Preview is packet-only; cancelling a preview
     * re-streams the query and sends live block data back to the player.
     *
     * @return The captured undo entry for a COMPLETING write, or null otherwise
     */
    protected BlockUndoEntry setBlock(
        Activity activityContext,
        Block block,
        Location location,
        @Nullable BlockData newBlockData,
        @Nullable BlockData oldBlockData,
        ReadWriteNBT readWriteNbt,
        Object owner,
        ModificationQueueMode mode,
        boolean applyPhysics
    ) {
        // A removal (clearing to air) must never trigger neighbor physics, even when
        // applyPhysics is enabled. With physics on, removing a block detaches anything
        // resting on it (flowers, buttons, crops) into an item drop *before* the queue
        // reaches that neighbor's own activity — so the rollback leaves the build short,
        // and the popped block's undo snapshot captures air, making it unrecoverable.
        // Placements still honor applyPhysics so light/redstone updates can propagate.
        boolean removing = newBlockData == null || newBlockData.getMaterial() == Material.AIR;
        boolean physics = applyPhysics && !removing;

        if (mode.equals(ModificationQueueMode.COMPLETING)) {
            // Double blocks (beds, doors, tall plants) are stored as a single activity for the
            // lower/foot half; the partner is synthesized here. Reconcile it *before* writing the
            // root block: placing the root with physics would otherwise re-break it, and removals
            // (physics disabled) would orphan the upper half / head.
            BlockUtils.reconcileBedPartner(block, newBlockData, oldBlockData, physics);
            BlockUtils.reconcileBisectedPartner(block, newBlockData, oldBlockData, physics);
        }

        if (newBlockData == null) {
            newBlockData = Bukkit.createBlockData(Material.AIR);
        }

        if (mode.equals(ModificationQueueMode.PLANNING) && owner instanceof Player player) {
            player.sendBlockChange(location, newBlockData);
            return null;
        } else if (!mode.equals(ModificationQueueMode.COMPLETING)) {
            return null;
        }

        // Capture the live block data + tile NBT immediately before the write
        // so /pr undo can replay world state without re-deriving from the log.
        BlockData oldLiveData = block.getBlockData();
        ReadWriteNBT oldLiveNbt = null;
        if (block.getState() instanceof TileState oldLiveState) {
            oldLiveNbt = NBT.createNBTObject();
            NBT.get(oldLiveState, oldLiveNbt::mergeCompound);
        }

        block.setBlockData(newBlockData, physics);

        // Set NBT for the new state, while ensuring the written block is a valid block entity
        if (readWriteNbt != null && block.getState() instanceof TileState newLiveState) {
            NBT.modify(newLiveState, nbt -> {
                nbt.mergeCompound(readWriteNbt);
            });
        }

        return new BlockUndoEntry(
            ((Number) activityContext.primaryKey()).longValue(),
            activityContext.worldUuid(),
            activityContext.coordinate(),
            oldLiveData,
            newBlockData,
            oldLiveNbt
        );
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
