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

package network.darkhelmet.prism.bukkit.listeners.player;

import com.google.inject.Inject;

import java.util.Optional;

import network.darkhelmet.prism.api.actions.Action;
import network.darkhelmet.prism.api.services.wands.Wand;
import network.darkhelmet.prism.api.util.Coordinate;
import network.darkhelmet.prism.bukkit.actions.BukkitBlockAction;
import network.darkhelmet.prism.bukkit.actions.BukkitItemStackAction;
import network.darkhelmet.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import network.darkhelmet.prism.bukkit.api.activities.BukkitActivity;
import network.darkhelmet.prism.bukkit.listeners.AbstractListener;
import network.darkhelmet.prism.bukkit.services.expectations.ExpectationService;
import network.darkhelmet.prism.bukkit.services.recording.BukkitRecordingService;
import network.darkhelmet.prism.bukkit.services.wands.WandService;
import network.darkhelmet.prism.bukkit.utils.TagLib;
import network.darkhelmet.prism.loader.services.configuration.ConfigurationService;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
            WandService wandService) {
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
            wand.get().use(targetLocation.getWorld().getUID(),
                new Coordinate(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ()));

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

        if (event.getAction().equals(org.bukkit.event.block.Action.PHYSICAL)
                && block.getType().equals(Material.FARMLAND)) {
            // Record block break for crop
            Block blockAbove = block.getRelative(BlockFace.UP);
            if (Tag.CROPS.isTagged(blockAbove.getType())) {
                processBlockBreak(blockAbove, event.getPlayer());
            }

            return;
        }

        if (event.getClickedBlock().getState() instanceof Jukebox jukebox) {
            recordJukeboxActivity(jukebox, event.getClickedBlock().getLocation(), player);
        } else if (event.getClickedBlock().getState() instanceof InventoryHolder inventoryHolder) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().inventoryOpen()) {
                return;
            }

            // Ignore inventory holders that don't open (chiseled bookshelf, decorated pot)
            if (inventoryHolder.getInventory().getType() == null
                    || inventoryHolder.getInventory().getType().getMenuType() == null) {
                return;
            }

            var action = new BukkitBlockAction(
                BukkitActionTypeRegistry.INVENTORY_OPEN, event.getClickedBlock().getState());

            var activity = BukkitActivity.builder()
                .action(action)
                .player(player)
                .location(event.getClickedBlock().getLocation())
                .build();

            recordingService.addToQueue(activity);
        } else if (TagLib.USABLE.isTagged(event.getClickedBlock().getType())) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().blockUse()) {
                return;
            }

            var action = new BukkitBlockAction(
                BukkitActionTypeRegistry.BLOCK_USE, event.getClickedBlock().getState());

            var activity = BukkitActivity.builder()
                .action(action)
                .player(player)
                .location(event.getClickedBlock().getLocation())
                .build();

            recordingService.addToQueue(activity);
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
        final Action action;
        if (jukebox.isPlaying()) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().itemRemove()) {
                return;
            }

            action = new BukkitItemStackAction(
                BukkitActionTypeRegistry.ITEM_REMOVE, new ItemStack(jukebox.getPlaying()));
        } else {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().itemInsert()) {
                return;
            }

            action = new BukkitItemStackAction(
                BukkitActionTypeRegistry.ITEM_INSERT, player.getInventory().getItemInMainHand());
        }

        var activity = BukkitActivity.builder()
            .action(action)
            .player(player)
            .location(location)
            .build();

        recordingService.addToQueue(activity);
    }
}
