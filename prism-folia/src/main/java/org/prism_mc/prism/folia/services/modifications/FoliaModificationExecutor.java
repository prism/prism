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

package org.prism_mc.prism.folia.services.modifications;

import com.google.inject.Inject;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.modifications.ModificationExecutor;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

/**
 * Folia implementation of {@link ModificationExecutor}. Groups modifications
 * by chunk region and schedules each group on its owning region thread. Each
 * region processes its batch independently, and a completion barrier aggregates
 * results when all regions finish.
 *
 * <p>Pre-processing (drain lava, remove drops/blocks) is executed per-region
 * with bounding boxes clipped to each region's boundaries, ensuring all world
 * operations run on the correct region thread.</p>
 *
 * <p>This executor is not a singleton — a new instance is created per queue
 * to avoid shared mutable state between concurrent operations.</p>
 */
public class FoliaModificationExecutor implements ModificationExecutor {

    /**
     * Folia regions are 32x32 chunks = 512x512 blocks. The region coordinate
     * is derived by shifting block coordinates right by 9 bits (2^9 = 512).
     * This value is hardcoded in Folia and not exposed via API.
     */
    private static final int REGION_BLOCK_SHIFT = 9;

    /**
     * Region size in blocks (512).
     */
    private static final int REGION_BLOCK_SIZE = 1 << REGION_BLOCK_SHIFT;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

    /**
     * Active scheduled tasks for cancellation. Uses CopyOnWriteArrayList
     * since tasks may self-remove from different region threads.
     */
    private final CopyOnWriteArrayList<ScheduledTask> activeTasks = new CopyOnWriteArrayList<>();

    /**
     * Dedicated lock for the onResult callback, shared across region threads.
     */
    private final Object resultLock = new Object();

    /**
     * Construct a new Folia modification executor.
     *
     * @param loggingService The logging service
     * @param prismScheduler The scheduler
     */
    @Inject
    public FoliaModificationExecutor(LoggingService loggingService, PrismScheduler prismScheduler) {
        this.loggingService = loggingService;
        this.prismScheduler = prismScheduler;
    }

    @Override
    public void execute(
        List<Activity> queue,
        ModificationQueueMode mode,
        ModificationRuleset ruleset,
        Location schedulerLocation,
        Function<Activity, ModificationResult> applyFn,
        Consumer<ModificationResult> onResult,
        BiConsumer<World, BoundingBox> preProcessor,
        BiConsumer<World, BoundingBox> postProcessor,
        Runnable onComplete
    ) {
        activeTasks.clear();

        // Snapshot the queue to avoid concurrent modification during grouping
        List<Activity> snapshot = new ArrayList<>(queue);

        // Group activities by region key (world + chunk region coordinates)
        Map<RegionKey, List<Activity>> regionBatches = groupByRegion(snapshot);

        if (regionBatches.isEmpty()) {
            onComplete.run();

            return;
        }

        loggingService.debug(
            "Folia executor: {0} activities across {1} regions",
            snapshot.size(),
            regionBatches.size()
        );

        // Track how many regions still need to finish
        AtomicInteger remainingRegions = new AtomicInteger(regionBatches.size());

        // Track remaining regions for post-processing
        int totalRegions = regionBatches.size();
        AtomicInteger postProcessRemaining = new AtomicInteger(totalRegions);
        List<RegionTarget> regionTargets = new ArrayList<>();

        // Schedule each region's batch on its owning region thread
        for (Map.Entry<RegionKey, List<Activity>> entry : regionBatches.entrySet()) {
            RegionKey regionKey = entry.getKey();
            List<Activity> regionQueue = entry.getValue();

            World world = Bukkit.getWorld(regionKey.worldUuid);
            if (world == null) {
                // Skip activities in unloaded worlds
                postProcessRemaining.decrementAndGet();
                if (remainingRegions.decrementAndGet() == 0) {
                    // Regions scheduled ahead of this one have already finished,
                    // so their post-processing still owes a run before completion.
                    runPostProcessing(postProcessor, postProcessRemaining, regionTargets, onComplete);
                }

                continue;
            }

            // Use the first activity's coordinate as the scheduling location
            Activity first = regionQueue.getFirst();
            Location regionLocation = new Location(
                world,
                first.coordinate().x(),
                first.coordinate().y(),
                first.coordinate().z()
            );

            // Clip the overall bounding box to this region's boundaries
            BoundingBox regionBounds = regionBoundingBox(world, regionKey.regionX, regionKey.regionZ);
            regionTargets.add(new RegionTarget(regionLocation, world, regionBounds));
            boolean[] regionPreProcessed = { false };
            // Persisted read pointer for PLANNING mode (see processRegionBatch).
            int[] regionReadIndex = { 0 };

            ScheduledTask task = prismScheduler.runAtLocationFixedRate(
                regionLocation,
                scheduledTask ->
                    processRegionBatch(
                        scheduledTask,
                        regionQueue,
                        mode,
                        ruleset,
                        world,
                        regionBounds,
                        preProcessor,
                        regionPreProcessed,
                        regionReadIndex,
                        applyFn,
                        onResult,
                        remainingRegions,
                        postProcessor,
                        postProcessRemaining,
                        regionTargets,
                        onComplete
                    ),
                1,
                ruleset.taskDelay()
            );

            activeTasks.add(task);
        }
    }

    @Override
    public void cancel() {
        for (ScheduledTask task : activeTasks) {
            task.cancel();
        }

        activeTasks.clear();
    }

    /**
     * Process a batch of modifications for a single region.
     */
    private void processRegionBatch(
        ScheduledTask scheduledTask,
        List<Activity> regionQueue,
        ModificationQueueMode mode,
        ModificationRuleset ruleset,
        World world,
        BoundingBox regionBounds,
        BiConsumer<World, BoundingBox> preProcessor,
        boolean[] regionPreProcessed,
        int[] regionReadIndex,
        Function<Activity, ModificationResult> applyFn,
        Consumer<ModificationResult> onResult,
        AtomicInteger remainingRegions,
        BiConsumer<World, BoundingBox> postProcessor,
        AtomicInteger postProcessRemaining,
        List<RegionTarget> regionTargets,
        Runnable onComplete
    ) {
        // Run pre-processing once on the first tick for this region
        if (!regionPreProcessed[0] && preProcessor != null) {
            // Flag first, so a pre-processing failure doesn't repeat every tick
            // and doesn't stop the batch itself from running and completing.
            regionPreProcessed[0] = true;

            try {
                preProcessor.accept(world, regionBounds);
            } catch (Throwable t) {
                loggingService.handleThrowable("A pre-modification error occurred.", t);
            }
        }

        int iterationCount = 0;
        int index = regionReadIndex[0];

        while (index < regionQueue.size() && iterationCount < ruleset.maxPerTask()) {
            Activity activity = regionQueue.get(index);
            iterationCount++;

            ModificationResult result = ModificationResult.builder().activity(activity).build();

            if (activity.action().type().reversible()) {
                try {
                    result = applyFn.apply(activity);
                } catch (Throwable t) {
                    result = ModificationResult.builder().activity(activity).errored().build();

                    loggingService.handleThrowable(String.format("A modification error occurred. %s", activity), t);
                }
            }

            // Thread-safe result tracking via dedicated lock. A failure here must
            // not skip the removal/advance below, or this activity is re-applied
            // on every subsequent tick.
            synchronized (resultLock) {
                try {
                    onResult.accept(result);
                } catch (Throwable t) {
                    loggingService.handleThrowable(
                        String.format("A modification result error occurred. %s", activity),
                        t
                    );
                }
            }

            // Remove from this region's queue if completing, advance index if planning
            if (mode.equals(ModificationQueueMode.COMPLETING)) {
                regionQueue.remove(index);
            } else {
                index++;
            }
        }

        // Persist the read pointer so the next tick resumes where this one left
        // off. In PLANNING mode items are not removed, so without this the index
        // would reset to 0 each tick and a region with more than maxPerTask
        // activities would reprocess the same prefix forever and never complete.
        regionReadIndex[0] = index;

        // If this region's batch is done, cancel its task and check completion
        boolean regionDone =
            regionQueue.isEmpty() || (mode.equals(ModificationQueueMode.PLANNING) && index >= regionQueue.size());
        if (regionDone) {
            scheduledTask.cancel();

            if (remainingRegions.decrementAndGet() == 0) {
                loggingService.debug("Folia executor: all regions completed, running post-processing.");

                // Run post-processing per-region on each region's thread
                runPostProcessing(postProcessor, postProcessRemaining, regionTargets, onComplete);
            }
        }
    }

    /**
     * Run post-processing (entity moves) on each region's thread with clipped bounding boxes.
     */
    private void runPostProcessing(
        BiConsumer<World, BoundingBox> postProcessor,
        AtomicInteger postProcessRemaining,
        List<RegionTarget> regionTargets,
        Runnable onComplete
    ) {
        if (postProcessor == null || regionTargets.isEmpty()) {
            onComplete.run();

            return;
        }

        for (RegionTarget target : regionTargets) {
            try {
                prismScheduler.runAtLocation(target.location(), () -> {
                    try {
                        postProcessor.accept(target.world(), target.bounds());
                    } catch (Throwable t) {
                        loggingService.handleThrowable("A post-modification error occurred.", t);
                    } finally {
                        finishRegionPostProcessing(postProcessRemaining, onComplete);
                    }
                });
            } catch (Throwable t) {
                loggingService.handleThrowable("Failed to schedule post-modification processing.", t);

                finishRegionPostProcessing(postProcessRemaining, onComplete);
            }
        }
    }

    /**
     * Record one region as post-processed.
     *
     * @param postProcessRemaining The count of regions still to post-process
     * @param onComplete The completion callback
     */
    private void finishRegionPostProcessing(AtomicInteger postProcessRemaining, Runnable onComplete) {
        if (postProcessRemaining.decrementAndGet() == 0) {
            onComplete.run();
        }
    }

    /**
     * Compute the bounding box for a region in block coordinates.
     *
     * @param world The world the region belongs to
     * @param regionX The region x coordinate
     * @param regionZ The region z coordinate
     * @return A bounding box covering the entire region
     */
    private BoundingBox regionBoundingBox(World world, int regionX, int regionZ) {
        int minX = regionX * REGION_BLOCK_SIZE;
        int minZ = regionZ * REGION_BLOCK_SIZE;
        return new BoundingBox(
            minX,
            world.getMinHeight(),
            minZ,
            minX + REGION_BLOCK_SIZE,
            world.getMaxHeight(),
            minZ + REGION_BLOCK_SIZE
        );
    }

    /**
     * Group activities by their Folia region key.
     *
     * @param queue The activity list (snapshot, safe to iterate)
     * @return A map of region key to activities in that region
     */
    private Map<RegionKey, List<Activity>> groupByRegion(List<Activity> queue) {
        Map<RegionKey, List<Activity>> groups = new LinkedHashMap<>();

        for (Activity activity : queue) {
            if (activity.worldUuid() == null || activity.coordinate() == null) {
                continue;
            }

            int regionX = (int) Math.floor(activity.coordinate().x()) >> REGION_BLOCK_SHIFT;
            int regionZ = (int) Math.floor(activity.coordinate().z()) >> REGION_BLOCK_SHIFT;

            RegionKey key = new RegionKey(activity.worldUuid(), regionX, regionZ);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(activity);
        }

        return groups;
    }

    /**
     * A key identifying a Folia region by world and region coordinates.
     */
    private record RegionKey(UUID worldUuid, int regionX, int regionZ) {}

    /**
     * A region's scheduling location, world and clipped bounds resolved when its batch was scheduled.
     *
     * <p>The world is held deliberately, not just for convenience: {@link Location}
     * refers to its world through a {@link java.lang.ref.Reference}, so keeping the
     * world strongly reachable for the life of the operation is what stops that
     * reference from being cleared and {@code getWorld()} from throwing part-way
     * through a long modification.</p>
     */
    private record RegionTarget(Location location, World world, BoundingBox bounds) {}
}
