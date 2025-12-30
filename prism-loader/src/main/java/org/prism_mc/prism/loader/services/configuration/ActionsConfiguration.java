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

package org.prism_mc.prism.loader.services.configuration;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class ActionsConfiguration {

    @Comment("bed-enter is when a player or entity gets into bed.")
    private boolean bedEnter = false;

    @Comment("block-break is when a player or entity destroys a block (except from burn/explode).")
    private boolean blockBreak = true;

    @Comment("block-fade is when a block fades - melting snow, etc.")
    private boolean blockFade = false;

    @Comment("block-form is when a block forms - concrete, snow, etc.")
    private boolean blockForm = true;

    @Comment("block-harvest is when a player harvests a block without breaking it (sweet berries).")
    private boolean blockHarvest = true;

    @Comment(
        """
        block-ignite is when a block is lit on fire.
        Note: Fire spreads to other blocks (block-spread). This records an initial ignition."""
    )
    private boolean blockIgnite = false;

    @Comment("block-place is when a player or entity places a block.")
    private boolean blockPlace = true;

    @Comment(
        """
        block-shift is when a piston pushes or pulls a block.
        It's disabled by default because these are difficult to rollback
        and can become extremely spammy due to redstone contraptions.
        It WILL record dragon egg breaks even if block-shift is disabled."""
    )
    private boolean blockShift = false;

    @Comment(
        """
        block-spread is when a block spreads to a new block. Fire, mushrooms, etc.
        This event can produce a lot of data. There's a lot of natural gen you may
        wish to ignore. For example, vine/cave_vines all produce spread events
        as they grow, and grass blocks as it's spreads to dirt.
        A filter to ignore those is strongly recommended if you enable this."""
    )
    private boolean blockSpread = false;

    @Comment("block-use is when a player uses a block (buttons, doors, anvil, enchanting table, etc).")
    private boolean blockUse = true;

    @Comment("bonemeal-use is when a player uses bonemeal on a block (grown trees/grass/flowers are block-place).")
    private boolean bonemealUse = false;

    @Comment("bucket-empty is when a player places a block by emptying a bucket.")
    private boolean bucketEmpty = true;

    @Comment("bucket-fill is when a player removes a block by filling a bucket.")
    private boolean bucketFill = true;

    @Comment("entity-death is when a non-player entity dies.")
    private boolean entityDeath = true;

    @Comment("entity-dye is when a player dyes an entity.")
    private boolean entityDye = false;

    @Comment("entity-eat is when an entity eats a block and changes it.")
    private boolean entityEat = false;

    @Comment("entity-leash is when a player leashes an entity.")
    private boolean entityLeash = false;

    @Comment("entity-place is when a player places an armor stand or end crystal.")
    private boolean entityPlace = true;

    @Comment("entity-remove is when a player collects an entity, like fish with a water bucket.")
    private boolean entityRemove = true;

    @Comment("entity-ride is when a player rides an entity.")
    private boolean entityRide = false;

    @Comment("entity-shear is when a player shears an entity.")
    private boolean entityShear = false;

    @Comment("entity-transform is for villager healing, tadpoles to frogs, etc.")
    private boolean entityTransform = false;

    @Comment("entity-unleash is when a player or natural cause unleashes an entity.")
    private boolean entityUnleash = true;

    @Comment("firework-launch is when a player launches a firework rocket.")
    private boolean fireworkLaunch = false;

    @Comment(
        """
        fluid-flow tracks the flow of water and lava. Even when this is false,
        prism will record blocks broken by either.
        We very strongly recommend leaving this false and using
        a drain command instead. This produces an insane amount of activity data."""
    )
    private boolean fluidFlow = false;

    @Comment(
        """
        hanging-break is when an item frame or painting is broken/detached.
        This event will operate if block-break is false, even for detachments."""
    )
    private boolean hangingBreak = true;

    @Comment("hanging-place is when an item frame or painting is hung up.")
    private boolean hangingPlace = true;

    @Comment(
        """
        hopper-insert is when a hopper inserts an item into an inventory.
        We *strongly* recommend using robust inventory/land protection plugins instead of enabling this
        action because hoppers move one item at a time and can lead to MAJOR database spam, especially in farms."""
    )
    private boolean hopperInsert = false;

    @Comment(
        """
        hopper-remove is when a hopper removes an item from an inventory.
        We *strongly* recommend using robust inventory/land protection plugins instead of enabling this
        action because hoppers move one item at a time and can lead to MAJOR database spam, especially in farms."""
    )
    private boolean hopperRemove = false;

    @Comment(
        """
        inventory-open is when a player opens an inventory (chest, furnace, etc).
        Item inserts or removes are tracked separately."""
    )
    private boolean inventoryOpen = false;

    @Comment("item-dispense is when a block dispenses an item.")
    private boolean itemDispense = true;

    @Comment("item-drop is when a player or block drops an item on the ground.")
    private boolean itemDrop = true;

    @Comment("item-insert is when a player or block inserts an item into an inventory.")
    private boolean itemInsert = true;

    @Comment("item-pickup is when a player or entity picks up an item from the ground.")
    private boolean itemPickup = true;

    @Comment("item-remove is when a player or block removes an item from an inventory.")
    private boolean itemRemove = true;

    @Comment("item-rotate is when a player rotates an item inside an item frame.")
    private boolean itemRotate = false;

    @Comment("item-throw is when a player throws an item.")
    private boolean itemThrow = true;

    @Comment("item-use is when a player uses an item.")
    private boolean itemUse = true;

    @Comment("player-command is when a player tries to use a command.")
    private boolean playerCommand = false;

    @Comment("player-death is when a player dies.")
    private boolean playerDeath = true;

    @Comment("player-join is when a player connects to the server.")
    private boolean playerJoin = false;

    @Comment("player-quit is when a player disconnects from the server.")
    private boolean playerQuit = false;

    @Comment("player-teleport is when a player teleports.")
    private boolean playerTeleport = false;

    @Comment("raid-trigger is when a player triggers a raid.")
    private boolean raidTrigger = true;

    @Comment("sign-edit is when a player edits the text on a sign.")
    private boolean signEdit = true;

    @Comment("vehicle-break is when something breaks a boat or minecart.")
    private boolean vehicleBreak = true;

    @Comment("vehicle-exit is when an entity exits a boat or minecart.")
    private boolean vehicleExit = true;

    @Comment("vehicle-place is when a player places a boat or minecart.")
    private boolean vehiclePlace = true;

    @Comment("vehicle-ride is when an entity enters a boat or minecart.")
    private boolean vehicleRide = true;

    @Comment(
        """
        worldedit-break is when a block is removed via WorldEdit.
        Requires WorldEdit to be installed."""
    )
    private boolean worldeditBreak = false;

    @Comment(
        """
        worldedit-place is when a block is placed via WorldEdit.
        Requires WorldEdit to be installed."""
    )
    private boolean worldeditPlace = false;

    @Comment("xp-pickup is when a player picks up XP orbs.")
    private boolean xpPickup = false;
}
