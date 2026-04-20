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

package org.prism_mc.prism.folia;

import org.prism_mc.prism.loader.services.dependencies.loader.PluginLoader;
import org.prism_mc.prism.paper.PrismPaperBootstrap;

/**
 * Folia-specific bootstrap. Provides the Folia Guice module override class
 * name so that Folia-aware scheduler and executor implementations are used.
 */
public class PrismFoliaBootstrap extends PrismPaperBootstrap {

    /**
     * Constructor.
     *
     * @param pluginLoader The loader plugin
     */
    public PrismFoliaBootstrap(PluginLoader pluginLoader) {
        super(pluginLoader);
    }

    @Override
    protected String moduleOverrideClassName() {
        return "org.prism_mc.prism.folia.injection.FoliaModule";
    }
}
