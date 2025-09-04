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

package org.prism_mc.prism.paper.providers;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.prism_mc.prism.loader.services.dependencies.loader.PluginLoader;

public class TaskChainProvider {

    /**
     * The plugin.
     */
    private final PluginLoader plugin;

    /**
     * The task chain factory.
     */
    @Getter
    private TaskChainFactory taskChainFactory;

    /**
     * Constructor.
     *
     * @param plugin The loader plugin
     */
    public TaskChainProvider(PluginLoader plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new task chain.
     *
     * @param <T> The type
     * @return The task chain
     */
    public <T> TaskChain<T> newChain() {
        if (taskChainFactory == null) {
            taskChainFactory = BukkitTaskChainFactory.create((Plugin) plugin);
        }

        return taskChainFactory.newChain();
    }
}
