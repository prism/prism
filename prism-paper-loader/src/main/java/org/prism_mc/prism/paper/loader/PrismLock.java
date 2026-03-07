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

import lombok.experimental.UtilityClass;

/**
 * Shared lock file constants for preventing concurrent access
 * between the Minecraft server plugin and the CLI.
 */
@UtilityClass
public final class PrismLock {

    /**
     * The name of the lock file used to detect a running server.
     */
    public static final String LOCK_FILE_NAME = "prism.lock";
}
