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
        if (!recordingService.queue().isEmpty()) {
            try {
                int batchCount = 0;
                int batchMax = storageConfig.primaryDataSource().batchMax();

                ActivityBatch batch = storageAdapter.createActivityBatch();
                batch.startBatch();

                while (!recordingService.queue().isEmpty()) {
                    batchCount++;
                    batch.add(recordingService.queue().poll());

                    // Batch max exceeded, break
                    if (batchCount > batchMax) {
                        loggingService.debug(
                            "Recorder: Batch max exceeded, running insert. Queue remaining: {0}",
                            recordingService.queue().size()
                        );

                        break;
                    }
                }

                batch.commitBatch();
            } catch (Exception e) {
                loggingService.handleException(e);
            }
        }

        recordingService.clearTask();
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
