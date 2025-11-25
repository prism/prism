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

package org.prism_mc.prism.paper.listeners.player;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.GenericPaperAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class PlayerCommandPreprocessListener extends AbstractListener implements Listener {

    /**
     * Constructor.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PlayerCommandPreprocessListener(
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
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();

        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().playerCommand()) {
            return;
        }

        String commandMessage = event.getMessage();

        for (var command : configurationService.prismConfig().activities().sensitiveCommands()) {
            if (event.getMessage().startsWith(String.format("/%s", command))) {
                commandMessage = String.format("/%s ...", command);
            }
        }

        var action = new GenericPaperAction(PaperActionTypeRegistry.PLAYER_COMMAND, commandMessage);
        var activity = PaperActivity.builder().action(action).location(player.getLocation()).cause(player).build();
        recordingService.addToQueue(activity);
    }
}
