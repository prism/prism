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
import java.time.Duration;
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
import org.prism_mc.prism.paper.services.recording.wal.WalService;

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
     * The WAL service.
     */
    private final WalService walService;

    /**
     * Set the recording mode.
     */
    private RecordMode recordMode = RecordMode.NORMAL;

    /**
     * Queue of activities.
     */
    private final LinkedBlockingQueue<Activity> queue;

    /**
     * The activity aggregator.
     */
    private final ActivityAggregator aggregator;

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
     * @param walService The WAL service
     */
    @Inject
    public PaperRecordingService(
        ConfigurationService configurationService,
        PaperFilterService filterService,
        LoggingService loggingService,
        RecordingTask recordingTask,
        WalService walService
    ) {
        this.configurationService = configurationService;
        this.filterService = filterService;
        this.loggingService = loggingService;
        this.recordingTask = recordingTask;
        this.walService = walService;
        this.parallelism = configurationService.prismConfig().recording().parallelism();
        this.aggregator = new ActivityAggregator(configurationService.prismConfig().recording().aggregationInterval());

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

        if (
            configurationService.prismConfig().recording().aggregateActivities() &&
            activity.action().type().aggregatable()
        ) {
            aggregator.aggregate(activity);
            return true;
        }

        if (!offerToQueue(activity)) {
            return false;
        }

        return true;
    }

    /**
     * Offer an activity to the queue and write it to the WAL if enabled.
     *
     * @param activity The activity
     * @return True if the activity was accepted
     */
    private boolean offerToQueue(Activity activity) {
        if (!queue.offer(activity)) {
            if (droppedActivities.getAndIncrement() == 0) {
                loggingService.warn(
                    "Recording queue is full ({0}), dropping activities. The database may not be keeping up.",
                    queue.size()
                );
            }

            return false;
        }

        walService.append(activity);
        return true;
    }

    @Override
    public void clearTask() {
        activeWorkers.decrementAndGet();
    }

    @Override
    public int resetDroppedCount() {
        return droppedActivities.getAndSet(0);
    }

    @Override
    public void flushAggregator() {
        aggregator.flush(activity -> offerToQueue(activity));
    }

    /**
     * Drains the queue synchronously with a timeout.
     *
     * <p>Attempts to flush all pending activities to the database before
     * shutdown. Stops early if the timeout is exceeded or a batch fails,
     * to avoid blocking server shutdown indefinitely.</p>
     *
     * @param timeout Maximum time to spend draining
     */
    public void drainSync(Duration timeout) {
        recordMode = RecordMode.DRAIN_SYNC;
        long deadline = System.nanoTime() + timeout.toNanos();

        aggregator.flushAll(activity -> offerToQueue(activity));

        loggingService.info("Draining {0} queued activities (timeout: {1}s)...", queue.size(), timeout.toSeconds());

        RecordingTask drainTask = this.recordingTask.toNew();
        while (!queue.isEmpty()) {
            if (System.nanoTime() > deadline) {
                loggingService.warn("Drain timeout reached, {0} activities remain", queue.size());
                break;
            }

            try {
                drainTask.saveOrThrow();
            } catch (Exception e) {
                loggingService.handleException(e);
                loggingService.warn("Drain aborted due to error, {0} activities remain", queue.size());
                break;
            }
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
