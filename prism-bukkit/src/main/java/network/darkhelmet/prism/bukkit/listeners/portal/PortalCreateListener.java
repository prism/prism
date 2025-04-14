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

package network.darkhelmet.prism.bukkit.listeners.portal;

import com.google.inject.Inject;

import network.darkhelmet.prism.bukkit.actions.BukkitBlockAction;
import network.darkhelmet.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import network.darkhelmet.prism.bukkit.api.activities.BukkitActivity;
import network.darkhelmet.prism.bukkit.listeners.AbstractListener;
import network.darkhelmet.prism.bukkit.services.expectations.ExpectationService;
import network.darkhelmet.prism.bukkit.services.recording.BukkitRecordingService;
import network.darkhelmet.prism.loader.services.configuration.ConfigurationService;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

public class PortalCreateListener extends AbstractListener implements Listener {
    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PortalCreateListener(
            ConfigurationService configurationService,
            ExpectationService expectationService,
            BukkitRecordingService recordingService) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for portal create events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortalCreate(final PortalCreateEvent event) {
        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().blockPlace()) {
            return;
        }

        for (BlockState block : event.getBlocks()) {
            var action = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_PLACE, block);

            var builder = BukkitActivity.builder().action(action).location(block.getLocation());

            if (event.getEntity() instanceof Player player) {
                builder.player(player);
            } else if (event.getEntity() != null) {
                builder.cause(nameFromCause(event.getEntity()));
            } else {
                builder.cause("nature");
            }

            if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
                // Include only the nether portal blocks that were created
                // because the obsidian frame was already present
                if (block.getType().equals(Material.NETHER_PORTAL)) {
                    recordingService.addToQueue(builder.build());
                }
            } else {
                recordingService.addToQueue(builder.build());
            }
        }
    }
}
