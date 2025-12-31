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

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.api.util.Pair;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class WorldEditIntegration {

    /**
     * The world edit instance.
     */
    private final WorldEditPlugin worldEdit;

    /**
     * The logging handler for WorldEdit operations.
     */
    private final WorldEditLoggingHandler loggingHandler;

    /**
     * Constructor.
     *
     * @param loggingService The logging service
     * @param worldEditPlugin The world edit bukkit plugin
     * @param recordingService The recording service
     * @param configurationService The configuration service
     */
    public WorldEditIntegration(
        LoggingService loggingService,
        Plugin worldEditPlugin,
        PaperRecordingService recordingService,
        ConfigurationService configurationService
    ) {
        this.worldEdit = (WorldEditPlugin) worldEditPlugin;

        // Create and register the logging handler
        this.loggingHandler = new WorldEditLoggingHandler(recordingService, configurationService, loggingService);
        WorldEdit.getInstance().getEventBus().register(loggingHandler);

        loggingService.info("Hooking into {0}", worldEditPlugin.getName());
    }

    /**
     * Get the minimum and maximum bounds of a player's selected region.
     *
     * @param player The player
     * @return The bounds
     */
    public Pair<Coordinate, Coordinate> getRegionBounds(Player player) {
        try {
            Region region = null;
            final BukkitPlayer lp = BukkitAdapter.adapt(player);
            final World lw = lp.getWorld();
            LocalSession session = worldEdit.getWorldEdit().getSessionManager().getIfPresent(lp);
            if (session != null) {
                region = session.getSelection(lw);
            }
            if (region == null) {
                return null;
            }

            final Coordinate minCoordinate = new Coordinate(
                region.getMinimumPoint().x(),
                region.getMinimumPoint().y(),
                region.getMinimumPoint().z()
            );

            final Coordinate maxCoordinate = new Coordinate(
                region.getMaximumPoint().x(),
                region.getMaximumPoint().y(),
                region.getMaximumPoint().z()
            );

            return new Pair<>(minCoordinate, maxCoordinate);
        } catch (final IncompleteRegionException e) {
            return null;
        }
    }
}
