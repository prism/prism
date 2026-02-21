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

package org.prism_mc.prism.paper.services.recording;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.storage.ActivityBatch;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.storage.StorageConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class RecordingTask implements Runnable {

    /**
     * The storage config.
     */
    private final StorageConfiguration storageConfig;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The recording manager.
     */
    private final RecordingService recordingService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Construct a new recording task.
     *
     * @param storageConfig The storage config
     * @param storageAdapter The storage adapter
     * @param recordingService The recording service
     * @param loggingService The logging service
     */
    @Inject
    public RecordingTask(
        StorageConfiguration storageConfig,
        StorageAdapter storageAdapter,
        RecordingService recordingService,
        LoggingService loggingService
    ) {
        this.storageConfig = storageConfig;
        this.storageAdapter = storageAdapter;
        this.recordingService = recordingService;
        this.loggingService = loggingService;
    }

    @Override
    public void run() {
        save();

        // Schedule the next recording
        recordingService.queueNextRecording(
            new RecordingTask(storageConfig, storageAdapter, recordingService, loggingService)
        );
    }

    /**
     * Saves anything in the queue, or as many as we can.
     */
    public void save() {
        recordingService.flushAggregator();

        // Reset the drop counter so we only track drops during this cycle
        recordingService.resetDroppedCount();

        try {
            saveOrThrow();
        } catch (Exception e) {
            loggingService.handleException(e);
        }

        int dropped = recordingService.resetDroppedCount();
        if (dropped > 0) {
            loggingService.warn("Dropped {0} activities due to a full recording queue.", dropped);
        }

        recordingService.clearTask();
    }

    /**
     * Saves anything in the queue, or as many as we can.
     *
     * @throws Exception If the batch commit fails
     */
    public void saveOrThrow() throws Exception {
        if (!recordingService.queue().isEmpty()) {
            int batchMax = storageConfig.primaryDataSource().batchMax();

            List<Activity> drained = new ArrayList<>(batchMax);
            recordingService.queue().drainTo(drained, batchMax);

            if (!drained.isEmpty()) {
                ActivityBatch batch = storageAdapter.createActivityBatch();
                batch.startBatch();

                for (Activity activity : drained) {
                    batch.add(activity);
                }

                batch.commitBatch();
            }
        }
    }

    /**
     * Create a new recording task.
     *
     * @return The recording task
     */
    public RecordingTask toNew() {
        return new RecordingTask(storageConfig, storageAdapter, recordingService, loggingService);
    }
}
