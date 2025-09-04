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
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.utils.ItemUtils;

public class InventoryDragListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public InventoryDragListener(
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
    public void onInventoryDrag(final InventoryDragEvent event) {
        // Who clicks in an inv that isn't a player? Seriously! Just to be safe...
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Get the location of the top inventory
        Location location = event.getInventory().getLocation();

        // If null, it's virtual/transient so we can ignore
        if (location == null) {
            return;
        }

        // Ignore player's working only with their own inventory
        if (event.getInventory().getHolder() instanceof Player other && other.equals(player)) {
            return;
        }

        for (final Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            int rawSlot = entry.getKey();

            if (rawSlot < event.getInventory().getSize()) {
                ItemStack existingStack = event.getView().getItem(rawSlot);

                int slotViewAmount = ItemUtils.nullOrAir(existingStack) ? 0 : existingStack.getAmount();
                int amount = entry.getValue().getAmount() - slotViewAmount;

                if (amount > 0 && !ItemUtils.nullOrAir(entry.getValue())) {
                    recordItemInsertActivity(location, player, entry.getValue(), amount);
                }
            }
        }
    }
}
