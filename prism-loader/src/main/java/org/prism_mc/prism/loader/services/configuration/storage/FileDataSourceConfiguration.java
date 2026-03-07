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

package org.prism_mc.prism.loader.services.configuration.storage;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class FileDataSourceConfiguration extends DataSourceConfiguration {

    @Comment(
        """
        The path for the database file, excluding the filename.
        By default the file will be inside the prism plugin folder.
        You must use forward-slashes (c:/example) or double backslashes (c:\\temp)."""
    )
    private String path = ".";

    /**
     * File-based databases don't use a named schema.
     *
     * @return An empty schema name
     */
    @Override
    public String schema() {
        return "";
    }
}
