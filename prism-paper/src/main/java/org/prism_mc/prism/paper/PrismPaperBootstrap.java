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

package org.prism_mc.prism.paper;

import org.prism_mc.prism.loader.services.dependencies.classpath.ClassPathAppender;
import org.prism_mc.prism.loader.services.dependencies.classpath.JarInJarClassPathAppender;
import org.prism_mc.prism.loader.services.dependencies.loader.PluginLoader;
import org.prism_mc.prism.loader.services.dependencies.loader.PrismBootstrap;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class PrismPaperBootstrap implements PrismBootstrap {

    /**
     * The loader plugin.
     */
    private final PluginLoader pluginLoader;

    /**
     * The plugin class path appender.
     */
    private final ClassPathAppender classPathAppender;

    /**
     * The prism plugin.
     */
    private final PrismPaper prism;

    /**
     * Constructor.
     *
     * @param pluginLoader The loader plugin
     */
    public PrismPaperBootstrap(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
        this.classPathAppender = new JarInJarClassPathAppender(getClass().getClassLoader());
        prism = new PrismPaper(this);
    }

    /**
     * Get the class path appender.
     *
     * @return The classpath appender
     */
    public ClassPathAppender classPathAppender() {
        return classPathAppender;
    }

    /**
     * The loader plugin.
     *
     * @return The loader plugin
     */
    public PluginLoader loader() {
        return pluginLoader;
    }

    /**
     * The logging service.
     *
     * @return The logging service
     */
    public LoggingService loggingService() {
        return pluginLoader.loggingService();
    }

    @Override
    public void onEnable() {
        this.prism.onEnable();
    }

    @Override
    public void onDisable() {
        this.prism.onDisable();
    }
}
