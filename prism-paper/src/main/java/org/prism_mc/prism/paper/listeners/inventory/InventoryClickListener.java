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

package org.prism_mc.prism.paper.listeners.inventory;

import com.google.inject.Inject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.utils.ItemUtils;

public class InventoryClickListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public InventoryClickListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens to inventory click events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        // Rename some things for clarity
        ItemStack heldItem = event.getCursor();
        ItemStack slotItem = event.getCurrentItem();

        boolean clickedTopInventory =
            event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory());

        // Ignore:
        // - Clicks with the creative menu open
        // - Useless/unknown clicks
        // - Drop item events because those are tracked by the PlayerDropItemListener
        if (
            event.getClick().equals(ClickType.CREATIVE) ||
            event.getAction().equals(InventoryAction.NOTHING) ||
            event.getAction().equals(InventoryAction.UNKNOWN) ||
            event.getAction().equals(InventoryAction.DROP_ALL_CURSOR) ||
            event.getAction().equals(InventoryAction.DROP_ALL_SLOT) ||
            event.getAction().equals(InventoryAction.DROP_ONE_CURSOR) ||
            event.getAction().equals(InventoryAction.DROP_ONE_SLOT)
        ) {
            return;
        }

        // Ignore inventories without a holder - smithing tables, etc.
        // Things that have a UI but do not actually store items.
        if (event.getInventory().getHolder() == null) {
            return;
        }

        // Get the location of the top inventory
        Location location = event.getInventory().getLocation();
        if (location == null) {
            return;
        }

        // Ignore non-players or player's working only within their own inventory
        if (
            !(event.getWhoClicked() instanceof Player player) ||
            (event.getInventory().getHolder() instanceof Player other && other.equals(player))
        ) {
            return;
        }

        // We don't care about most actions that occur in the player's inventory
        if (!clickedTopInventory) {
            if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                if (ItemUtils.isValidItem(slotItem)) {
                    int quantityAccepted = ItemUtils.inventoryAcceptsQuantity(
                        event.getInventory(),
                        slotItem.getType(),
                        slotItem.getType().getMaxStackSize()
                    );

                    if (quantityAccepted > 0) {
                        recordItemInsertActivity(
                            location,
                            player,
                            slotItem,
                            Integer.min(quantityAccepted, slotItem.getAmount())
                        );
                    }
                }

                return;
            } else if (event.getAction().equals(InventoryAction.COLLECT_TO_CURSOR)) {
                int totalCollected = ItemUtils.countCollectedToCursor(
                    player.getInventory(),
                    event.getInventory(),
                    heldItem.getType(),
                    heldItem.getAmount()
                );
                if (totalCollected > 0) {
                    recordItemRemoveActivity(location, player, heldItem, totalCollected);
                }

                return;
            } else {
                return;
            }
        }

        // Handle all actions that can move items between two inventories
        switch (event.getAction()) {
            case COLLECT_TO_CURSOR -> {
                int totalCollected = ItemUtils.countCollectedToCursor(
                    player.getInventory(),
                    event.getInventory(),
                    heldItem.getType(),
                    heldItem.getAmount()
                );
                if (totalCollected > 0) {
                    recordItemRemoveActivity(location, player, heldItem, totalCollected);
                }
            }
            case HOTBAR_SWAP, PICKUP_ALL -> {
                recordItemRemoveActivity(location, player, slotItem);
            }
            case HOTBAR_MOVE_AND_READD -> {
                ItemStack swapItem = null;
                if (event.getHotbarButton() == -1) {
                    swapItem = player.getInventory().getItemInOffHand();
                } else {
                    swapItem = player.getInventory().getItem(event.getHotbarButton());
                }

                // No need to check if the item exists because this only fires when it does
                recordItemInsertActivity(location, player, swapItem);
                recordItemRemoveActivity(location, player, slotItem);
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                if (ItemUtils.isValidItem(slotItem)) {
                    int quantityAccepted = ItemUtils.inventoryAcceptsQuantity(
                        player.getInventory(),
                        slotItem.getType(),
                        slotItem.getType().getMaxStackSize()
                    );
                    if (quantityAccepted > 0) {
                        recordItemRemoveActivity(
                            location,
                            player,
                            slotItem,
                            Integer.min(quantityAccepted, slotItem.getAmount())
                        );
                    }
                }
            }
            case PICKUP_HALF -> {
                if (ItemUtils.isValidItem(slotItem)) {
                    recordItemRemoveActivity(location, player, slotItem, Integer.max(slotItem.getAmount() / 2, 1));
                }
            }
            case PICKUP_ONE -> {
                recordItemRemoveActivity(location, player, slotItem, 1);
            }
            case PLACE_ALL -> {
                recordItemInsertActivity(location, player, heldItem);
            }
            case PLACE_ONE -> {
                recordItemInsertActivity(location, player, heldItem, 1);
            }
            case PLACE_SOME -> {
                if (ItemUtils.isValidItem(slotItem)) {
                    recordItemInsertActivity(
                        location,
                        player,
                        heldItem,
                        heldItem.getMaxStackSize() - slotItem.getAmount()
                    );
                }
            }
            case SWAP_WITH_CURSOR -> {
                recordItemRemoveActivity(location, player, slotItem);
                recordItemInsertActivity(location, player, heldItem);
            }
            default -> {
                // Do nothing
            }
        }
    }
}
