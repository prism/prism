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

package org.prism_mc.prism.loader.services.schema;

import java.nio.file.Path;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

/**
 * Interface for running schema updates from the CLI without a Minecraft server.
 */
public interface SchemaUpdateCli {
    /**
     * Run schema updates against the configured database.
     *
     * @param configService The configuration service
     * @param loggingService The logging service
     * @param dataPath The plugin data path
     * @throws Exception If a schema update error occurs
     */
    void run(ConfigurationService configService, LoggingService loggingService, Path dataPath) throws Exception;
}
