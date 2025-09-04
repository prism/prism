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

package org.prism_mc.prism.paper.loader;

import dev.triumphteam.gui.TriumphGui;
import org.bukkit.plugin.java.JavaPlugin;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.dependencies.loader.JarInJarClassLoader;
import org.prism_mc.prism.loader.services.dependencies.loader.PluginLoader;
import org.prism_mc.prism.loader.services.dependencies.loader.PrismBootstrap;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class PrismPaperPluginLoader extends JavaPlugin implements PluginLoader {

    /**
     * The file name of the "JarInJar".
     */
    private static final String JAR_NAME = "prism-paper.jarinjar";

    /**
     * The qualified name of the bootstrapper.
     */
    private static final String BOOTSTRAP_CLASS = "org.prism_mc.prism.paper.PrismPaperBootstrap";

    /**
     * The configuration service.
     */
    private ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private LoggingService loggingService;

    /**
     * The prism bootstrapper.
     */
    private PrismBootstrap prismBootstrap;

    @Override
    public ConfigurationService configurationService() {
        return configurationService;
    }

    @Override
    public LoggingService loggingService() {
        return loggingService;
    }

    @Override
    public void onEnable() {
        // Instantiate the loader
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);

        // Instantiate the bootstrapper (which then instantiates the actual Prism plugin)
        this.prismBootstrap = loader.instantiatePlugin(BOOTSTRAP_CLASS, PluginLoader.class, this);

        // Initialize the config service and load config files
        configurationService = new ConfigurationService(getDataFolder().toPath(), this.getLogger());

        // Initialize the logger and logging service
        loggingService = new LoggingService(configurationService, this.getLogger());

        TriumphGui.init(this);

        // Call onEnable in the bootstrapper
        this.prismBootstrap.onEnable();
    }

    @Override
    public void onDisable() {
        this.prismBootstrap.onDisable();
    }
}
