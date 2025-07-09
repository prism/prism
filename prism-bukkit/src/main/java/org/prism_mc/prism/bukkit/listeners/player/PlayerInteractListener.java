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

package org.prism_mc.prism.bukkit.listeners.player;

import com.google.inject.Inject;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.Jukebox;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.services.wands.Wand;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.bukkit.actions.BukkitBlockAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.bukkit.services.wands.WandService;
import org.prism_mc.prism.bukkit.utils.ItemUtils;
import org.prism_mc.prism.bukkit.utils.TagLib;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class PlayerInteractListener extends AbstractListener implements Listener {

    /**
     * The wand service.
     */
    private final WandService wandService;

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     * @param wandService The wand service
     */
    @Inject
    public PlayerInteractListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService,
        WandService wandService
    ) {
        super(configurationService, expectationService, recordingService);
        this.wandService = wandService;
    }

    /**
     * Listen to player interact events (only for internal logic, not monitoring).
     *
     * @param event Tne event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        // Ignore if block is null (can't get location)
        // or if the event is fired for an off-hand click.
        // (Block will be null when clicking air)
        if (block == null || (event.getHand() != null && !event.getHand().equals(EquipmentSlot.HAND))) {
            return;
        }

        // Check if the player has a wand
        Optional<Wand> wand = wandService.getWand(player);
        if (wand.isPresent()) {
            // Left click = block's location
            // Right click = location of block connected to the clicked block face
            Location targetLocation = block.getLocation();
            if (event.getAction().equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
                targetLocation = block.getRelative(event.getBlockFace()).getLocation();
            }

            // Use the wand
            wand
                .get()
                .use(
                    targetLocation.getWorld().getUID(),
                    new Coordinate(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ())
                );

            // Cancel the event
            event.setCancelled(true);
        }
    }

    /**
     * Listen to player interact events (monitoring).
     *
     * @param event Tne event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractMonitor(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        // Ignore if block is null (can't get location)
        // or if the event is fired for an off-hand click.
        // (Block will be null when clicking air)
        if (block == null || (event.getHand() != null && !event.getHand().equals(EquipmentSlot.HAND))) {
            return;
        }

        if (event.useInteractedBlock().equals(Event.Result.DENY) || event.useItemInHand().equals(Event.Result.DENY)) {
            return;
        }

        final BlockState blockState = block.getState();
        final Location location = block.getLocation();
        final ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (
            event.getAction().equals(org.bukkit.event.block.Action.PHYSICAL) &&
            block.getType().equals(Material.FARMLAND)
        ) {
            // Record block break for crop
            Block blockAbove = block.getRelative(BlockFace.UP);
            if (Tag.CROPS.isTagged(blockAbove.getType())) {
                processBlockBreak(blockAbove, event.getPlayer());
            }

            return;
        }

        if (blockState instanceof ChiseledBookshelf chiseledBookshelf && event.getClickedPosition() != null) {
            var slot = chiseledBookshelf.getSlot(event.getClickedPosition());
            var item = chiseledBookshelf.getInventory().getItem(slot);

            if (item == null) {
                if (Tag.ITEMS_BOOKSHELF_BOOKS.isTagged(heldItem.getType())) {
                    recordItemInsertActivity(location, player, heldItem, 1);
                }
            } else {
                recordItemRemoveActivity(location, player, item, 1);
            }
        } else if (blockState instanceof Jukebox jukebox) {
            recordJukeboxActivity(jukebox, location, player);
        } else if (blockState instanceof Lectern lectern) {
            var lecternItem = lectern.getInventory().getItem(0);

            if (TagLib.LECTERN_ITEMS.isTagged(heldItem.getType()) && ItemUtils.nullOrAir(lecternItem)) {
                recordItemInsertActivity(location, player, heldItem);
            }
        } else if (blockState instanceof InventoryHolder inventoryHolder) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().inventoryOpen()) {
                return;
            }

            // Ignore inventory holders that don't open (chiseled bookshelf, decorated pot)
            if (
                inventoryHolder.getInventory().getType() == null ||
                inventoryHolder.getInventory().getType().getMenuType() == null
            ) {
                return;
            }

            var action = new BukkitBlockAction(BukkitActionTypeRegistry.INVENTORY_OPEN, blockState);

            var activity = BukkitActivity.builder().action(action).cause(player).location(location).build();

            recordingService.addToQueue(activity);
        } else if (TagLib.USABLE.isTagged(block.getType())) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().blockUse()) {
                return;
            }

            var action = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_USE, blockState);

            var activity = BukkitActivity.builder().action(action).cause(player).location(location).build();

            recordingService.addToQueue(activity);
        } else if (TagLib.USABLE_ITEMS.isTagged(heldItem.getType())) {
            recordItemUseActivity(location, player, heldItem);
        }
    }

    /**
     * Helper to record interactions with a jukebox.
     *
     * @param jukebox The jukebox
     * @param location The location
     * @param player The player
     */
    private void recordJukeboxActivity(Jukebox jukebox, Location location, Player player) {
        if (jukebox.isPlaying()) {
            if (!configurationService.prismConfig().actions().itemRemove()) {
                return;
            }

            recordItemRemoveActivity(location, player, new ItemStack(jukebox.getPlaying()));
        } else if (!ItemUtils.nullOrAir((player.getInventory().getItemInMainHand()))) {
            if (!configurationService.prismConfig().actions().itemInsert()) {
                return;
            }

            recordItemInsertActivity(location, player, player.getInventory().getItemInMainHand());
        }
    }
}
