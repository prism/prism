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

package org.prism_mc.prism.bukkit.listeners.sign;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.bukkit.actions.BukkitBlockAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class SignChangeListener extends AbstractListener implements Listener {

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The object mapper
     */
    final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     * @param loggingService The logging service
     */
    @Inject
    public SignChangeListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService,
        LoggingService loggingService
    ) {
        super(configurationService, expectationService, recordingService);
        this.loggingService = loggingService;
    }

    /**
     * Listens for sign change events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(final SignChangeEvent event) {
        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().signEdit()) {
            return;
        }

        try {
            var lines = event
                .lines()
                .stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .toArray(String[]::new);
            final Player player = event.getPlayer();

            var signMetadata = Metadata.builder().signText(lines).build();
            var action = new BukkitBlockAction(
                BukkitActionTypeRegistry.SIGN_EDIT,
                event.getBlock().getState(),
                null,
                signMetadata
            );
            var side = event.getSide().equals(Side.FRONT) ? "front_text" : "back_text";

            // Because the block state doesn't have the new lines from a sign change event,
            // we need to fake it by merging in the text so it gets recorded.
            action.mergeCompound(String.format("{%s:{messages:[%s]}}", side, objectMapper.writeValueAsString(lines)));

            var activity = BukkitActivity.builder()
                .action(action)
                .location(event.getBlock().getLocation())
                .cause(player)
                .build();

            recordingService.addToQueue(activity);
        } catch (JsonProcessingException e) {
            loggingService.handleException(e);
        }
    }
}
