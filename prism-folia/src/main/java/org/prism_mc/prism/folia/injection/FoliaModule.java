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

package org.prism_mc.prism.folia.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.prism_mc.prism.folia.services.modifications.FoliaModificationExecutor;
import org.prism_mc.prism.folia.services.scheduling.FoliaScheduler;
import org.prism_mc.prism.paper.services.modifications.ModificationExecutor;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

/**
 * Guice override module for Folia. Used with {@code Modules.override()} to
 * replace the Paper scheduler and modification executor bindings with
 * Folia-specific implementations.
 */
public class FoliaModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PrismScheduler.class).to(FoliaScheduler.class).in(Singleton.class);
        bind(ModificationExecutor.class).to(FoliaModificationExecutor.class);
    }
}
