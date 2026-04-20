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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import lombok.Getter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.injection.PrismModule;

public class InjectorProvider {

    /**
     * The injector.
     */
    @Getter
    private Injector injector;

    /**
     * Construct the injector provider.
     *
     * @param prism The prism plugin
     * @param loggingService The logging service
     * @param moduleOverrideClassName Optional override module class name, or null
     */
    public InjectorProvider(PrismPaper prism, LoggingService loggingService, String moduleOverrideClassName) {
        Module module = new PrismModule(prism, loggingService);

        if (moduleOverrideClassName != null) {
            try {
                Module override = (Module) Class.forName(moduleOverrideClassName)
                    .getDeclaredConstructor()
                    .newInstance();
                module = Modules.override(module).with(override);
            } catch (Exception e) {
                loggingService.error("Failed to load module override: {0}", moduleOverrideClassName);
                loggingService.handleException(e);
            }
        }

        this.injector = Guice.createInjector(module);
    }
}
