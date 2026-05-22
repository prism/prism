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

package org.prism_mc.prism.paper.services.modifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ActivityStream;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationResultStatus;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.UndoEntry;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.ModificationConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;
import org.prism_mc.prism.paper.utils.BlockUtils;
import org.prism_mc.prism.paper.utils.EntityUtils;

public abstract class AbstractWorldModificationQueue implements ModificationQueue {

    /**
     * Multiplier applied to maxPerTask when sizing each streaming refill.
     */
    private static final int BATCH_FETCH_MULTIPLIER = 5;

    /**
     * The logging service.
     */
    protected LoggingService loggingService;

    /**
     * The configuration service.
     */
    protected ConfigurationService configurationService;

    /**
     * The message service.
     */
    protected final MessageService messageService;

    /**
     * The storage adapter.
     */
    protected final StorageAdapter storageAdapter;

    /**
     * The scheduler.
     */
    protected final PrismScheduler prismScheduler;

    /**
     * The modification executor.
     */
    protected final ModificationExecutor modificationExecutor;

    /**
     * The modification ruleset.
     */
    protected ModificationRuleset modificationRuleset;

    /**
     * The streaming activity source. Pulled in batches as the queue drains.
     */
    protected final ActivityStream activityStream;

    /**
     * The current in-memory batch handed to the executor. Refilled from the stream between executor runs.
     */
    protected final List<Activity> modificationsQueue = Collections.synchronizedList(new ArrayList<>());

    /**
     * The onEnd handler.
     */
    protected final Consumer<ModificationQueueResult> onEndCallback;

    /**
     * The owner.
     */
    @Getter
    protected final Object owner;

    @Getter
    protected final ActivityQuery query;

    /**
     * The state.
     */
    @Getter
    protected ModificationQueueMode mode = ModificationQueueMode.UNDECIDED;

    /**
     * Count how many were read from the queue.
     */
    protected int countModificationsRead;

    /**
     * Total results processed across all statuses. Tracked separately from
     * {@link #results} because APPLIED/PLANNED results are not retained there.
     */
    protected int countProcessed = 0;

    /**
     * Count how many were applied.
     */
    protected int countApplied = 0;

    /**
     * Count how many were partially applied.
     */
    protected int countPartial = 0;

    /**
     * Count how many were planned.
     */
    protected int countPlanned = 0;

    /**
     * Count how many were skipped.
     */
    protected int countSkipped = 0;

    /**
     * Counters accumulated by pre/post-process across region threads. Guarded by
     * {@code this} — pre/post-process may run on different region threads on Folia.
     */
    protected int countDrainedLava = 0;
    protected int countRemovedBlocks = 0;
    protected int countRemovedDrops = 0;
    protected int countMovedEntities = 0;

    /**
     * Non-applied results retained for {@code /pr report partial} and
     * {@code /pr report skips}. Applied results are converted to
     * {@link #undoEntries} and discarded; otherwise the list would pin one
     * full Activity per applied block.
     */
    protected final List<ModificationResult> results = new ArrayList<>();

    /**
     * Snapshot of the queue size at execute() time. Used as the denominator
     * for progress reporting; the live queue size shrinks in COMPLETING mode
     * as items are processed and is not a useful total.
     */
    private int progressTotal;

    /**
     * Last progress percent reported to the owner. Updated only when a new
     * progress-report step threshold is crossed.
     */
    private int progressLastReportedPercent;

    /**
     * Per-applied-block undo snapshots. Captured live from the world before
     * the queue overwrote each block. Used by {@code /pr undo} to replay
     * world state without re-querying the activity log.
     */
    protected final List<UndoEntry> undoEntries = new ArrayList<>();

    /**
     * Primary keys of activities applied during the current batch, awaiting a
     * {@code markReversed} flush. Drained after each batch so the IN list size
     * stays bounded and we don't hit per-statement limits on rollbacks of
     * hundreds of thousands of activities.
     */
    private final List<Long> pendingReversalKeys = new ArrayList<>();

    /**
     * Set once a {@code markReversed} flush has failed and the owner has been
     * notified, so subsequent batch failures don't spam the same error.
     */
    private boolean reversalErrorReported = false;

    /**
     * Incrementally tracked bounding box min/max from result coordinates.
     */
    private double bbMinX = Double.MAX_VALUE;
    private double bbMinY = Double.MAX_VALUE;
    private double bbMinZ = Double.MAX_VALUE;
    private double bbMaxX = -Double.MAX_VALUE;
    private double bbMaxY = -Double.MAX_VALUE;
    private double bbMaxZ = -Double.MAX_VALUE;
    private boolean bbHasCoordinates = false;

    /**
     * Whether pre-processing has already run for this operation. Pre-process
     * (drain lava, remove blocks, remove drops) must only fire on the first
     * batch; subsequent refills pass a null pre-processor.
     */
    private boolean preProcessRan = false;

    /**
     * Whether the operation has been canceled or finalized.
     */
    private volatile boolean cancelled = false;

    /**
     * Construct a new world modification.
     *
     * @param loggingService The logging service
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     * @param prismScheduler The scheduler
     * @param modificationExecutor The modification executor
     * @param modificationRuleset Modification rule set
     * @param owner The owner
     * @param query The query
     * @param activityStream The streaming activity source
     * @param onEndCallback The ended callback
     */
    public AbstractWorldModificationQueue(
        LoggingService loggingService,
        ConfigurationService configurationService,
        MessageService messageService,
        StorageAdapter storageAdapter,
        PrismScheduler prismScheduler,
        ModificationExecutor modificationExecutor,
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream,
        Consumer<ModificationQueueResult> onEndCallback
    ) {
        this.loggingService = loggingService;
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
        this.prismScheduler = prismScheduler;
        this.modificationExecutor = modificationExecutor;
        this.modificationRuleset = modificationRuleset;
        this.owner = owner;
        this.query = query;
        this.activityStream = activityStream;
        this.onEndCallback = onEndCallback;
    }

    @Override
    public int queueSize() {
        if (cancelled) {
            return 0;
        }
        return Math.max(0, activityStream.total() - countProcessed);
    }

    @Override
    public int total() {
        return progressTotal;
    }

    @Override
    public int processed() {
        return countProcessed;
    }

    /**
     * Apply a modification.
     *
     * @param activity The activity
     * @return The modification result
     */
    protected ModificationResult applyModification(Activity activity) {
        return ModificationResult.builder().skipped().build();
    }

    /**
     * Apply pre-modification tasks within the given region bounds. The actual
     * area affected is the intersection of the modification bounding box and
     * the region bounds. On Paper, regionBounds covers the full world. On Folia,
     * regionBounds is clipped to the current region.
     *
     * @param world The world
     * @param regionBounds The region-safe bounding box to clip operations to
     */
    protected void preProcess(World world, BoundingBox regionBounds) {
        BoundingBox effectiveBox = modificationBoundingBox().intersection(regionBounds);
        if (effectiveBox.getVolume() <= 0) {
            return;
        }

        synchronized (this) {
            if (modificationRuleset.drainLava()) {
                countDrainedLava += BlockUtils.removeBlocksByMaterial(
                    world,
                    effectiveBox,
                    List.of(Material.LAVA)
                ).size();
            }

            if (modificationRuleset.removeDrops()) {
                countRemovedDrops += EntityUtils.removeDropsInRange(world, effectiveBox);
            }

            if (!modificationRuleset.removeBlocks().isEmpty()) {
                List<Material> materials = modificationRuleset
                    .removeBlocks()
                    .stream()
                    .map(m -> Material.valueOf(m.toUpperCase()))
                    .toList();

                countRemovedBlocks += BlockUtils.removeBlocksByMaterial(world, effectiveBox, materials).size();
            }
        }
    }

    /**
     * Apply post-modification tasks within the given region bounds. The actual
     * area affected is the intersection of the modification bounding box and
     * the region bounds.
     *
     * @param world The world
     * @param regionBounds The region-safe bounding box to clip operations to
     */
    protected void postProcess(World world, BoundingBox regionBounds) {
        if (!modificationRuleset.moveEntities() || results.isEmpty()) {
            return;
        }

        BoundingBox effectiveBox = modificationBoundingBox().intersection(regionBounds);
        if (effectiveBox.getVolume() <= 0) {
            return;
        }

        int count = EntityUtils.moveEntitiesToGround(world, effectiveBox, prismScheduler);
        synchronized (this) {
            countMovedEntities += count;
        }
    }

    /**
     * Get the modification's bounding box.
     *
     * @return The result set bounding box, the query's, or empty
     */
    private BoundingBox modificationBoundingBox() {
        var boundingBox = boundingBoxFromResults();

        if (
            boundingBox == null &&
            query.worldUuid() != null &&
            query.minCoordinate() != null &&
            query.maxCoordinate() != null
        ) {
            boundingBox = boundingBoxFromQuery();
        }

        if (boundingBox == null || checkBoundingBoxExceedsLimits(boundingBox)) {
            return new BoundingBox();
        }

        // Expand by 1 block so entities on/adjacent to affected blocks are included
        boundingBox.expand(1);

        return boundingBox;
    }

    /**
     * Compute a bounding box from the min/max coordinates of query.
     *
     * @return A bounding box
     */
    private BoundingBox boundingBoxFromQuery() {
        double x1 = query.minCoordinate().x();
        double y1 = query.minCoordinate().y();
        double z1 = query.minCoordinate().z();
        double x2 = query.maxCoordinate().x();
        double y2 = query.maxCoordinate().y();
        double z2 = query.maxCoordinate().z();
        return new BoundingBox(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Update the incrementally tracked bounding box with a new result's coordinate.
     *
     * @param result The modification result
     */
    private void trackBoundingBox(ModificationResult result) {
        Coordinate coordinate = result.activity().coordinate();
        if (coordinate == null) {
            return;
        }

        bbHasCoordinates = true;
        bbMinX = Math.min(bbMinX, coordinate.x());
        bbMinY = Math.min(bbMinY, coordinate.y());
        bbMinZ = Math.min(bbMinZ, coordinate.z());
        bbMaxX = Math.max(bbMaxX, coordinate.x());
        bbMaxY = Math.max(bbMaxY, coordinate.y());
        bbMaxZ = Math.max(bbMaxZ, coordinate.z());
    }

    /**
     * Get the bounding box from the incrementally tracked min/max coordinates.
     *
     * @return A bounding box, or null if no results have coordinates
     */
    private BoundingBox boundingBoxFromResults() {
        if (!bbHasCoordinates) {
            return null;
        }

        return new BoundingBox(bbMinX, bbMinY, bbMinZ, bbMaxX, bbMaxY, bbMaxZ);
    }

    /**
     * Enforces limits on bounding boxes for pre-and-post-modification actions.
     *
     * @param boundingBox - Bounding box
     * @return True if within the limits
     */
    private boolean checkBoundingBoxExceedsLimits(BoundingBox boundingBox) {
        var limit = configurationService.prismConfig().modifications().maxQueryBoundingBoxLength();
        return boundingBox.getHeight() > limit || boundingBox.getWidthX() > limit || boundingBox.getWidthZ() > limit;
    }

    @Override
    public void apply() {
        resetState(ModificationQueueMode.COMPLETING);
        announceStart();
        fetchAndRunNextBatch();
    }

    /**
     * Reset counters and prepare the result builder for a fresh apply or preview run.
     *
     * @param newMode The mode this run will operate in
     */
    protected void resetState(ModificationQueueMode newMode) {
        countModificationsRead = 0;
        countProcessed = 0;
        countApplied = 0;
        countPartial = 0;
        countPlanned = 0;
        countSkipped = 0;
        countDrainedLava = 0;
        countRemovedBlocks = 0;
        countRemovedDrops = 0;
        countMovedEntities = 0;
        results.clear();
        undoEntries.clear();
        modificationsQueue.clear();
        pendingReversalKeys.clear();
        reversalErrorReported = false;
        preProcessRan = false;
        cancelled = false;
        activityStream.reopen();
        progressTotal = activityStream.total();
        progressLastReportedPercent = 0;
        this.mode = newMode;
    }

    /**
     * Send starting message.
     */
    private void announceStart() {
        if (progressTotal >= configurationService.prismConfig().modifications().progressReportThreshold()) {
            sendToOwner(receiver -> messageService.modificationsStarting(receiver, progressTotal));
        }
    }

    /**
     * Pull the next batch off the activity stream (async, off the region
     * thread), then resume executor scheduling on the global region. When
     * the stream is exhausted, finalize the result and fire the end callback.
     */
    private void fetchAndRunNextBatch() {
        if (cancelled) {
            return;
        }

        int batchSize = Math.max(modificationRuleset.maxPerTask() * BATCH_FETCH_MULTIPLIER, 1);

        prismScheduler.runAsync(() -> {
            List<Activity> batch;
            try {
                batch = activityStream.next(batchSize);
            } catch (Exception e) {
                loggingService.handleException(e);
                prismScheduler.runGlobal(this::finalizeResult);
                return;
            }

            if (cancelled) {
                return;
            }

            if (batch.isEmpty()) {
                prismScheduler.runGlobal(this::finalizeResult);
                return;
            }

            prismScheduler.runGlobal(() -> {
                if (cancelled) {
                    return;
                }
                modificationsQueue.clear();
                modificationsQueue.addAll(batch);
                executeCurrentBatch();
            });
        });
    }

    /**
     * Hand the current in-memory batch to the modification executor. When the
     * executor finishes the batch, kick off another stream fetch — or finalize
     * if the stream is drained.
     */
    private void executeCurrentBatch() {
        String batchMsg = "Modification batch beginning application. Batch size: {0}";
        loggingService.debug(batchMsg, modificationsQueue.size());

        Location schedulerLocation = schedulerLocation();

        // Pre-process (drain lava, remove drops/blocks) fires only on the first batch.
        // After that, blocks are already cleared and re-running would either re-clear
        // newly-placed rollback blocks or inflate counts.
        boolean shouldPreProcess =
            !preProcessRan &&
            mode.equals(ModificationQueueMode.COMPLETING) &&
            query.worldUuid() != null &&
            query.minCoordinate() != null &&
            query.maxCoordinate() != null;

        BiConsumer<World, BoundingBox> preProcessor = shouldPreProcess
            ? (world, boundingBox) -> {
                preProcess(world, boundingBox);
                preProcessRan = true;
            }
            : null;

        // Post-process (move entities) runs at the end of every batch. The
        // movedEntities count can be slightly inflated for multi-batch runs
        // since the same entities may fall within the cumulative bounding box
        // on successive passes; the operation itself is idempotent.
        BiConsumer<World, BoundingBox> postProcessor = mode.equals(ModificationQueueMode.COMPLETING)
            ? this::postProcess
            : null;

        modificationExecutor.execute(
            modificationsQueue,
            mode,
            modificationRuleset,
            schedulerLocation,
            this::applyModification,
            result -> {
                trackBoundingBox(result);
                countProcessed++;

                if (result.status().equals(ModificationResultStatus.PLANNED)) {
                    countPlanned++;
                    // Don't pin the result — keep just the lightweight undo
                    // snapshot, which cancelPreview replays to revert client
                    // packets without holding Activity refs.
                    if (result.undoEntry() != null) {
                        undoEntries.add(result.undoEntry());
                    }
                } else if (result.status().equals(ModificationResultStatus.APPLIED)) {
                    countApplied++;
                    // Same shape as PLANNED: snapshot is all the queue and
                    // any future /pr undo need; the Activity ref would just
                    // pin memory per block.
                    if (result.undoEntry() != null) {
                        undoEntries.add(result.undoEntry());
                    }
                    if (mode.equals(ModificationQueueMode.COMPLETING)) {
                        pendingReversalKeys.add((long) result.activity().primaryKey());
                    }
                } else if (result.status().equals(ModificationResultStatus.PARTIAL)) {
                    countPartial++;
                    results.add(result);
                } else {
                    countSkipped++;
                    results.add(result);
                }

                maybeReportProgress();
            },
            preProcessor,
            postProcessor,
            () -> {
                flushPendingReversalKeys();
                fetchAndRunNextBatch();
            }
        );
    }

    /**
     * Snapshot and clear the keys accumulated during the just-completed batch,
     * then mark them reversed off the region thread. Done per-batch so the IN
     * list never grows large enough to exceed driver/statement limits during
     * rollbacks of very large queries.
     */
    private void flushPendingReversalKeys() {
        if (pendingReversalKeys.isEmpty()) {
            return;
        }

        List<Long> snapshot = new ArrayList<>(pendingReversalKeys);
        pendingReversalKeys.clear();
        boolean reversed = markReversedState();

        prismScheduler.runAsync(() -> {
            try {
                storageAdapter.markReversed(snapshot, reversed);
            } catch (Exception e) {
                loggingService.handleException(e);
                notifyReversalFailureOnce();
            }
        });
    }

    /**
     * Notify the owning player/sender of a reversal-marking failure, at most
     * once per operation.
     */
    private void notifyReversalFailureOnce() {
        synchronized (this) {
            if (reversalErrorReported) {
                return;
            }
            reversalErrorReported = true;
        }

        if (owner() instanceof Player player) {
            prismScheduler.runForEntity(player, () -> messageService.errorQueueReversedFailure(player));
        } else if (owner() instanceof CommandSender sender) {
            prismScheduler.runGlobal(() -> messageService.errorQueueReversedFailure(sender));
        }
    }

    /**
     * Stream exhausted (or errored). Build the final result and notify the
     * registered end callback.
     */
    private void finalizeResult() {
        if (cancelled) {
            return;
        }

        ModificationQueueResult result = ModificationQueueResult.builder()
            .queue(this)
            .mode(mode)
            .results(results)
            .undoEntries(undoEntries)
            .applied(countApplied)
            .partial(countPartial)
            .planned(countPlanned)
            .skipped(countSkipped)
            .drainedLava(countDrainedLava)
            .removedBlocks(countRemovedBlocks)
            .removedDrops(countRemovedDrops)
            .movedEntities(countMovedEntities)
            .build();

        if (configurationService.prismConfig().modifications().debugMemory()) {
            loggingService.info(
                "Dispatching /spark heapdump at queue completion (mode={0}, results={1})",
                mode,
                results.size()
            );
            prismScheduler.runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spark heapdump"));
        }

        loggingService.debug("Modification queue fully processed, finishing up.");
        onEnd(result);
    }

    /**
     * Set the queue mode and start streaming for a preview run.
     */
    protected void startPreview() {
        resetState(ModificationQueueMode.PLANNING);
        announceStart();
        fetchAndRunNextBatch();
    }

    /**
     * Get a representative location for scheduling. Uses the first activity's
     * coordinate or falls back to null (for global scheduling).
     *
     * @return A location, or null
     */
    protected Location schedulerLocation() {
        if (!modificationsQueue.isEmpty()) {
            Activity first = modificationsQueue.getFirst();
            if (first.worldUuid() != null && first.coordinate() != null) {
                World world = Bukkit.getWorld(first.worldUuid());
                if (world != null) {
                    return new Location(world, first.coordinate().x(), first.coordinate().y(), first.coordinate().z());
                }
            }
        }

        return null;
    }

    @Override
    public void destroy() {
        cancelled = true;
        modificationExecutor.cancel();
        try {
            activityStream.close();
        } catch (Exception e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Notify the owner of progress when a new progress-report milestone is reached.
     * Called from the executor's result callback (once per processed item) so the
     * percent check is the hot path — kept to integer math and a single field read.
     * Thresholds are read from {@link ModificationConfiguration}.
     */
    private void maybeReportProgress() {
        var modConfig = configurationService.prismConfig().modifications();
        if (progressTotal < modConfig.progressReportThreshold()) {
            return;
        }

        int processed = countProcessed;
        int percent = (int) (((long) processed * 100L) / progressTotal);

        if (percent >= progressLastReportedPercent + modConfig.progressReportStepPercent() && percent < 100) {
            progressLastReportedPercent = percent;
            sendToOwner(receiver -> messageService.modificationsProgress(receiver, percent, processed, progressTotal));
        }
    }

    /**
     * Dispatch a message to the queue's owner on the appropriate scheduler thread.
     * Folia routes per-entity messages to the player's region thread; for non-player
     * senders we fall through to the global scheduler. No-op if the owner is neither.
     *
     * @param send The send action, given the resolved receiver
     */
    private void sendToOwner(Consumer<CommandSender> send) {
        if (owner() instanceof Player player) {
            prismScheduler.runForEntity(player, () -> send.accept(player));
        } else if (owner() instanceof CommandSender sender) {
            prismScheduler.runGlobal(() -> send.accept(sender));
        }
    }

    /**
     * Whether activities should be marked as reversed (true) or unreversed (false).
     *
     * @return True for rollback, false for restore
     */
    protected abstract boolean markReversedState();

    /**
     * Called when the modification queue has ended.
     *
     * @param result The result
     */
    protected void onEnd(ModificationQueueResult result) {
        // Reversal flags are written per-batch via flushPendingReversalKeys to
        // keep the IN list short. By the time we get here, every applied
        // activity has already been scheduled for marking.

        // Execute the callback, letting the caller know we've ended
        onEndCallback.accept(result);
    }
}
