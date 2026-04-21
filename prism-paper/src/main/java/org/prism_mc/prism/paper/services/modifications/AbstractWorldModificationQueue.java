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
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationResultStatus;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;
import org.prism_mc.prism.paper.utils.BlockUtils;
import org.prism_mc.prism.paper.utils.EntityUtils;

public abstract class AbstractWorldModificationQueue implements ModificationQueue {

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
     * Manage a queue of pending modifications.
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
     * A list of all modification results.
     */
    protected final List<ModificationResult> results = new ArrayList<>();

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
     * @param modifications A list of all modifications
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
        final List<Activity> modifications,
        Consumer<ModificationQueueResult> onEndCallback
    ) {
        modificationsQueue.addAll(modifications);
        this.loggingService = loggingService;
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
        this.prismScheduler = prismScheduler;
        this.modificationExecutor = modificationExecutor;
        this.modificationRuleset = modificationRuleset;
        this.owner = owner;
        this.query = query;
        this.onEndCallback = onEndCallback;
    }

    @Override
    public int queueSize() {
        return modificationsQueue.size();
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
     * @param builder The result builder (thread-safe updates via synchronized)
     * @param world The world
     * @param regionBounds The region-safe bounding box to clip operations to
     */
    protected void preProcess(
        ModificationQueueResult.ModificationQueueResultBuilder builder,
        World world,
        BoundingBox regionBounds
    ) {
        BoundingBox effectiveBox = modificationBoundingBox().intersection(regionBounds);
        if (effectiveBox.getVolume() <= 0) {
            return;
        }

        synchronized (builder) {
            if (modificationRuleset.drainLava()) {
                int count = BlockUtils.removeBlocksByMaterial(world, effectiveBox, List.of(Material.LAVA)).size();
                builder.drainedLava(builder.build().drainedLava() + count);
            }

            if (modificationRuleset.removeDrops()) {
                int count = EntityUtils.removeDropsInRange(world, effectiveBox);
                builder.removedDrops(builder.build().removedDrops() + count);
            }

            if (!modificationRuleset.removeBlocks().isEmpty()) {
                List<Material> materials = modificationRuleset
                    .removeBlocks()
                    .stream()
                    .map(m -> Material.valueOf(m.toUpperCase()))
                    .toList();

                int count = BlockUtils.removeBlocksByMaterial(world, effectiveBox, materials).size();
                builder.removedBlocks(builder.build().removedBlocks() + count);
            }
        }
    }

    /**
     * Apply post-modification tasks within the given region bounds. The actual
     * area affected is the intersection of the modification bounding box and
     * the region bounds.
     *
     * @param builder The result builder (thread-safe updates via synchronized)
     * @param world The world
     * @param regionBounds The region-safe bounding box to clip operations to
     */
    protected void postProcess(
        ModificationQueueResult.ModificationQueueResultBuilder builder,
        World world,
        BoundingBox regionBounds
    ) {
        if (!modificationRuleset.moveEntities() || results.isEmpty()) {
            return;
        }

        BoundingBox effectiveBox = modificationBoundingBox().intersection(regionBounds);
        if (effectiveBox.getVolume() <= 0) {
            return;
        }

        int count = EntityUtils.moveEntitiesToGround(world, effectiveBox, prismScheduler);
        synchronized (builder) {
            builder.movedEntities(builder.build().movedEntities() + count);
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
        countModificationsRead = 0;
        countApplied = 0;
        countPartial = 0;
        countPlanned = 0;
        countSkipped = 0;
        results.clear();
        this.mode = ModificationQueueMode.COMPLETING;
        execute();
    }

    protected void execute() {
        String queueSizeMsg = "Modification queue beginning application. Queue size: {0}";
        loggingService.debug(queueSizeMsg, modificationsQueue.size());

        if (!modificationsQueue.isEmpty()) {
            Location schedulerLocation = schedulerLocation();

            ModificationQueueResult.ModificationQueueResultBuilder builder = ModificationQueueResult.builder()
                .queue(this);

            // Build pre/post processors that the executor will call on region-safe threads
            boolean shouldPreProcess =
                mode.equals(ModificationQueueMode.COMPLETING) &&
                query.worldUuid() != null &&
                query.minCoordinate() != null &&
                query.maxCoordinate() != null;

            modificationExecutor.execute(
                modificationsQueue,
                mode,
                modificationRuleset,
                schedulerLocation,
                this::applyModification,
                result -> {
                    results.add(result);
                    trackBoundingBox(result);

                    if (result.status().equals(ModificationResultStatus.PLANNED)) {
                        countPlanned++;
                    } else if (result.status().equals(ModificationResultStatus.APPLIED)) {
                        countApplied++;
                    } else if (result.status().equals(ModificationResultStatus.PARTIAL)) {
                        countPartial++;
                    } else {
                        countSkipped++;
                    }
                },
                shouldPreProcess ? (world, boundingBox) -> preProcess(builder, world, boundingBox) : null,
                mode.equals(ModificationQueueMode.COMPLETING)
                    ? (world, boundingBox) -> postProcess(builder, world, boundingBox)
                    : null,
                () -> {
                    ModificationQueueResult result = builder
                        .mode(mode)
                        .results(results)
                        .applied(countApplied)
                        .partial(countPartial)
                        .planned(countPlanned)
                        .skipped(countSkipped)
                        .build();

                    onEnd(result);
                }
            );
        }
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
        modificationExecutor.cancel();
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
        if (result.mode().equals(ModificationQueueMode.COMPLETING)) {
            // Get PKs of all applied activities
            List<Long> primarykeys = result
                .results()
                .stream()
                .filter(r -> r.status().equals(ModificationResultStatus.APPLIED))
                .map(r -> (long) ((Activity) r.activity()).primaryKey())
                .toList();

            // Run the database update off the main thread to avoid blocking ticks
            prismScheduler.runAsync(() -> {
                try {
                    storageAdapter.markReversed(primarykeys, markReversedState());
                } catch (Exception e) {
                    loggingService.handleException(e);

                    if (owner() instanceof Player player) {
                        prismScheduler.runForEntity(player, () -> {
                            messageService.errorQueueReversedFailure(player);
                        });
                    } else if (owner() instanceof CommandSender sender) {
                        prismScheduler.runGlobal(() -> {
                            messageService.errorQueueReversedFailure(sender);
                        });
                    }
                }
            });
        }

        // Execute the callback, letting the caller know we've ended
        onEndCallback.accept(result);
    }
}
