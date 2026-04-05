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

package org.prism_mc.prism.paper.actions.factory;

import com.google.inject.Singleton;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.paper.actions.GenericPaperAction;
import org.prism_mc.prism.paper.actions.PaperBlockAction;
import org.prism_mc.prism.paper.actions.PaperEntityAction;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.PaperPlayerAction;

@Singleton
public class PaperActionFactory implements org.prism_mc.prism.paper.api.actions.PrismPaperActionFactory {

    @Override
    public Action createGenericAction(ActionType actionType) {
        return new GenericPaperAction(actionType);
    }

    @Override
    public Action createGenericAction(ActionType actionType, String descriptor) {
        return new GenericPaperAction(actionType, descriptor);
    }

    @Override
    public Action createGenericAction(ActionType actionType, String descriptor, @Nullable Metadata metadata) {
        return new GenericPaperAction(actionType, descriptor, metadata);
    }

    @Override
    public Action createBlockAction(ActionType actionType, BlockState block) {
        return createBlockAction(actionType, block, null);
    }

    @Override
    public Action createBlockAction(ActionType actionType, BlockState block, @Nullable BlockState replacedBlock) {
        return new PaperBlockAction(actionType, block, replacedBlock);
    }

    @Override
    public Action createEntityAction(ActionType actionType, Entity entity) {
        return createEntityAction(actionType, entity, null);
    }

    @Override
    public Action createEntityAction(ActionType actionType, Entity entity, @Nullable Metadata metadata) {
        if (metadata != null) {
            return new PaperEntityAction(actionType, entity, metadata);
        }

        return new PaperEntityAction(actionType, entity);
    }

    @Override
    public Action createItemAction(ActionType actionType, ItemStack itemStack) {
        return new PaperItemStackAction(actionType, itemStack);
    }

    @Override
    public Action createItemAction(ActionType actionType, ItemStack itemStack, int quantity, String descriptor) {
        return new PaperItemStackAction(actionType, itemStack, quantity, descriptor);
    }

    @Override
    public Action createPlayerAction(ActionType actionType, Player player) {
        return new PaperPlayerAction(actionType, player);
    }
}
