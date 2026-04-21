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

/**
 * Strategy for scheduling and executing world modifications. On Paper, all
 * modifications run on the global region thread. On Folia, modifications are
 * grouped by chunk region and scheduled on the owning region thread.
 */
public interface ModificationExecutor {
    /**
     * Schedule and execute modifications from the queue.
     *
     * <p>The {@code preProcessor} and {@code postProcessor} perform world operations
     * (block removal, entity queries) within a bounding box. On Paper, they are called
     * once with the full bounding box. On Folia, they are called per-region with
     * bounding boxes clipped to each region's boundaries, on that region's thread.</p>
     *
     * @param queue The modifications to process
     * @param mode The queue mode (COMPLETING or PLANNING)
     * @param ruleset The modification ruleset (controls batch size and delay)
     * @param schedulerLocation A representative location for scheduling
     * @param applyFn Function to apply a single modification
     * @param onResult Callback for each processed result
     * @param preProcessor Pre-processing callback (world, boundingBox) for region-safe operations
     * @param postProcessor Post-processing callback (world, boundingBox) for region-safe operations
     * @param onComplete Callback when all modifications are processed (after post-processing)
     */
    void execute(
        List<Activity> queue,
        ModificationQueueMode mode,
        ModificationRuleset ruleset,
        Location schedulerLocation,
        Function<Activity, ModificationResult> applyFn,
        Consumer<ModificationResult> onResult,
        BiConsumer<World, BoundingBox> preProcessor,
        BiConsumer<World, BoundingBox> postProcessor,
        Runnable onComplete
    );

    /**
     * Cancel any in-progress execution.
     */
    void cancel();
}
