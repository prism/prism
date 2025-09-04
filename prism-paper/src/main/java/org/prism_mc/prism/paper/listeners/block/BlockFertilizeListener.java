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

package org.prism_mc.prism.paper.listeners.block;

import com.google.inject.Inject;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperBlockAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class BlockFertilizeListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public BlockFertilizeListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for block fertilize events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(final BlockFertilizeEvent event) {
        if (configurationService.prismConfig().actions().bonemealUse()) {
            var action = new PaperBlockAction(PaperActionTypeRegistry.BONEMEAL_USE, event.getBlock().getState());

            var builder = PaperActivity.builder().action(action).location(event.getBlock().getLocation());

            if (event.getPlayer() != null) {
                builder.cause(event.getPlayer());
            } else {
                builder.cause("unknown");
            }

            recordingService.addToQueue(builder.build());
        }

        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().blockPlace()) {
            return;
        }

        // Bonemeal use on trees/crops leads to duplicate info in the blocks list.
        if (event.getBlocks().size() <= 1) {
            return;
        }

        // When there's more than the origin block listed, it means bonemeal
        // affected a larger area and produced grass/flowers.
        for (BlockState blockState : event.getBlocks()) {
            var action = new PaperBlockAction(PaperActionTypeRegistry.BLOCK_PLACE, blockState);

            var builder = PaperActivity.builder().action(action).location(event.getBlock().getLocation());

            if (event.getPlayer() != null) {
                builder.cause(event.getPlayer());
            } else {
                builder.cause("unknown");
            }

            recordingService.addToQueue(builder.build());
        }
    }
}
