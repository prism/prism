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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Paper implementation of {@link PrismScheduler}. All sync methods delegate
 * to the global region scheduler since Paper has a single main thread.
 */
@Singleton
public class PaperScheduler implements PrismScheduler {

    /**
     * The plugin instance used for scheduler registration.
     */
    private final Plugin plugin;

    /**
     * Construct a new Paper scheduler.
     *
     * @param plugin The plugin
     */
    @Inject
    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAtLocation(Location location, Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    @Override
    public ScheduledTask runAtLocationFixedRate(
        Location location,
        Consumer<ScheduledTask> task,
        long initialDelayTicks,
        long periodTicks
    ) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }

    @Override
    public void runForEntity(Entity entity, Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    @Override
    public void teleport(Entity entity, Location destination) {
        entity.teleport(destination);
    }

    @Override
    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    @Override
    public void runAsyncDelayed(Runnable task, long delay, TimeUnit unit) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), delay, unit);
    }
}
