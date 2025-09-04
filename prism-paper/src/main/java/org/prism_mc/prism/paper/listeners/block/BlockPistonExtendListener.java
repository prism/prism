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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperBlockAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class BlockPistonExtendListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public BlockPistonExtendListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for block shift events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        // Allow tracking block-break of dragon eggs even if block-shift false
        for (Block block : event.getBlocks()) {
            if (block.getPistonMoveReaction().equals(PistonMoveReaction.BREAK)) {
                recordBlockBreakAction(block, "piston");
            }
        }

        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().blockShift()) {
            return;
        }

        for (Block block : event.getBlocks()) {
            // Ignore blocks that we already tracked or won't be affected
            if (
                block.getType().equals(Material.AIR) ||
                block.getPistonMoveReaction().equals(PistonMoveReaction.BLOCK) ||
                block.getPistonMoveReaction().equals(PistonMoveReaction.BREAK) ||
                block.getPistonMoveReaction().equals(PistonMoveReaction.IGNORE)
            ) {
                continue;
            }

            Location newBlockLocation = block.getRelative(event.getDirection()).getLocation();

            var action = new PaperBlockAction(
                PaperActionTypeRegistry.BLOCK_SHIFT,
                block.getState(),
                newBlockLocation.getBlock().getState()
            );

            var activity = PaperActivity.builder().action(action).location(newBlockLocation).cause("piston").build();

            recordingService.addToQueue(activity);
        }
    }
}
