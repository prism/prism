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

package org.prism_mc.prism.bukkit.services.query;

import org.bukkit.Location;
import org.bukkit.World;
import org.prism_mc.prism.api.util.Coordinate;

public class ParameterContext {

    /**
     * The reference location.
     */
    public Location referenceLocation;

    /**
     * The world.
     */
    public World world;

    /**
     * Constructor.
     *
     * @param referenceLocation The reference location
     */
    public ParameterContext(Location referenceLocation) {
        if (referenceLocation != null) {
            this.referenceLocation = referenceLocation;
            this.world = referenceLocation.getWorld();
        }
    }

    /**
     * Set the reference location.
     *
     * @param coordinate The coordinate
     */
    public void setReferenceLocation(Coordinate coordinate) {
        this.referenceLocation = new Location(world, coordinate.x(), coordinate.y(), coordinate.z());
    }

    /**
     * Set the world.
     *
     * @param newWorld The world
     */
    public void setWorld(World newWorld) {
        this.referenceLocation = new Location(
            newWorld,
            referenceLocation.x(),
            referenceLocation.y(),
            referenceLocation.z()
        );
        this.world = newWorld;
    }
}
