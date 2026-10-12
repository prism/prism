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

import com.google.inject.Inject;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

/**
 * Paper implementation of {@link ModificationExecutor}. Processes all
 * modifications linearly in a single repeating task on the global region
 * thread. Pre/post processors are called once with a full-world bounding box
 * so the queue's own bounding box clipping produces the correct area.
 *
 * <p>Not a singleton — a new instance is created per queue to avoid
 * shared mutable state between operations.</p>
 */
public class PaperModificationExecutor implements ModificationExecutor {

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

    /**
     * The current scheduled task, if any.
     */
    private ScheduledTask scheduledTask;

    /**
     * Count of modifications read (used for preview pointer advancement).
     */
    private int countModificationsRead;

    /**
     * Whether pre-processing has run.
     */
    private boolean preProcessed;

    /**
     * Construct a new Paper modification executor.
     *
     * @param loggingService The logging service
     * @param prismScheduler The scheduler
     */
    @Inject
    public PaperModificationExecutor(LoggingService loggingService, PrismScheduler prismScheduler) {
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
        countModificationsRead = 0;
        preProcessed = false;

        scheduledTask = prismScheduler.runAtLocationFixedRate(
            schedulerLocation,
            task -> {
                // Run pre-processing once on the first tick
                if (!preProcessed && preProcessor != null && schedulerLocation != null) {
                    // Flag first, so a pre-processing failure doesn't repeat every
                    // tick and doesn't stop the batch itself from running.
                    preProcessed = true;

                    try {
                        World world = schedulerLocation.getWorld();
                        preProcessor.accept(world, fullWorldBoundingBox(world));
                    } catch (Throwable t) {
                        loggingService.handleThrowable("A pre-modification error occurred.", t);
                    }
                }

                loggingService.debug("New modification run beginning...");

                int iterationCount = 0;
                int index = countModificationsRead;

                // Limit the absolute max number of steps per execution of this task
                while (index < queue.size() && iterationCount < ruleset.maxPerTask()) {
                    final Activity activity = queue.get(index);
                    iterationCount++;

                    // Simulate queue pointer advancement for previews
                    if (mode.equals(ModificationQueueMode.PLANNING)) {
                        countModificationsRead++;
                    }

                    ModificationResult result = ModificationResult.builder().activity(activity).build();

                    // Delegate reversible modifications to the actions
                    if (activity.action().type().reversible()) {
                        try {
                            result = applyFn.apply(activity);
                        } catch (Throwable t) {
                            result = ModificationResult.builder().activity(activity).errored().build();

                            loggingService.handleThrowable(
                                String.format("A modification error occurred. %s", activity),
                                t
                            );
                        }
                    }

                    // A failure here must not skip the removal/advance below, or
                    // this activity is re-applied on every subsequent tick.
                    try {
                        onResult.accept(result);
                    } catch (Throwable t) {
                        loggingService.handleThrowable(
                            String.format("A modification result error occurred. %s", activity),
                            t
                        );
                    }

                    // Remove from the queue if we're not previewing
                    if (mode.equals(ModificationQueueMode.COMPLETING)) {
                        queue.remove(index);
                    } else {
                        index++;
                    }
                }

                // The task for this action is done being used
                if (queue.isEmpty() || countModificationsRead >= queue.size()) {
                    loggingService.debug("Modification queue fully processed, finishing up.");

                    // Cancel the repeating task
                    task.cancel();

                    try {
                        // Run post-processing
                        if (postProcessor != null && schedulerLocation != null) {
                            World world = schedulerLocation.getWorld();
                            postProcessor.accept(world, fullWorldBoundingBox(world));
                        }
                    } catch (Throwable t) {
                        loggingService.handleThrowable("A post-modification error occurred.", t);
                    } finally {
                        // The task is already cancelled; without this the queue
                        // would never fetch another batch or finalize.
                        onComplete.run();
                    }
                }
            },
            1,
            ruleset.taskDelay()
        );
    }

    /**
     * Build a bounding box covering a world's full extent and height range. Used
     * on Paper where there are no region boundaries, so intersection with the
     * modification bounding box produces that box unchanged.
     *
     * @param world The world
     * @return A bounding box covering the entire world
     */
    private BoundingBox fullWorldBoundingBox(World world) {
        return new BoundingBox(
            -30_000_000,
            world.getMinHeight(),
            -30_000_000,
            30_000_000,
            world.getMaxHeight(),
            30_000_000
        );
    }

    @Override
    public void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
    }
}
