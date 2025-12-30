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

package org.prism_mc.prism.paper.integrations.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

/**
 * Handler that listens for WorldEdit EditSessionEvents and wraps
 * the extent with a PrismLoggingExtent to capture block changes.
 */
public class WorldEditLoggingHandler {

    /**
     * The recording service.
     */
    private final PaperRecordingService recordingService;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Whether FAWE is available.
     */
    private final boolean faweAvailable;

    /**
     * Construct the WorldEditLoggingHandler.
     *
     * @param recordingService The recording service
     * @param configurationService The configuration service
     * @param loggingService The logging service
     */
    public WorldEditLoggingHandler(
        PaperRecordingService recordingService,
        ConfigurationService configurationService,
        LoggingService loggingService
    ) {
        this.recordingService = recordingService;
        this.configurationService = configurationService;
        this.loggingService = loggingService;
        this.faweAvailable = isFawePresent();
    }

    /**
     * Check if FAWE classes are available.
     *
     * @return true if FAWE is present
     */
    private static boolean isFawePresent() {
        try {
            Class.forName("com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Handle EditSessionEvent to add our processor for FAWE or wrap extent for WorldEdit.
     *
     * @param event The EditSessionEvent
     */
    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        // Use BEFORE_CHANGE stage
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
            return;
        }

        // Check if either WorldEdit action is enabled
        if (
            !configurationService.prismConfig().actions().worldeditBreak() &&
            !configurationService.prismConfig().actions().worldeditPlace()
        ) {
            return;
        }

        // Get the actor (may be null for non-player operations)
        Player actor = null;
        if (event.getActor() != null && event.getActor() instanceof Player) {
            actor = (Player) event.getActor();
        }

        // Get the Bukkit world
        World weWorld = event.getWorld();
        if (weWorld == null) {
            return;
        }

        org.bukkit.World bukkitWorld = Bukkit.getWorld(weWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        // Get the Bukkit player if actor is available
        org.bukkit.entity.Player bukkitPlayer = null;
        if (actor != null) {
            bukkitPlayer = Bukkit.getPlayer(actor.getUniqueId());
        }

        // Check if FAWE is available and being used
        if (faweAvailable && FaweLoggingHelper.isParallelQueueExtent(event.getExtent())) {
            FaweLoggingHelper.addProcessor(
                event.getExtent(),
                bukkitPlayer,
                bukkitWorld,
                recordingService,
                configurationService,
                loggingService
            );
        } else {
            // Regular WorldEdit - use extent wrapper
            event.setExtent(
                new PrismLoggingExtent(
                    event.getExtent(),
                    bukkitPlayer,
                    bukkitWorld,
                    recordingService,
                    configurationService
                )
            );
        }
    }
}
