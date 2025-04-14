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

package network.darkhelmet.prism.bukkit.listeners.block;

import com.google.inject.Inject;

import network.darkhelmet.prism.bukkit.actions.BukkitBlockAction;
import network.darkhelmet.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import network.darkhelmet.prism.bukkit.api.activities.BukkitActivity;
import network.darkhelmet.prism.bukkit.listeners.AbstractListener;
import network.darkhelmet.prism.bukkit.services.expectations.ExpectationService;
import network.darkhelmet.prism.bukkit.services.recording.BukkitRecordingService;
import network.darkhelmet.prism.bukkit.utils.TagLib;
import network.darkhelmet.prism.loader.services.configuration.ConfigurationService;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

public class BlockFromToListener extends AbstractListener implements Listener {
    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public BlockFromToListener(
            ConfigurationService configurationService,
            ExpectationService expectationService,
            BukkitRecordingService recordingService) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for block from/to events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(final BlockFromToEvent event) {
        final BlockState fromState = event.getBlock().getState();
        final BlockState toState = event.getToBlock().getState();

        // If the liquid is flowing to a detachable block, log it
        if (TagLib.FLUID_BREAKABLE.isTagged(toState.getType())) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().blockBreak()) {
                return;
            }

            final Block block = event.getBlock();
            var action = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_BREAK, toState);

            var activity = BukkitActivity.builder()
                .action(action)
                .cause(nameFromCause(fromState))
                .location(block.getLocation())
                .build();

            recordingService.addToQueue(activity);

            return;
        }

        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().fluidFlow()) {
            return;
        }

        var action = new BukkitBlockAction(BukkitActionTypeRegistry.FLUID_FLOW, toState);

        var activity = BukkitActivity.builder()
            .action(action)
            .cause(nameFromCause(fromState))
            .location(fromState.getLocation())
            .build();

        recordingService.addToQueue(activity);
    }
}
