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
import org.prism_mc.prism.loader.storage.StorageType;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class StorageConfiguration {

    @Comment(
        """
        When enabled, worlds are identified by name instead of UUID.
        This is useful for servers with software that frequently regenerates worlds
        where the world name stays the same but the UUID changes.
        Disabled by default. Only enable if you have a specific need for this."""
    )
    private boolean identifyWorldsByName = false;

    @Comment(
        """
        Enable query spy. This logs queries and helpful debug information.
        Used primarily for development and debugging. Use carefully."""
    )
    private boolean spy = false;

    @Comment(
        """
        Set which storage system to use.
        Available options: H2, MARIADB, MYSQL, POSTGRES, SQLITE
        NOTE: Only one storage system may be used at a time.
        Transferring data from one to another is not yet supported."""
    )
    private StorageType primaryStorageType = StorageType.SQLITE;

    @Comment(
        """
        Settings for H2 file-based databases. File-based databases aren't generally
        recommended but the pros/cons depend entirely on your usage and needs."""
    )
    private FileDataSourceConfiguration h2 = new FileDataSourceConfiguration();

    @Comment("Settings for MariaDB")
    private MysqlDataSourceConfiguration mariadb = new MysqlDataSourceConfiguration();

    @Comment("Settings for MySQL")
    private MysqlDataSourceConfiguration mysql = new MysqlDataSourceConfiguration();

    @Comment("Settings for Postgres")
    private PostgresDataSourceConfiguration postgres = new PostgresDataSourceConfiguration();

    @Comment(
        """
        Settings for sqlite file-based databases. File-based databases aren't generally
        recommended but the pros/cons depend entirely on your usage and needs."""
    )
    private SqliteDataSourceConfiguration sqlite = new SqliteDataSourceConfiguration();

    /**
     * Get the primary data source.
     *
     * @return The primary data source
     */
    public DataSourceConfiguration primaryDataSource() {
        return switch (primaryStorageType) {
            case H2 -> h2;
            case MARIADB -> mariadb;
            case MYSQL -> mysql;
            case POSTGRES -> postgres;
            case SQLITE -> sqlite;
        };
    }
}
