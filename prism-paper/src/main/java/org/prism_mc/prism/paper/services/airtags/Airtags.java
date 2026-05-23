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

package org.prism_mc.prism.paper.services.airtags;

import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

@UtilityClass
public class Airtags {

    /**
     * The PDC key under which an airtag ID is stored on an item.
     */
    public static final NamespacedKey KEY = new NamespacedKey("prism", "airtag");

    /**
     * Whether the item carries a Prism airtag.
     *
     * @param itemStack The item stack (may be null)
     * @return True if airtagged
     */
    public static boolean has(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.STRING);
    }

    /**
     * Read the airtag ID stored on an item, or null if absent.
     *
     * @param itemStack The item stack (may be null)
     * @return The airtag ID, or null
     */
    public static String get(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }
}
