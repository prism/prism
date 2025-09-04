/*
 * Prism
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

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class VeinScanner {

    /**
     * The current scan count.
     */
    private int scanCount = 0;

    /**
     * All matching locations.
     */
    private final List<Location> locations = new ArrayList<>();

    /**
     * The max scan count.
     */
    private final int maxScanCount;

    /**
     * The start block.
     */
    private final BlockState startBlock;

    /**
     * The material tags to count as a vein member.
     */
    private final Tag<Material> materialTag;

    /**
     * Constructor.
     *
     * @param startBlock The start block
     * @param materialTag The material tag
     * @param maxScanCount The max scan count
     */
    public VeinScanner(BlockState startBlock, Tag<Material> materialTag, int maxScanCount) {
        this.startBlock = startBlock;
        this.materialTag = materialTag;
        this.maxScanCount = maxScanCount;
    }

    /**
     * Scan neighboring blocks for vein members.
     *
     * @return All vein member locations
     */
    public List<Location> scan() {
        scanVein(startBlock);

        return locations;
    }

    /**
     * Recursively scan neighbors.
     *
     * @param currentBlock The current "basis" block
     */
    protected void scanVein(BlockState currentBlock) {
        if (materialTag.isTagged(currentBlock.getType())) {
            locations.add(currentBlock.getLocation());

            // Scan all immediate neighbors
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    for (int y = -1; y <= 1; y++) {
                        final Block neighbor = currentBlock.getBlock().getRelative(x, y, z);
                        // Ensure it matches the tag and wasn't already found
                        if (materialTag.isTagged(neighbor.getType()) && !locations.contains(neighbor.getLocation())) {
                            scanCount++;
                            if (scanCount <= maxScanCount) {
                                scanVein(neighbor.getState());
                            }
                        }
                    }
                }
            }
        }
    }
}
