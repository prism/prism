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

package org.prism_mc.prism.paper.listeners.sign;

import com.google.inject.Inject;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperBlockAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

public class SignChangeListener extends AbstractListener implements Listener {

    /**
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     * @param prismScheduler The scheduler
     */
    @Inject
    public SignChangeListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService,
        PrismScheduler prismScheduler
    ) {
        super(configurationService, expectationService, recordingService);
        this.prismScheduler = prismScheduler;
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

        var lines = event
            .lines()
            .stream()
            .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
            .toArray(String[]::new);
        final Player player = event.getPlayer();
        final var block = event.getBlock();

        var signMetadata = Metadata.builder().signText(lines).build();
        var action = new PaperBlockAction(PaperActionTypeRegistry.SIGN_EDIT, block.getState(), null, signMetadata);

        var activity = PaperActivity.builder().action(action).location(block.getLocation()).cause(player).build();

        // The event fires before the new lines reach the block entity, and tile nbt is always
        // read live, so the post-edit snapshot has to wait a tick. Reading it back from the
        // sign lets the server encode the text in whatever format the running version uses.
        prismScheduler.runAtLocation(block.getLocation(), () -> {
            BlockState state = block.getState();
            if (state instanceof Sign) {
                action.captureReplacedTileState(state);
            }

            recordingService.addToQueue(activity);
        });
    }
}
