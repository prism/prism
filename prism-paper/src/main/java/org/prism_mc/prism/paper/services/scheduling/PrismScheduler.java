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

package org.prism_mc.prism.paper.services.scheduling;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Abstraction over Bukkit/Folia scheduling APIs. On Paper, all "sync" methods
 * delegate to the global region scheduler. On Folia, location-bound work uses
 * the region scheduler and entity-bound work uses the entity scheduler.
 */
public interface PrismScheduler {
    /**
     * Run a task on the thread that owns the given location's region.
     *
     * @param location The location
     * @param task The task
     */
    void runAtLocation(Location location, Runnable task);

    /**
     * Run a repeating task on the thread that owns the given location's region.
     *
     * @param location The location
     * @param task The task, receiving a handle that can be cancelled
     * @param initialDelayTicks Initial delay in ticks
     * @param periodTicks Period in ticks
     * @return A scheduled task handle
     */
    io.papermc.paper.threadedregions.scheduler.ScheduledTask runAtLocationFixedRate(
        Location location,
        Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask> task,
        long initialDelayTicks,
        long periodTicks
    );

    /**
     * Run a task on the thread appropriate for the given entity.
     *
     * @param entity The entity
     * @param task The task
     */
    void runForEntity(Entity entity, Runnable task);

    /**
     * Teleport an entity to a destination.
     *
     * @param entity The entity
     * @param destination The destination location
     */
    void teleport(Entity entity, Location destination);

    /**
     * Run a task on the global region thread. Use for non-location-bound work
     * such as dispatching console commands.
     *
     * @param task The task
     */
    void runGlobal(Runnable task);

    /**
     * Run a task asynchronously (off any game thread).
     *
     * @param task The task
     */
    void runAsync(Runnable task);

    /**
     * Run a task asynchronously after a delay.
     *
     * @param task The task
     * @param delay The delay amount
     * @param unit The time unit
     */
    void runAsyncDelayed(Runnable task, long delay, TimeUnit unit);
}
