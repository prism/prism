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
import com.google.inject.Singleton;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.api.containers.PaperPlayerContainer;
import org.prism_mc.prism.paper.services.filters.PaperFilterService;

@Singleton
public class PaperRecordingService implements RecordingService {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The filter service.
     */
    private final PaperFilterService filterService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The recording task.
     */
    private final RecordingTask recordingTask;

    /**
     * Set the recording mode.
     */
    private RecordMode recordMode = RecordMode.NORMAL;

    /**
     * Queue of activities.
     */
    private final LinkedBlockingQueue<Activity> queue;

    /**
     * Count of activities dropped due to a full queue since the last drain.
     */
    private final AtomicInteger droppedActivities = new AtomicInteger();

    /**
     * Number of currently active recording workers.
     */
    private final AtomicInteger activeWorkers = new AtomicInteger();

    /**
     * Maximum number of parallel recording workers.
     */
    private final int parallelism;

    /**
     * The drain mode.
     */
    private enum RecordMode {
        NORMAL,
        DRAIN_SYNC,
        STOPPED,
    }

    /**
     * Construct the recording manager.
     *
     * @param configurationService The configuration service
     * @param filterService The filter service
     * @param loggingService The logging service
     * @param recordingTask The recording task
     */
    @Inject
    public PaperRecordingService(
        ConfigurationService configurationService,
        PaperFilterService filterService,
        LoggingService loggingService,
        RecordingTask recordingTask
    ) {
        this.configurationService = configurationService;
        this.filterService = filterService;
        this.loggingService = loggingService;
        this.recordingTask = recordingTask;
        this.parallelism = configurationService.prismConfig().recording().parallelism();

        int capacity = configurationService.prismConfig().recording().queueMaxCapacity();
        this.queue = capacity > 0 ? new LinkedBlockingQueue<>(capacity) : new LinkedBlockingQueue<>();

        queueNextRecording(recordingTask);
    }

    @Override
    public boolean addToQueue(final Activity activity) {
        if (activity == null) {
            return false;
        }

        // Ignore players in creative if disabled globally
        if (
            configurationService.prismConfig().activities().ignoreCreative() &&
            activity instanceof PaperActivity paperActivity &&
            paperActivity.cause().container() instanceof PaperPlayerContainer paperPlayerContainer &&
            paperPlayerContainer.player().getGameMode().equals(GameMode.CREATIVE)
        ) {
            return false;
        }

        if (!filterService.shouldRecord(activity)) {
            return false;
        }

        if (!queue.offer(activity)) {
            if (droppedActivities.getAndIncrement() == 0) {
                loggingService.warn(
                    "Recording queue is full ({0}), dropping activities. The database may not be keeping up.",
                    queue.size()
                );
            }

            return false;
        }

        return true;
    }

    @Override
    public void clearTask() {
        activeWorkers.decrementAndGet();

        int dropped = droppedActivities.getAndSet(0);
        if (dropped > 0) {
            loggingService.warn("Dropped {0} activities due to a full recording queue.", dropped);
        }
    }

    /**
     * Drains the queue sync.
     */
    public void drainSync() {
        recordMode = RecordMode.DRAIN_SYNC;

        RecordingTask drainTask = this.recordingTask.toNew();
        while (!queue.isEmpty()) {
            drainTask.save();
        }
    }

    @Override
    public LinkedBlockingQueue<Activity> queue() {
        return queue;
    }

    @Override
    public void queueNextRecording(Runnable recordingTask) {
        long delay = configurationService.prismConfig().recording().delay();
        scheduleWorkers(delay);
    }

    /**
     * Schedule recording workers up to the configured parallelism.
     *
     * @param delay The delay in ticks before starting each worker
     */
    private void scheduleWorkers(long delay) {
        if (!recordMode.equals(RecordMode.NORMAL)) {
            return;
        }

        while (true) {
            int current = activeWorkers.get();
            if (current >= parallelism) {
                break;
            }

            if (!activeWorkers.compareAndSet(current, current + 1)) {
                continue;
            }

            Bukkit.getServer()
                .getScheduler()
                .runTaskLaterAsynchronously(PrismPaper.instance().loaderPlugin(), this.recordingTask.toNew(), delay);
        }
    }

    @Override
    public void stop() {
        recordMode = RecordMode.STOPPED;
    }
}
