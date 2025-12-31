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

import com.fastasyncworldedit.core.extent.processor.IBatchProcessorHolder;
import com.fastasyncworldedit.core.queue.implementation.ParallelQueueExtent;
import com.sk89q.worldedit.extent.Extent;
import lombok.experimental.UtilityClass;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

/**
 * Helper class for FAWE-specific logging functionality.
 * This class is only loaded when FAWE is present to avoid ClassNotFoundException.
 */
@UtilityClass
public class FaweLoggingHelper {

    /**
     * Check if the extent is a FAWE ParallelQueueExtent.
     *
     * @param extent The extent to check
     * @return true if the extent is a ParallelQueueExtent
     */
    public static boolean isParallelQueueExtent(Extent extent) {
        return extent instanceof ParallelQueueExtent;
    }

    /**
     * Add our processor to the FAWE ParallelQueueExtent.
     *
     * @param extent The ParallelQueueExtent
     * @param bukkitPlayer The Bukkit player (may be null)
     * @param bukkitWorld The Bukkit world
     * @param recordingService The recording service
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @return true if the processor was added successfully
     */
    public static boolean addProcessor(
        Extent extent,
        Player bukkitPlayer,
        World bukkitWorld,
        PaperRecordingService recordingService,
        ConfigurationService configurationService,
        LoggingService loggingService
    ) {
        if (!(extent instanceof ParallelQueueExtent parallelQueue)) {
            return false;
        }

        // Get the inner extent which holds the processor
        Extent innerExtent = parallelQueue.getExtent();
        if (!(innerExtent instanceof IBatchProcessorHolder processorHolder)) {
            loggingService.error("Failed to add FAWE processor: inner extent is not IBatchProcessorHolder");

            return false;
        }

        // Create our processor and chain with existing
        PrismBlockChangeProcessor prismProcessor = new PrismBlockChangeProcessor(
            bukkitPlayer,
            bukkitWorld,
            recordingService,
            configurationService
        );
        processorHolder.setProcessor(prismProcessor.join(processorHolder.getProcessor()));

        return true;
    }
}
