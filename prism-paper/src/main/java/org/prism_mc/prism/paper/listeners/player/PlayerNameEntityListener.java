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
import io.papermc.paper.event.player.PlayerNameEntityEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperEntityAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class PlayerNameEntityListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService   The expectation service
     * @param recordingService     The recording service
     */
    @Inject
    public PlayerNameEntityListener(
            ConfigurationService configurationService,
            ExpectationService expectationService,
            PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for entity rename events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerNameEntity(final PlayerNameEntityEvent event) {
        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().entityRename()) {
            return;
        }

        PaperEntityAction action;
        if (event.getName() != null) {
            var plainTextNewName = PlainTextComponentSerializer.plainText().serialize(event.getName());
            var metadata = Metadata.builder().newName(plainTextNewName).build();

            action = new PaperEntityAction(PaperActionTypeRegistry.ENTITY_RENAME, event.getEntity(), metadata);
        } else {
            action = new PaperEntityAction(PaperActionTypeRegistry.ENTITY_RENAME, event.getEntity());
        }

        var activity = PaperActivity.builder()
                .action(action)
                .location(event.getEntity().getLocation())
                .cause(event.getPlayer())
                .build();

        recordingService.addToQueue(activity);
    }
}
