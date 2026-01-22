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
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.GenericPaperAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class AsyncPlayerChatListener extends AbstractListener implements Listener {

    /**
     * Constructor.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public AsyncPlayerChatListener(
        final ConfigurationService configurationService,
        final ExpectationService expectationService,
        final PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Event listener.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(final AsyncChatEvent event) {
        final Player player = event.getPlayer();

        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().playerChat()) {
            return;
        }

        final String chatMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        final String originalChatMessage = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());

        final GenericPaperAction action;
        if (chatMessage.equals(originalChatMessage)) {
            action = new GenericPaperAction(PaperActionTypeRegistry.PLAYER_CHAT, chatMessage);
        } else {
            final var messageMetadata = Metadata.builder().originalMessage(originalChatMessage).build();
            action = new GenericPaperAction(PaperActionTypeRegistry.PLAYER_CHAT, chatMessage, messageMetadata);
        }

        final var activity = PaperActivity.builder()
            .action(action)
            .location(player.getLocation())
            .cause(player)
            .build();
        recordingService.addToQueue(activity);
    }
}
