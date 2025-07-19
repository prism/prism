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
public class PostgresDataSourceConfiguration extends SqlDataSourceConfiguration {

    @Comment(
        """
        Set the schema for prism tables/functions to use.
        We recommend creating a "prism" schema for all prism entities.
        https://neon.tech/postgresql/postgresql-administration/postgresql-create-schema"""
    )
    public String schema = "prism";

    @Comment(
        """
        Enable stored procedures. Stored procedures allow Prism to modify database records
        more efficiently and with reduced network traffic.
        However, your postgres account must have privileges to `CREATE FUNCTION`.
        If you're unsure, Prism tells you in the server console during server startup.
        Prism will force disable this setting if you do not have necessary permission."""
    )
    private boolean useStoredProcedures = true;

    /**
     * Constructor.
     */
    public PostgresDataSourceConfiguration() {
        super("5432", "postgres");
    }

    /**
     * The catalog.
     *
     * @return The catalog
     */
    @Override
    public String catalog() {
        return database();
    }

    /**
     * Disable the setting for using stored procedures.
     */
    public void disallowStoredProcedures() {
        this.useStoredProcedures = false;
    }
}
