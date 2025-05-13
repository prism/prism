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

package org.prism_mc.prism.bukkit.api.activities;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.Activity;

@Getter
@SuperBuilder
public class BukkitActivity extends Activity {

    /**
     * The bukkit player.
     */
    private Player bukkitPlayer;

    /**
     * Get the location.
     *
     * @return The location
     */
    public Location location() {
        var world = Bukkit.getServer().getWorld(worldUuid());
        return new Location(world, coordinate.x(), coordinate.y(), coordinate.z());
    }

    public abstract static class BukkitActivityBuilder<C extends BukkitActivity, B extends BukkitActivityBuilder<C, B>>
        extends ActivityBuilder<C, B> {

        /**
         * Set the world and coordinate from a Location.
         *
         * @param location Location
         * @return The builder
         */
        public B location(Location location) {
            this.world(location.getWorld().getUID(), location.getWorld().getName());
            this.coordinate(location.getX(), location.getY(), location.getZ());
            return self();
        }

        /**
         * Set the player uuid/name from a Player.
         *
         * @param player Player
         * @return The builder
         */
        public B player(Player player) {
            this.player(player.getUniqueId(), player.getName());
            this.bukkitPlayer(player);
            return self();
        }
    }
}
