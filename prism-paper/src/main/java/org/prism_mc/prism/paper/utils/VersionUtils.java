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

package org.prism_mc.prism.paper.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public class VersionUtils {

    /**
     * The server major version.
     */
    private static byte serverMajorVersion;

    /**
     * The server minor version.
     */
    private static byte serverMinorVersion;

    /**
     * The server patch version.
     */
    private static byte serverPatchVersion;

    static {
        var segments = Bukkit.getServer().getBukkitVersion().split("-");
        String[] split = segments[0].split("\\.");
        serverMajorVersion = Byte.parseByte(split[0]);
        serverMinorVersion = split.length > 1 ? Byte.parseByte(split[1]) : 0;
        serverPatchVersion = split.length > 2 ? Byte.parseByte(split[2]) : 0;
    }

    /**
     * Check if the server is at least the given major, minor, and patch version.
     *
     * @param major Major version number
     * @param minor Minor version number
     * @param patch Patch version number
     * @return True if the server is at least the version
     */
    public static boolean atLeast(int major, int minor, int patch) {
        return (serverMajorVersion >= major && serverMinorVersion >= minor && serverPatchVersion >= patch);
    }

    /**
     * Get the version as we detected it.
     *
     * @return The version
     */
    public static String detectedVersion() {
        return String.format("%d.%d.%d", serverMajorVersion, serverMinorVersion, serverPatchVersion);
    }
}
