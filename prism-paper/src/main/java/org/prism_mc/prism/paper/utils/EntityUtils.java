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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

@UtilityClass
public class EntityUtils {

    /**
     * Moves entities up to ground level.
     *
     * @param world The world
     * @param boundingBox The bounding box
     * @return A count of entities moved
     */
    public static int moveEntitiesToGround(World world, BoundingBox boundingBox) {
        var totalMoved = 0;

        for (var entity : entitiesInRangeByClass(world, boundingBox, LivingEntity.class)) {
            final Location location = entity.getLocation();

            Location destination = null;
            while (true) {
                if (location.getY() >= 256) {
                    break;
                }

                if (location.getBlock().isPassable() && location.getBlock().getRelative(BlockFace.UP).isPassable()) {
                    destination = location;

                    break;
                }

                location.setY(location.getY() + 1);
            }

            if (destination != null) {
                // Teleport to the destination
                entity.teleport(destination);
            } else {
                // Teleport to the highest block
                entity.teleport(
                    world
                        .getHighestBlockAt(entity.getLocation().getBlockX(), entity.getLocation().getBlockZ())
                        .getRelative(BlockFace.UP)
                        .getLocation()
                );
            }

            totalMoved++;
        }

        return totalMoved;
    }

    /**
     * Checks if an entity type is a hanging entity.
     *
     * @param entityType The entity type
     * @return True if entity type is hanging
     */
    private static boolean isHanging(EntityType entityType) {
        return (
            entityType.equals(EntityType.ITEM_FRAME) ||
            entityType.equals(EntityType.GLOW_ITEM_FRAME) ||
            entityType.equals(EntityType.PAINTING)
        );
    }

    /**
     * Remove drops (items + experience orbs) within a bounding box.
     *
     * @param world The world
     * @param boundingBox The bounding box
     * @return The count of drops removed
     */
    public static int removeDropsInRange(World world, BoundingBox boundingBox) {
        Collection<Entity> nearbyDrops = entitiesInRangeByClass(world, boundingBox, Item.class, ExperienceOrb.class);
        for (Entity e : nearbyDrops) {
            e.remove();
        }

        return nearbyDrops.size();
    }

    /**
     * Find all entities within a bounding box by their class.
     *
     * @param world The world
     * @param boundingBox The bounding box
     * @param entities The entities
     * @return A list of matched entities
     */
    public static List<Entity> entitiesInRangeByClass(World world, BoundingBox boundingBox, Class<?>... entities) {
        return world
            .getNearbyEntities(boundingBox)
            .stream()
            .filter(e -> Arrays.stream(entities).anyMatch(clazz -> clazz.isInstance(e)))
            .toList();
    }
}
