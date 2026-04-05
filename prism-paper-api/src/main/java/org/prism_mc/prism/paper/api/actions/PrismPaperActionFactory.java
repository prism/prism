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

package org.prism_mc.prism.paper.api.actions;

import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ActionFactory;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionType;

public interface PrismPaperActionFactory extends ActionFactory {
    /**
     * Create a block action.
     *
     * @param actionType The action type
     * @param block The block state
     * @return The action
     */
    Action createBlockAction(ActionType actionType, BlockState block);

    /**
     * Create a block action with a replaced block.
     *
     * @param actionType The action type
     * @param block The block state
     * @param replacedBlock The replaced block state
     * @return The action
     */
    Action createBlockAction(ActionType actionType, BlockState block, @Nullable BlockState replacedBlock);

    /**
     * Create an entity action.
     *
     * @param actionType The action type
     * @param entity The entity
     * @return The action
     */
    Action createEntityAction(ActionType actionType, Entity entity);

    /**
     * Create an entity action with metadata.
     *
     * @param actionType The action type
     * @param entity The entity
     * @param metadata The metadata
     * @return The action
     */
    Action createEntityAction(ActionType actionType, Entity entity, @Nullable Metadata metadata);

    /**
     * Create an item action.
     *
     * @param actionType The action type
     * @param itemStack The item stack
     * @return The action
     */
    Action createItemAction(ActionType actionType, ItemStack itemStack);

    /**
     * Create an item action with a custom quantity and descriptor.
     *
     * @param actionType The action type
     * @param itemStack The item stack
     * @param quantity The quantity
     * @param descriptor The descriptor
     * @return The action
     */
    Action createItemAction(ActionType actionType, ItemStack itemStack, int quantity, String descriptor);

    /**
     * Create a player action.
     *
     * @param actionType The action type
     * @param player The player
     * @return The action
     */
    Action createPlayerAction(ActionType actionType, Player player);
}
