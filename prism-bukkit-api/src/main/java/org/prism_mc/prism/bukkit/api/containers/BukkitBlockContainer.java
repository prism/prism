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

package org.prism_mc.prism.bukkit.api.containers;

import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.containers.BlockContainer;
import org.prism_mc.prism.api.containers.TranslatableContainer;
import org.prism_mc.prism.api.util.Coordinate;

public class BukkitBlockContainer extends TranslatableContainer implements BlockContainer {

    /**
     * The block namespace.
     */
    @Getter
    private final String blockNamespace;

    /**
     * The block name.
     */
    @Getter
    private final String blockName;

    /**
     * The block data.
     */
    @Getter
    private final BlockData blockData;

    /**
     * Construct a block container.
     *
     * @param blockState The block state
     */
    public BukkitBlockContainer(BlockState blockState) {
        this(blockState.getBlockData(), blockState.getType().getBlockTranslationKey());
    }

    /**
     * Construct a block container.
     *
     * @param blockData The block data
     * @param translationKey The translation key
     */
    public BukkitBlockContainer(BlockData blockData, String translationKey) {
        super(translationKey);
        this.blockData = blockData;

        // Removes all block data and splits the namespaced key into namespace/block name
        var segments = this.blockData.getAsString().replaceAll("\\[.*$", "").split(":");
        if (segments.length > 1) {
            this.blockNamespace = segments[0];
            this.blockName = segments[1];
        } else {
            this.blockNamespace = "";
            this.blockName = segments[0];
        }
    }

    /**
     * Construct a block state action.
     *
     * @param blockNamespace The namespace
     * @param blockName The name
     * @param blockData The block data
     * @param translationKey The translation key
     */
    public BukkitBlockContainer(String blockNamespace, String blockName, BlockData blockData, String translationKey) {
        super(translationKey);
        this.blockNamespace = blockNamespace;
        this.blockName = blockName;
        this.blockData = blockData;
    }

    @Override
    public @Nullable String serializeBlockData() {
        return this.blockData != null ? this.blockData.getAsString(true).replaceAll("^[^\\[]+", "") : "";
    }

    /**
     * A convenience method for getting a location.
     *
     * @param worldUuid The world uuid
     * @param coordinate The coordinate
     * @return The location
     */
    protected Location location(UUID worldUuid, Coordinate coordinate) {
        World world = Bukkit.getWorld(worldUuid);
        return new Location(world, coordinate.x(), coordinate.y(), coordinate.z());
    }

    @Override
    public String toString() {
        return String.format(
            "BlockContainer{namespace=%s,name=%s,blockData=%s,translationKey=%s}",
            blockNamespace,
            blockName,
            blockData,
            translationKey
        );
    }
}
