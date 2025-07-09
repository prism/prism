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

import java.util.Locale;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.Cause;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.bukkit.api.containers.BukkitBlockContainer;
import org.prism_mc.prism.bukkit.api.containers.BukkitEntityContainer;
import org.prism_mc.prism.bukkit.api.containers.BukkitPlayerContainer;

@Getter
@SuperBuilder
public class BukkitActivity extends Activity {

    /**
     * Converts an enum name to a human-readable string.
     *
     * @param name The enum name
     * @return The string
     */
    public static String enumNameToString(String name) {
        return enumNameToString(name, Locale.ENGLISH);
    }

    /**
     * Converts an enum name to a human-readable string.
     *
     * @param name The enum name
     * @param locale The locale
     * @return The string
     */
    public static String enumNameToString(String name, Locale locale) {
        return name.toLowerCase(locale).replace('_', ' ');
    }

    /**
     * Converts a causing object to a block, entity, player, or string cause.
     *
     * @param cause The cause
     * @return The originalCause name
     */
    public static Cause toCause(Object cause) {
        if (cause instanceof Cause existingCause) {
            return existingCause;
        } else if (cause instanceof Player player) {
            return new Cause(new BukkitPlayerContainer(player));
        } else if (cause instanceof Entity causeEntity) {
            if (causeEntity.getType().equals(EntityType.FALLING_BLOCK)) {
                return new Cause(new StringContainer("gravity"));
            }

            return new Cause(new BukkitEntityContainer(causeEntity.getType()));
        } else if (cause instanceof EntityType causeEntityType) {
            return new Cause(new BukkitEntityContainer(causeEntityType));
        } else if (cause instanceof Block block) {
            return new Cause(new BukkitBlockContainer(block.getState()));
        } else if (cause instanceof BlockState causeBlockState) {
            return new Cause(new BukkitBlockContainer(causeBlockState));
        } else if (cause instanceof BlockIgniteEvent.IgniteCause igniteCause) {
            return new Cause(new StringContainer(enumNameToString(igniteCause.name())));
        } else if (cause instanceof EntityDamageEvent.DamageCause damageCause) {
            return new Cause(new StringContainer(enumNameToString(damageCause.name())));
        } else if (cause instanceof EntityUnleashEvent.UnleashReason unleashReason) {
            return new Cause(new StringContainer(enumNameToString(unleashReason.name())));
        } else if (cause instanceof TNTPrimeEvent.PrimeCause primeCause) {
            return new Cause(new StringContainer(enumNameToString(primeCause.name())));
        } else if (cause instanceof String causeStr) {
            return new Cause(new StringContainer(causeStr));
        }

        return new Cause(new StringContainer("unknown"));
    }

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
         * Set the cause.
         *
         * @param cause Cause
         * @return The builder
         */
        public B cause(Object cause) {
            super.cause(toCause(cause));
            return self();
        }

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
    }
}
