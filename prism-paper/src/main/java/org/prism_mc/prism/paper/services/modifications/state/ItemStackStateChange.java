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

package org.prism_mc.prism.paper.services.modifications.state;

import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.services.modifications.StateChange;

public class ItemStackStateChange extends StateChange<ItemStack> {

    /**
     * Construct a new item stack state change.
     *
     * @param oldState The old state
     * @param newState The new state
     */
    public ItemStackStateChange(ItemStack oldState, ItemStack newState) {
        super(oldState, newState);
    }
}
