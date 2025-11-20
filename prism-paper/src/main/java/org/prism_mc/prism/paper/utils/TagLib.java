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

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.Tag;

@UtilityClass
public class TagLib {

    /**
     * Buckets which produce a water block when emptied outside of water.
     */
    public static final CustomTag<Material> WATER_BUCKETS = new CustomTag<>(
        Material.class,
        Material.AXOLOTL_BUCKET,
        Material.COD_BUCKET,
        Material.PUFFERFISH_BUCKET,
        Material.SALMON_BUCKET,
        Material.TROPICAL_FISH_BUCKET,
        Material.WATER_BUCKET,
        Material.TADPOLE_BUCKET
    );

    /**
     * Some blocks are always "waterlogged" and not actually subclasses of Waterlogged.
     */
    public static final CustomTag<Material> WATER_BLOCKS = new CustomTag<>(
        Material.class,
        Material.WATER,
        Material.BUBBLE_COLUMN,
        Material.KELP,
        Material.KELP_PLANT,
        Material.SEAGRASS,
        Material.TALL_SEAGRASS,
        Material.TUBE_CORAL,
        Material.BRAIN_CORAL,
        Material.BUBBLE_CORAL,
        Material.FIRE_CORAL,
        Material.CONDUIT
    );

    /**
     * Buckets which produce a lava block when emptied outside of lava.
     */
    public static final CustomTag<Material> LAVA_BUCKETS = new CustomTag<>(Material.class, Material.LAVA_BUCKET);

    /**
     * All plants that have a one-block structure.
     */
    public static final CustomTag<Material> PLANTS = new CustomTag<>(
        Material.class,
        Material.FERN,
        Material.DEAD_BUSH,
        Material.DANDELION,
        Material.POPPY,
        Material.BLUE_ORCHID,
        Material.ALLIUM,
        Material.AZURE_BLUET,
        Material.RED_TULIP,
        Material.ORANGE_TULIP,
        Material.WHITE_TULIP,
        Material.PINK_TULIP,
        Material.OXEYE_DAISY,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,
        Material.LILY_PAD,
        Material.KELP,
        Material.KELP_PLANT,
        Material.SHORT_GRASS,
        Material.SEAGRASS,
        Material.SWEET_BERRY_BUSH,
        Material.TALL_GRASS,
        Material.TALL_SEAGRASS,
        Material.SEA_PICKLE,
        Material.BIG_DRIPLEAF
    )
        .append(Tag.WALL_CORALS)
        .append(Tag.CORALS);

    static {
        if (VersionUtils.atLeast(1, 21, 5)) {
            PLANTS.append(Material.CACTUS_FLOWER, Material.SHORT_DRY_GRASS, Material.FIREFLY_BUSH);
        }
    }

    /**
     * Plants that have a two-block structure.
     */
    public static final CustomTag<Material> TALL_PLANTS = new CustomTag<>(
        Material.class,
        Material.SUNFLOWER,
        Material.LILAC,
        Material.ROSE_BUSH,
        Material.PEONY,
        Material.TALL_GRASS,
        Material.LARGE_FERN,
        Material.TALL_SEAGRASS
    );

    static {
        if (VersionUtils.atLeast(1, 21, 5)) {
            TALL_PLANTS.append(Material.TALL_DRY_GRASS);
        }
    }

    /**
     * All plants (not counting crops).
     */
    public static final CustomTag<Material> ALL_PLANTS = new CustomTag<>(Material.class, PLANTS).append(TALL_PLANTS);

    /**
     * Ground litter.
     */
    public static final CustomTag<Material> LITTER = new CustomTag<>(Material.class, Material.PINK_PETALS);

    static {
        if (VersionUtils.atLeast(1, 21, 5)) {
            LITTER.append(Material.LEAF_LITTER, Material.WILDFLOWERS);
        }
    }

    /**
     * All vegetation that can be grown.
     */
    public static final CustomTag<Material> GROWABLES = new CustomTag<>(
        Material.class,
        Material.BAMBOO,
        Material.CACTUS,
        Material.KELP,
        Material.KELP_PLANT,
        Material.SUGAR_CANE,
        Material.CHORUS_PLANT,
        Material.CHORUS_FLOWER
    ).append(Tag.CROPS);

    /**
     * All banners that are placed on the top of a block.
     */
    public static final CustomTag<Material> TOP_BANNERS = new CustomTag<>(Material.class, Tag.BANNERS).exclude(
        "_WALL_",
        CustomTag.MatchMode.CONTAINS
    );

    /**
     * Banners hung on a wall.
     */
    public static final CustomTag<Material> WALL_BANNERS = new CustomTag<>(Material.class, Tag.BANNERS).exclude(
        TOP_BANNERS
    );

    /**
     * Materials that will fall due to physics if the supporting block is broken.
     */
    public static final CustomTag<Material> GRAVITY_AFFECTED = new CustomTag<>(
        Material.class,
        Material.GRAVEL,
        Material.ANVIL,
        Material.DRAGON_EGG
    )
        .append(Tag.SAND)
        .append("_CONCRETE_POWDER", CustomTag.MatchMode.SUFFIX);

    /**
     * Materials that attach to any side of a block.
     */
    public static final CustomTag<Material> DETACHABLES = new CustomTag<>(
        Material.class,
        Material.BELL,
        Material.AMETHYST_CLUSTER,
        Material.CHORUS_PLANT
    );

    /**
     * Materials that attach to any side of a block, recursively.
     */
    public static final CustomTag<Material> RECURSIVE_DETACHABLES = new CustomTag<>(
        Material.class,
        Material.CHORUS_PLANT,
        Material.END_PORTAL,
        Material.NETHER_PORTAL
    );

    /**
     * Materials that can have recursive attachments.
     */
    public static final CustomTag<Material> RECURSIVE_DETACHABLE_HOLDERS = new CustomTag<>(
        Material.class,
        Material.END_PORTAL_FRAME,
        Material.CHORUS_FLOWER,
        Material.CHORUS_PLANT,
        Material.OBSIDIAN
    );

    /**
     * All redstone-related items that detach when connected block is broken.
     */
    public static final CustomTag<Material> REDSTONE_DETACHABLE = new CustomTag<>(
        Material.class,
        Material.COMPARATOR,
        Material.LEVER,
        Material.REPEATER,
        Material.REDSTONE_TORCH,
        Material.REDSTONE_WALL_TORCH,
        Material.REDSTONE_WIRE
    ).append(Tag.BUTTONS, Tag.PRESSURE_PLATES);

    /**
     * All materials that can attach to themselves on the bottom (breaks travel downward).
     */
    public static final CustomTag<Material> RECURSIVE_BOTTOM_DETACHABLES = new CustomTag<>(
        Material.class,
        Material.BIG_DRIPLEAF_STEM,
        Material.POINTED_DRIPSTONE,
        Material.VINE,
        Material.WEEPING_VINES,
        Material.WEEPING_VINES_PLANT
    )
        .append(Tag.CAVE_VINES)
        .append(RECURSIVE_DETACHABLES);

    /**
     * Materials that attach to the bottom of a block.
     */
    public static final CustomTag<Material> BOTTOM_DETACHABLES = new CustomTag<>(
        Material.class,
        Material.HANGING_ROOTS,
        Material.SPORE_BLOSSOM,
        Material.LANTERN,
        Material.SOUL_LANTERN
    ).append(DETACHABLES, RECURSIVE_BOTTOM_DETACHABLES);

    static {
        if (VersionUtils.atLeast(1, 21, 9)) {
            BOTTOM_DETACHABLES.append(Tag.LANTERNS);
        }
    }

    /**
     * All materials that can detach from the side of a block.
     */
    public static final CustomTag<Material> SIDE_DETACHABLES = new CustomTag<>(
        Material.class,
        // Pistons
        Material.STICKY_PISTON,
        Material.PISTON,
        Material.PISTON_HEAD,
        Material.MOVING_PISTON,
        // Torches
        Material.WALL_TORCH,
        Material.REDSTONE_WALL_TORCH,
        Material.SOUL_WALL_TORCH,
        // Hanging
        Material.ITEM_FRAME,
        Material.PAINTING,
        // Misc
        Material.COCOA,
        Material.GLOW_LICHEN,
        Material.LEVER,
        Material.SCAFFOLDING,
        Material.TRIPWIRE_HOOK
    )
        .append(Tag.BUTTONS, Tag.WALL_SIGNS, Tag.CLIMBABLE, Tag.WALL_CORALS)
        .append(WALL_BANNERS, DETACHABLES);

    static {
        if (VersionUtils.atLeast(1, 21, 9)) {
            SIDE_DETACHABLES.append(Material.COPPER_WALL_TORCH);
        }
    }

    /**
     * All materials that can attach to themselves on the top (breaks travel upward).
     */
    public static final CustomTag<Material> RECURSIVE_TOP_DETACHABLES = new CustomTag<>(
        Material.class,
        Material.BAMBOO,
        Material.BIG_DRIPLEAF_STEM,
        Material.KELP_PLANT,
        Material.KELP,
        Material.CACTUS,
        Material.POINTED_DRIPSTONE,
        Material.SCAFFOLDING,
        Material.SUGAR_CANE,
        Material.TWISTING_VINES,
        Material.TWISTING_VINES_PLANT
    )
        .append(Tag.CAVE_VINES)
        .append(RECURSIVE_DETACHABLES);

    /**
     * All materials that can detach from the top of a block.
     */
    public static final CustomTag<Material> TOP_DETACHABLES = new CustomTag<>(
        Material.class,
        Material.STICKY_PISTON,
        Material.DEAD_BUSH,
        Material.PISTON,
        Material.PISTON_HEAD,
        Material.MOVING_PISTON,
        Material.TORCH,
        Material.SOUL_TORCH,
        Material.LEVER,
        Material.SNOW,
        Material.LILY_PAD,
        Material.NETHER_WART,
        Material.BEACON,
        Material.ITEM_FRAME,
        Material.CONDUIT,
        Material.BELL,
        Material.MOSS_CARPET,
        Material.LANTERN,
        Material.SOUL_LANTERN
    )
        .append(Tag.DOORS, Tag.RAILS, Tag.SAPLINGS, Tag.STANDING_SIGNS)
        .append(Tag.WOOL_CARPETS, Tag.FLOWER_POTS)
        .append(
            REDSTONE_DETACHABLE,
            GROWABLES,
            ALL_PLANTS,
            DETACHABLES,
            TOP_BANNERS,
            RECURSIVE_TOP_DETACHABLES,
            LITTER
        );

    static {
        if (VersionUtils.atLeast(1, 21, 9)) {
            TOP_DETACHABLES.append(Material.COPPER_TORCH).append(Tag.LANTERNS);
        }
    }

    /**
     * All wall-placeable skulls.
     */
    public static final CustomTag<Material> WALL_SKULLS = new CustomTag<>(
        Material.class,
        Material.SKELETON_WALL_SKULL,
        Material.WITHER_SKELETON_WALL_SKULL,
        Material.CREEPER_WALL_HEAD,
        Material.DRAGON_WALL_HEAD,
        Material.PLAYER_WALL_HEAD,
        Material.ZOMBIE_WALL_HEAD
    );

    /**
     * All floor-placeable skulls.
     */
    public static final CustomTag<Material> FLOOR_SKULLS = new CustomTag<>(
        Material.class,
        Material.SKELETON_SKULL,
        Material.WITHER_SKELETON_SKULL,
        Material.CREEPER_HEAD,
        Material.DRAGON_HEAD,
        Material.PLAYER_HEAD,
        Material.ZOMBIE_HEAD
    );

    /**
     * All skulls.
     */
    public static final CustomTag<Material> ALL_SKULLS = new CustomTag<>(Material.class, WALL_SKULLS).append(
        FLOOR_SKULLS
    );

    /**
     * All materials that can be broken by fluid flowing.
     */
    public static final CustomTag<Material> FLUID_BREAKABLE = new CustomTag<>(Material.class, TOP_DETACHABLES)
        .append(Material.END_ROD)
        .append(Material.TRIPWIRE)
        .append(ALL_SKULLS);

    /**
     * All materials that can be "used" - opened but doesn't store inventory or performs some action.
     */
    public static final CustomTag<Material> USABLE = new CustomTag<>(
        Material.class,
        Material.ANVIL,
        Material.BELL,
        Material.CARTOGRAPHY_TABLE,
        Material.COMPARATOR,
        Material.CRAFTING_TABLE,
        Material.DRAGON_EGG,
        Material.ENCHANTING_TABLE,
        Material.FLETCHING_TABLE,
        Material.GRINDSTONE,
        Material.LEVER,
        Material.LOOM,
        Material.NOTE_BLOCK,
        Material.REPEATER,
        Material.SMITHING_TABLE,
        Material.STONECUTTER
    )
        .append(Tag.BUTTONS)
        .append(Tag.DOORS)
        .append(Tag.TRAPDOORS)
        .append(Tag.FENCE_GATES)
        .append(Tag.PRESSURE_PLATES);

    /**
     * Tag blocks which retain their inventory when broken.
     */
    public static final CustomTag<Material> KEEPS_INVENTORY = new CustomTag<>(
        Material.class,
        Material.SHULKER_BOX
    ).append(Tag.SHULKER_BOXES);

    /**
     * Tag all armor items.
     */
    public static final CustomTag<Material> ALL_ARMOR = new CustomTag<>(Material.class).append(
        Tag.ITEMS_FOOT_ARMOR,
        Tag.ITEMS_LEG_ARMOR,
        Tag.ITEMS_CHEST_ARMOR,
        Tag.ITEMS_HEAD_ARMOR
    );

    /**
     * Spawn eggs.
     */
    public static final CustomTag<Material> SPAWN_EGGS = new CustomTag<>(
        Material.class,
        Material.ALLAY_SPAWN_EGG,
        Material.ARMADILLO_SPAWN_EGG,
        Material.AXOLOTL_SPAWN_EGG,
        Material.BAT_SPAWN_EGG,
        Material.BEE_SPAWN_EGG,
        Material.BLAZE_SPAWN_EGG,
        Material.BOGGED_SPAWN_EGG,
        Material.BREEZE_SPAWN_EGG,
        Material.CAMEL_SPAWN_EGG,
        Material.CAT_SPAWN_EGG,
        Material.CAVE_SPIDER_SPAWN_EGG,
        Material.CHICKEN_SPAWN_EGG,
        Material.COD_SPAWN_EGG,
        Material.CREAKING_SPAWN_EGG,
        Material.CREEPER_SPAWN_EGG,
        Material.DOLPHIN_SPAWN_EGG,
        Material.DONKEY_SPAWN_EGG,
        Material.DROWNED_SPAWN_EGG,
        Material.ELDER_GUARDIAN_SPAWN_EGG,
        Material.ENDER_DRAGON_SPAWN_EGG,
        Material.ENDERMAN_SPAWN_EGG,
        Material.ENDERMITE_SPAWN_EGG,
        Material.EVOKER_SPAWN_EGG,
        Material.FOX_SPAWN_EGG,
        Material.GHAST_SPAWN_EGG,
        Material.GLOW_SQUID_SPAWN_EGG,
        Material.GUARDIAN_SPAWN_EGG,
        Material.HOGLIN_SPAWN_EGG,
        Material.HORSE_SPAWN_EGG,
        Material.IRON_GOLEM_SPAWN_EGG,
        Material.LLAMA_SPAWN_EGG,
        Material.MAGMA_CUBE_SPAWN_EGG,
        Material.MOOSHROOM_SPAWN_EGG,
        Material.MULE_SPAWN_EGG,
        Material.OCELOT_SPAWN_EGG,
        Material.PANDA_SPAWN_EGG,
        Material.PHANTOM_SPAWN_EGG,
        Material.PIG_SPAWN_EGG,
        Material.PIGLIN_BRUTE_SPAWN_EGG,
        Material.PIGLIN_SPAWN_EGG,
        Material.PILLAGER_SPAWN_EGG,
        Material.POLAR_BEAR_SPAWN_EGG,
        Material.PUFFERFISH_SPAWN_EGG,
        Material.RABBIT_SPAWN_EGG,
        Material.SALMON_SPAWN_EGG,
        Material.SHEEP_SPAWN_EGG,
        Material.SHULKER_SPAWN_EGG,
        Material.SILVERFISH_SPAWN_EGG,
        Material.SKELETON_HORSE_SPAWN_EGG,
        Material.SKELETON_SPAWN_EGG,
        Material.SLIME_SPAWN_EGG,
        Material.SNIFFER_SPAWN_EGG,
        Material.SNOW_GOLEM_SPAWN_EGG,
        Material.SPIDER_SPAWN_EGG,
        Material.SQUID_SPAWN_EGG,
        Material.STRAY_SPAWN_EGG,
        Material.STRIDER_SPAWN_EGG,
        Material.TADPOLE_SPAWN_EGG,
        Material.TRADER_LLAMA_SPAWN_EGG,
        Material.TROPICAL_FISH_SPAWN_EGG,
        Material.TURTLE_SPAWN_EGG,
        Material.VEX_SPAWN_EGG,
        Material.VILLAGER_SPAWN_EGG,
        Material.VINDICATOR_SPAWN_EGG,
        Material.WANDERING_TRADER_SPAWN_EGG,
        Material.WARDEN_SPAWN_EGG,
        Material.WITCH_SPAWN_EGG,
        Material.WITHER_SPAWN_EGG,
        Material.WOLF_SPAWN_EGG,
        Material.ZOGLIN_SPAWN_EGG,
        Material.ZOMBIE_HORSE_SPAWN_EGG,
        Material.ZOMBIE_SPAWN_EGG,
        Material.ZOMBIE_VILLAGER_SPAWN_EGG,
        Material.ZOMBIFIED_PIGLIN_SPAWN_EGG
    );

    static {
        if (VersionUtils.atLeast(1, 21, 6)) {
            SPAWN_EGGS.append(Material.HAPPY_GHAST_SPAWN_EGG);
        }
    }

    /**
     * All usable items (for tracking item-use).
     */
    public static final CustomTag<Material> USABLE_ITEMS = new CustomTag<>(Material.class).append(TagLib.SPAWN_EGGS);

    /**
     * All items lecterns can hold.
     */
    public static final CustomTag<Material> LECTERN_ITEMS = new CustomTag<>(
        Material.class,
        Material.WRITTEN_BOOK,
        Material.WRITABLE_BOOK
    );
}
