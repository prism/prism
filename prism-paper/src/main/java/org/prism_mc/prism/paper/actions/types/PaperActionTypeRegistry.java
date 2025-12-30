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

import com.google.inject.Singleton;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.core.actions.types.AbstractActionTypeRegistry;

@Singleton
public class PaperActionTypeRegistry extends AbstractActionTypeRegistry {

    /**
     * Static cache of action types.
     */
    public static final ActionType BED_ENTER = new BlockActionType("bed-enter", ActionResultType.NONE, false);
    public static final ActionType BLOCK_BREAK = new BlockActionType("block-break", ActionResultType.REMOVES, true);
    public static final ActionType BLOCK_FADE = new BlockActionType("block-fade", ActionResultType.REMOVES, true);
    public static final ActionType BLOCK_FORM = new BlockActionType("block-form", ActionResultType.CREATES, true);
    public static final ActionType BLOCK_HARVEST = new BlockActionType("block-harvest", ActionResultType.CREATES, true);
    public static final ActionType BLOCK_IGNITE = new BlockActionType("block-ignite", ActionResultType.NONE, false);
    public static final ActionType BLOCK_PLACE = new BlockActionType("block-place", ActionResultType.CREATES, true);
    public static final ActionType BLOCK_SHIFT = new BlockActionType("block-shift", ActionResultType.NONE, false);
    public static final ActionType BLOCK_SPREAD = new BlockActionType("block-spread", ActionResultType.CREATES, true);
    public static final ActionType BLOCK_USE = new BlockActionType("block-use", ActionResultType.NONE, false);
    public static final ActionType BONEMEAL_USE = new BlockActionType("bonemeal-use", ActionResultType.NONE, false);
    public static final ActionType BUCKET_EMPTY = new ItemActionType("bucket-empty", ActionResultType.NONE, false);
    public static final ActionType BUCKET_FILL = new ItemActionType("bucket-fill", ActionResultType.NONE, false);
    public static final ActionType ENTITY_DEATH = new EntityActionType("entity-death", ActionResultType.REMOVES, true);
    public static final ActionType ENTITY_DYE = new EntityActionType("entity-dye", ActionResultType.REPLACES, true);
    public static final ActionType ENTITY_EAT = new BlockActionType("entity-eat", ActionResultType.REMOVES, true);
    public static final ActionType ENTITY_LEASH = new EntityActionType("entity-leash", ActionResultType.NONE, false);
    public static final ActionType ENTITY_PLACE = new EntityActionType("entity-place", ActionResultType.CREATES, true);
    public static final ActionType ENTITY_REMOVE = new EntityActionType(
        "entity-remove",
        ActionResultType.REMOVES,
        true
    );
    public static final ActionType ENTITY_SHEAR = new EntityActionType("entity-shear", ActionResultType.CREATES, true);
    public static final ActionType ENTITY_RIDE = new EntityActionType("entity-ride", ActionResultType.NONE, false);
    public static final ActionType ENTITY_TRANSFORM = new EntityActionType(
        "entity-transform",
        ActionResultType.CREATES,
        false
    );
    public static final ActionType ENTITY_UNLEASH = new EntityActionType(
        "entity-unleash",
        ActionResultType.NONE,
        false
    );
    public static final ActionType FIREWORK_LAUNCH = new GenericActionType(
        "firework-launch",
        ActionResultType.NONE,
        false
    );
    public static final ActionType FLUID_FLOW = new BlockActionType("fluid-flow", ActionResultType.CREATES, true);
    public static final ActionType HANGING_BREAK = new EntityActionType(
        "hanging-break",
        ActionResultType.REMOVES,
        true
    );
    public static final ActionType HANGING_PLACE = new EntityActionType(
        "hanging-place",
        ActionResultType.CREATES,
        true
    );
    public static final ActionType HOPPER_INSERT = new ItemActionType("hopper-insert", ActionResultType.CREATES, false);
    public static final ActionType HOPPER_REMOVE = new ItemActionType("hopper-remove", ActionResultType.REMOVES, true);
    public static final ActionType INVENTORY_OPEN = new BlockActionType("inventory-open", ActionResultType.NONE, false);
    public static final ActionType ITEM_DISPENSE = new ItemActionType("item-dispense", ActionResultType.NONE, false);
    public static final ActionType ITEM_DROP = new ItemActionType("item-drop", ActionResultType.REMOVES, true);
    public static final ActionType ITEM_INSERT = new ItemActionType("item-insert", ActionResultType.CREATES, false);
    public static final ActionType ITEM_PICKUP = new ItemActionType("item-pickup", ActionResultType.NONE, false);
    public static final ActionType ITEM_REMOVE = new ItemActionType("item-remove", ActionResultType.REMOVES, true);
    public static final ActionType ITEM_ROTATE = new ItemActionType("item-rotate", ActionResultType.NONE, false);
    // This is a generic action type because a. it can be an item or entity and b. it can't be modified
    public static final ActionType ITEM_THROW = new GenericActionType("item-throw", ActionResultType.NONE, false);
    public static final ActionType ITEM_USE = new GenericActionType("item-use", ActionResultType.NONE, false);
    public static final ActionType PLAYER_COMMAND = new GenericActionType(
        "player-command",
        ActionResultType.NONE,
        false
    );
    public static final ActionType PLAYER_DEATH = new PlayerActionType("player-death", ActionResultType.REMOVES, false);
    public static final ActionType PLAYER_JOIN = new GenericActionType(
        "player-join",
        ActionResultType.NONE,
        false,
        false
    );
    public static final ActionType PLAYER_QUIT = new GenericActionType(
        "player-quit",
        ActionResultType.NONE,
        false,
        false
    );
    public static final ActionType PLAYER_TELEPORT = new GenericActionType(
        "player-teleport",
        ActionResultType.NONE,
        false
    );
    public static final ActionType RAID_TRIGGER = new GenericActionType(
        "raid-trigger",
        ActionResultType.NONE,
        false,
        false
    );
    public static final ActionType SIGN_EDIT = new BlockActionType("sign-edit", ActionResultType.CREATES, false);
    public static final ActionType VEHICLE_BREAK = new EntityActionType(
        "vehicle-break",
        ActionResultType.REMOVES,
        true
    );
    public static final ActionType VEHICLE_EXIT = new EntityActionType("vehicle-exit", ActionResultType.NONE, false);
    public static final ActionType VEHICLE_PLACE = new EntityActionType(
        "vehicle-place",
        ActionResultType.CREATES,
        true
    );
    public static final ActionType VEHICLE_RIDE = new EntityActionType("vehicle-ride", ActionResultType.NONE, false);
    public static final ActionType WORLDEDIT_BREAK = new BlockActionType(
        "worldedit-break",
        ActionResultType.REMOVES,
        true
    );
    public static final ActionType WORLDEDIT_PLACE = new BlockActionType(
        "worldedit-place",
        ActionResultType.CREATES,
        true
    );
    public static final ActionType XP_PICKUP = new GenericActionType("xp-pickup", ActionResultType.NONE, false);

    /**
     * Construct the action registry.
     */
    public PaperActionTypeRegistry() {
        // Register Prism actions
        registerAction(BED_ENTER);
        registerAction(BLOCK_BREAK);
        registerAction(BLOCK_FADE);
        registerAction(BLOCK_FORM);
        registerAction(BLOCK_HARVEST);
        registerAction(BLOCK_IGNITE);
        registerAction(BLOCK_PLACE);
        registerAction(BLOCK_SHIFT);
        registerAction(BLOCK_SPREAD);
        registerAction(BLOCK_USE);
        registerAction(BONEMEAL_USE);
        registerAction(BUCKET_EMPTY);
        registerAction(BUCKET_FILL);
        registerAction(ENTITY_DEATH);
        registerAction(ENTITY_DYE);
        registerAction(ENTITY_EAT);
        registerAction(ENTITY_LEASH);
        registerAction(ENTITY_PLACE);
        registerAction(ENTITY_REMOVE);
        registerAction(ENTITY_RIDE);
        registerAction(ENTITY_SHEAR);
        registerAction(ENTITY_TRANSFORM);
        registerAction(ENTITY_UNLEASH);
        registerAction(FIREWORK_LAUNCH);
        registerAction(FLUID_FLOW);
        registerAction(HANGING_BREAK);
        registerAction(HANGING_PLACE);
        registerAction(HOPPER_INSERT);
        registerAction(HOPPER_REMOVE);
        registerAction(INVENTORY_OPEN);
        registerAction(ITEM_DISPENSE);
        registerAction(ITEM_DROP);
        registerAction(ITEM_INSERT);
        registerAction(ITEM_PICKUP);
        registerAction(ITEM_REMOVE);
        registerAction(ITEM_ROTATE);
        registerAction(ITEM_THROW);
        registerAction(ITEM_USE);
        registerAction(PLAYER_COMMAND);
        registerAction(PLAYER_DEATH);
        registerAction(PLAYER_JOIN);
        registerAction(PLAYER_QUIT);
        registerAction(PLAYER_TELEPORT);
        registerAction(RAID_TRIGGER);
        registerAction(SIGN_EDIT);
        registerAction(VEHICLE_BREAK);
        registerAction(VEHICLE_EXIT);
        registerAction(VEHICLE_PLACE);
        registerAction(VEHICLE_RIDE);
        registerAction(WORLDEDIT_BREAK);
        registerAction(WORLDEDIT_PLACE);
        registerAction(XP_PICKUP);
    }
}
