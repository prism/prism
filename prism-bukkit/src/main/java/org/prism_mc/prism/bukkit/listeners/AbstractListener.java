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

package org.prism_mc.prism.bukkit.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.bukkit.actions.BukkitBlockAction;
import org.prism_mc.prism.bukkit.actions.BukkitEntityAction;
import org.prism_mc.prism.bukkit.actions.BukkitItemStackAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.bukkit.utils.BlockUtils;
import org.prism_mc.prism.bukkit.utils.TagLib;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class AbstractListener {

    /**
     * The configuration service.
     */
    protected final ConfigurationService configurationService;

    /**
     * The expectation service.
     */
    protected final ExpectationService expectationService;

    /**
     * The recording service.
     */
    protected final BukkitRecordingService recordingService;

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    public AbstractListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService
    ) {
        this.configurationService = configurationService;
        this.expectationService = expectationService;
        this.recordingService = recordingService;
    }

    /**
     * Converts a cause to a string name.
     *
     * <p>Note: This is for non-players.</p>
     *
     * @param cause The cause
     * @return The cause name
     */
    protected String nameFromCause(Object cause) {
        String finalCause = null;
        if (cause instanceof Entity causeEntity) {
            if (causeEntity.getType().equals(EntityType.FALLING_BLOCK)) {
                finalCause = "gravity";
            } else {
                finalCause = causeEntity.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
            }
        } else if (cause instanceof EntityType causeEntityType) {
            finalCause = causeEntityType.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        } else if (cause instanceof Block causeBlock) {
            finalCause = causeBlock.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        } else if (cause instanceof BlockState causeBlockState) {
            finalCause = causeBlockState.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        } else if (cause instanceof BlockIgniteEvent.IgniteCause igniteCause) {
            finalCause = igniteCause.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        } else if (cause instanceof EntityDamageEvent.DamageCause damageCause) {
            finalCause = damageCause.name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        } else if (cause instanceof String causeStr) {
            finalCause = causeStr;
        }

        return finalCause;
    }

    /**
     * Process a block break. This looks for hanging items, detachables, etc.
     *
     * @param brokenBlock The block.
     * @param cause The cause.
     */
    protected void processBlockBreak(Block brokenBlock, Object cause) {
        final Block block = BlockUtils.rootBlock(brokenBlock);

        // Find any hanging entities
        if (configurationService.prismConfig().actions().hangingBreak()) {
            for (var hanging : block.getLocation().getNearbyEntitiesByType(Hanging.class, 5)) {
                expectationService.expectDetach(hanging, cause);
            }
        }

        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().blockBreak()) {
            return;
        }

        // Record all blocks that will detach
        for (Block detachable : BlockUtils.detachables(new ArrayList<>(), block)) {
            recordBlockBreakAction(detachable, cause);
        }

        // Record this block
        recordBlockBreakAction(block, cause);
    }

    /**
     * Process explosions.
     *
     * <p>This skips detachable logic because the affected
     * block lists will already include them.</p>
     *
     * <p>This skips checking for hanging items because
     * they're AIR by now.</p>
     *
     * @param affectedBlocks A list of affected blocks
     * @param cause The cause
     */
    protected void processExplosion(List<Block> affectedBlocks, Object cause) {
        if (configurationService.prismConfig().actions().blockBreak()) {
            for (Block affectedBlock : affectedBlocks) {
                // Ignore the tops of bisected blocks or heads of beds
                if (
                    (affectedBlock.getBlockData() instanceof Bisected bisected &&
                        bisected.getHalf().equals(Bisected.Half.TOP) &&
                        !(bisected instanceof Stairs) &&
                        !(bisected instanceof TrapDoor)) ||
                    (affectedBlock.getBlockData() instanceof Bed bed && bed.getPart().equals(Bed.Part.HEAD))
                ) {
                    continue;
                }

                // Record all blocks that will fall
                for (Block faller : BlockUtils.gravityAffectedBlocksAbove(new ArrayList<>(), affectedBlock)) {
                    // Skip blocks already in the affected block list
                    if (affectedBlocks.contains(faller)) {
                        continue;
                    }

                    recordBlockBreakAction(faller, cause);
                }

                // Record this block
                recordBlockBreakAction(affectedBlock, cause);
            }
        }
    }

    /**
     * Convenience method for recording a block break action.
     *
     * @param block The block
     * @param cause The cause
     */
    protected void recordBlockBreakAction(Block block, Object cause) {
        var action = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_BREAK, block.getState());

        recordItemDropFromBlockContents(block);

        var builder = BukkitActivity.builder().action(action).location(block.getLocation());
        if (cause instanceof String) {
            builder.cause((String) cause);
        } else if (cause instanceof Player player) {
            builder.player(player);
        }

        recordingService.addToQueue(builder.build());
    }

    /**
     * Record a hanging entity break.
     *
     * @param hanging The hanging entity
     * @param cause The cause
     */
    protected void recordHangingBreak(Entity hanging, Object cause) {
        var action = new BukkitEntityAction(BukkitActionTypeRegistry.HANGING_BREAK, hanging);

        var builder = BukkitActivity.builder().action(action).location(hanging.getLocation());
        if (cause instanceof Player player) {
            builder.player(player);
        } else {
            builder.cause(nameFromCause(cause));
        }

        recordingService.addToQueue(builder.build());
    }

    /**
     * Record an item drop activity.
     *
     * @param location The location
     * @param cause The cause (Player or a named cause string)
     * @param itemStack The item stack
     */
    protected void recordItemDropActivity(Location location, Object cause, ItemStack itemStack) {
        recordItemDropActivity(location, cause, itemStack, itemStack.getAmount());
    }

    /**
     * Record an item drop activity.
     *
     * @param location The location
     * @param cause The cause (Player or a named cause string)
     * @param itemStack The item stack
     * @param amount The amount
     */
    protected void recordItemDropActivity(Location location, Object cause, ItemStack itemStack, Integer amount) {
        if (!configurationService.prismConfig().actions().itemDrop()) {
            return;
        }

        Player player = null;
        String namedCause = null;

        if (cause instanceof Player _player) {
            player = _player;
        } else {
            namedCause = nameFromCause(cause);
        }

        recordItemActivity(BukkitActionTypeRegistry.ITEM_DROP, location, player, namedCause, itemStack, amount);
    }

    /**
     * Record inventory item drops from a block break. Ignores blocks without an inventory.
     *
     * @param block The block
     */
    protected void recordItemDropFromBlockContents(Block block) {
        if (TagLib.KEEPS_INVENTORY.isTagged(block.getType())) {
            return;
        }

        if (block.getState() instanceof InventoryHolder holder) {
            var inventory = holder.getInventory();

            if (holder.getInventory() instanceof DoubleChestInventory doubleChestInventory) {
                var chest = (Chest) block.getBlockData();

                // Only log this block's side of a double chest
                // This is a bit bizarre but chest.getType() will be LEFT/RIGHT from the perspective
                // opposite from where the player would see a chest.
                //
                // Per the spigot javadocs: "Left and right are relative to the chest itself,
                // i.e. opposite to what a player placing the appropriate block would see"
                //
                // However, it seems like DoubleChestInventory's getLeftSide/getRightSide
                // are from the perspective of the player.
                //
                // This means that when the broken chest block is type LEFT
                // the inventory on the RIGHT is what actually drops its contents.
                if (chest.getType() == Chest.Type.LEFT) {
                    inventory = doubleChestInventory.getRightSide();
                } else {
                    inventory = doubleChestInventory.getLeftSide();
                }
            }

            recordItemDropFromInventory(
                inventory,
                block.getLocation(),
                String.format("broken %s", nameFromCause(block))
            );
        }
    }

    /**
     * Record item drops from a given inventory.
     *
     * @param inventory The inventory
     * @param location The location
     * @param cause The player or named cause
     */
    protected void recordItemDropFromInventory(Inventory inventory, Location location, Object cause) {
        if (!configurationService.prismConfig().actions().itemDrop()) {
            return;
        }

        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                recordItemDropActivity(location, cause, item, null);
            }
        }
    }

    /**
     * Record an item insert activity.
     *
     * @param location The location
     * @param player The player
     * @param itemStack The item stack
     * @param amount The amount
     */
    protected void recordItemInsertActivity(Location location, Player player, ItemStack itemStack, int amount) {
        if (!configurationService.prismConfig().actions().itemInsert()) {
            return;
        }

        recordItemActivity(BukkitActionTypeRegistry.ITEM_INSERT, location, player, null, itemStack, amount);
    }

    /**
     * Record an item insert activity.
     *
     * @param location The location
     * @param player The player
     * @param itemStack The item stack
     */
    protected void recordItemInsertActivity(Location location, Player player, ItemStack itemStack) {
        if (!configurationService.prismConfig().actions().itemInsert()) {
            return;
        }

        recordItemActivity(BukkitActionTypeRegistry.ITEM_INSERT, location, player, null, itemStack, null);
    }

    /**
     * Record an item remove activity.
     *
     * @param location The location
     * @param player The player
     * @param itemStack The item stack
     */
    protected void recordItemRemoveActivity(Location location, Player player, ItemStack itemStack) {
        if (!configurationService.prismConfig().actions().itemInsert()) {
            return;
        }

        recordItemActivity(BukkitActionTypeRegistry.ITEM_REMOVE, location, player, null, itemStack, null);
    }

    /**
     * Record an item remove activity.
     *
     * @param location The location
     * @param player The player
     * @param itemStack The item stack
     * @param amount The amount
     */
    protected void recordItemRemoveActivity(Location location, Player player, ItemStack itemStack, int amount) {
        if (!configurationService.prismConfig().actions().itemInsert()) {
            return;
        }

        recordItemActivity(BukkitActionTypeRegistry.ITEM_REMOVE, location, player, null, itemStack, amount);
    }

    /**
     * Record an item use activity.
     *
     * @param location The location
     * @param player The player
     * @param itemStack The item stack
     */
    protected void recordItemUseActivity(Location location, Player player, ItemStack itemStack) {
        if (!configurationService.prismConfig().actions().itemUse()) {
            return;
        }

        recordItemActivity(BukkitActionTypeRegistry.ITEM_USE, location, player, null, itemStack, 1);
    }

    /**
     * Record AN item insert/remove activity.
     *
     * @param actionType The action type
     * @param location The location
     * @param player The player
     * @param cause The cause
     * @param itemStack The item stack
     * @param amount The amount
     */
    private void recordItemActivity(
        ActionType actionType,
        Location location,
        Player player,
        String cause,
        ItemStack itemStack,
        Integer amount
    ) {
        // Clone the item stack and set the quantity because
        // this is what we use to record the action
        ItemStack clonedStack = itemStack.clone();

        if (amount != null) {
            clonedStack.setAmount(amount);
        }

        var action = new BukkitItemStackAction(actionType, clonedStack);

        var builder = BukkitActivity.builder().action(action).location(location);
        if (player != null) {
            builder.player(player);
        }

        if (cause != null) {
            builder.cause(cause);
        }

        recordingService.addToQueue(builder.build());
    }
}
