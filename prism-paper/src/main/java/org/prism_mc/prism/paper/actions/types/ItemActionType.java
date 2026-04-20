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

package org.prism_mc.prism.paper.actions.types;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ActionData;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;

public class ItemActionType extends ActionType {

    /**
     * Construct a new item action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public ItemActionType(String key, ActionResultType resultType, boolean reversible) {
        super(key, resultType, reversible);
    }

    /**
     * Construct a new item action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     * @param aggregatable Whether activities should be aggregated
     */
    public ItemActionType(String key, ActionResultType resultType, boolean reversible, boolean aggregatable) {
        super(key, resultType, reversible, true, null, aggregatable);
    }

    /**
     * Construct a new item action type with a default past tense string.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     * @param defaultPastTense The default past tense translation string
     */
    public ItemActionType(String key, ActionResultType resultType, boolean reversible, String defaultPastTense) {
        super(key, resultType, reversible, true, null, false, defaultPastTense);
    }

    @Override
    public Action createAction(ActionData actionData) {
        ItemStack itemStack;
        if (actionData.itemData() != null) {
            itemStack = NBT.itemStackFromNBT(NBT.parseNBT(actionData.itemData()));
        } else {
            Material material = Material.valueOf(actionData.material());
            itemStack = new ItemStack(material);
        }

        return new PaperItemStackAction(this, itemStack, actionData.itemQuantity(), actionData.descriptor());
    }
}
