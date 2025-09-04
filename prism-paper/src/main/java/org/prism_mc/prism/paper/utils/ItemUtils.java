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

import static org.prism_mc.prism.paper.api.activities.PaperActivity.enumNameToString;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class ItemUtils {

    /**
     * Try our best to count the number of items that will be collected on double click.
     *
     * <p>I honestly can't determine how Minecraft chooses which stacks to take from, as the pattern
     * varies based on where in the inv your item is. Sometimes it's stacks closest, sometimes not.
     * Sometimes it's stacks to the left, sometimes right. It's client-side anyway so this is just a guess.</p>
     *
     * @param bottom The bottom inventory (player)
     * @param top The top inventory (container)
     * @param material The material
     * @param startQuantity The start quantity
     * @return The amount
     */
    public static int countCollectedToCursor(Inventory bottom, Inventory top, Material material, int startQuantity) {
        int remaining = material.getMaxStackSize() - startQuantity;

        // The bottom inv is always checked first. If we found enough, nothing will be tracked.
        var bottomAmount = countMatchingMaterials(bottom, material);
        if (bottomAmount > remaining) {
            return 0;
        } else {
            remaining -= bottomAmount;
        }

        // Return how many the top inv contributed
        var topAmount = countMatchingMaterials(top, material);
        return Math.min(topAmount, remaining);
    }

    /**
     * Count of matching items by material in an inventory.
     *
     * @param inventory The inventory
     * @param material The material
     * @return Amount
     */
    public static int countMatchingMaterials(Inventory inventory, Material material) {
        int total = 0;

        for (var itemStack : inventory.getStorageContents()) {
            if (itemStack != null && itemStack.getType().equals(material)) {
                total += itemStack.getAmount();
            }
        }

        return total;
    }

    /**
     * Get the amount of a material an inventory has room for.
     *
     * @param inventory The inventory
     * @param material The material
     * @param max The max acceptable quantity to return
     * @return Amount
     */
    public static int inventoryAcceptsQuantity(Inventory inventory, Material material, Integer max) {
        var acceptableQuantity = 0;

        for (var item : inventory.getStorageContents()) {
            if (nullOrAir(item)) {
                acceptableQuantity += material.getMaxStackSize();
            } else if (item.getType().equals(material)) {
                acceptableQuantity += material.getMaxStackSize() - item.getAmount();
            }

            if (max != null && acceptableQuantity >= max) {
                acceptableQuantity = max;

                break;
            }
        }

        return acceptableQuantity;
    }

    /**
     * Checks for valid items. Plugins have a way of giving us bad data.
     *
     * @param item The item
     * @return True if null, air, or quantity <= 0
     */
    public static boolean isValidItem(ItemStack item) {
        return !nullOrAir(item) && item.getAmount() > 0;
    }

    /**
     * Get the material "nice" name for an item stack.
     *
     * @param itemStack The item stack
     * @return The material name
     */
    public static String materialName(ItemStack itemStack) {
        return enumNameToString(itemStack.getType().name());
    }

    /**
     * Checks for null or air items.
     *
     * @param item The item
     * @return True if null or air
     */
    public static boolean nullOrAir(ItemStack item) {
        return item == null || item.getType().equals(Material.AIR);
    }
}
