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

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class AirtagsConfiguration {

    @Comment(
        """
        The number of airtags a player may own when they have no prism.airtag.limit.<n>
        permission node. Set to -1 for unlimited."""
    )
    private int defaultLimit = -1;

    @Comment("Item tags whose members may be airtagged.")
    private List<String> tags = new ArrayList<>(
        List.of(
            "minecraft:swords",
            "minecraft:axes",
            "minecraft:pickaxes",
            "minecraft:shovels",
            "minecraft:hoes",
            "minecraft:head_armor",
            "minecraft:chest_armor",
            "minecraft:leg_armor",
            "minecraft:foot_armor"
        )
    );

    @Comment("Materials that may be airtagged.")
    private List<String> materials = new ArrayList<>(
        List.of(
            "elytra",
            "totem_of_undying",
            "trident",
            "mace",
            "shield",
            "bow",
            "crossbow",
            "fishing_rod",
            "enchanted_golden_apple",
            "nether_star"
        )
    );
}
