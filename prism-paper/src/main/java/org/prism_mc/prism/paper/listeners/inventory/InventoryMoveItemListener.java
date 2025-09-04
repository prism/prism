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
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class InventoryMoveItemListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public InventoryMoveItemListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Event listener.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        if (event.getInitiator().getHolder() instanceof BlockState initiatorBlockState) {
            if (
                event.getSource().getHolder() instanceof BlockState sourceBlockState &&
                !sourceBlockState.getType().equals(Material.HOPPER)
            ) {
                if (!configurationService.prismConfig().actions().hopperRemove()) {
                    return;
                }

                recordItemActivity(
                    PaperActionTypeRegistry.HOPPER_REMOVE,
                    sourceBlockState.getLocation(),
                    initiatorBlockState,
                    event.getItem(),
                    event.getItem().getAmount()
                );
            }

            if (
                event.getDestination().getHolder() instanceof BlockState destBlockState &&
                !destBlockState.getType().equals(Material.HOPPER)
            ) {
                if (!configurationService.prismConfig().actions().hopperInsert()) {
                    return;
                }

                recordItemActivity(
                    PaperActionTypeRegistry.HOPPER_INSERT,
                    destBlockState.getLocation(),
                    initiatorBlockState,
                    event.getItem(),
                    event.getItem().getAmount()
                );
            }
        }
    }
}
