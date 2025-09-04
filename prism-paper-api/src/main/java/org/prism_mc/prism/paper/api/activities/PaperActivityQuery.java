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

package org.prism_mc.prism.paper.api.activities;

import lombok.experimental.SuperBuilder;
import org.bukkit.Location;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.util.Coordinate;

@SuperBuilder(toBuilder = true)
public class PaperActivityQuery extends ActivityQuery {

    public abstract static class PaperActivityQueryBuilder<
        C extends PaperActivityQuery, B extends PaperActivityQueryBuilder<C, B>
    >
        extends ActivityQueryBuilder<C, B> {

        /**
         * Set the world uuid and coordinate from a Location.
         *
         * @param location Location
         * @return The builder
         */
        public B location(Location location) {
            this.worldUuid(location.getWorld().getUID());
            this.coordinate(location.getX(), location.getY(), location.getZ());
            return self();
        }

        /**
         * Set the reference coordinate from a Location.
         *
         * @param location The location
         * @return The builder
         */
        public B referenceLocation(Location location) {
            this.worldUuid(location.getWorld().getUID());
            this.referenceCoordinate(new Coordinate(location.getX(), location.getY(), location.getZ()));
            return self();
        }
    }
}
