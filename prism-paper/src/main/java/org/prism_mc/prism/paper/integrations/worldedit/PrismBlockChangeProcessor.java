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

package org.prism_mc.prism.paper.integrations.worldedit;

import com.fastasyncworldedit.core.extent.processor.ProcessorScope;
import com.fastasyncworldedit.core.queue.IBatchProcessor;
import com.fastasyncworldedit.core.queue.IChunk;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.fastasyncworldedit.core.queue.IChunkSet;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypesCache;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperBlockAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

/**
 * FAWE-specific batch processor that logs block changes to Prism.
 *
 * <p>This processor intercepts chunk-level block changes made via FastAsyncWorldEdit
 * operations and logs them as worldedit-break and worldedit-place activities.</p>
 */
public class PrismBlockChangeProcessor implements IBatchProcessor {

    /**
     * The Bukkit player performing the operation.
     */
    private final Player player;

    /**
     * The Bukkit world.
     */
    private final World bukkitWorld;

    /**
     * The recording service.
     */
    private final PaperRecordingService recordingService;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * Construct a PrismBlockChangeProcessor.
     *
     * @param player The player performing the operation (may be null)
     * @param bukkitWorld The Bukkit world
     * @param recordingService The recording service
     * @param configurationService The configuration service
     */
    public PrismBlockChangeProcessor(
        Player player,
        World bukkitWorld,
        PaperRecordingService recordingService,
        ConfigurationService configurationService
    ) {
        this.player = player;
        this.bukkitWorld = bukkitWorld;
        this.recordingService = recordingService;
        this.configurationService = configurationService;
    }

    @Override
    public IChunkSet processSet(IChunk chunk, IChunkGet get, IChunkSet set) {
        boolean logBreak = configurationService.prismConfig().actions().worldeditBreak();
        boolean logPlace = configurationService.prismConfig().actions().worldeditPlace();

        if (!logBreak && !logPlace) {
            return set;
        }

        Object cause = player != null ? player : "WorldEdit";

        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        // Iterate through all layers (sections) in the chunk
        int minSection = get.getMinSectionPosition();
        int maxSection = get.getMaxSectionPosition();

        for (int layer = minSection; layer <= maxSection; layer++) {
            // Check if this section has any changes
            if (!set.hasSection(layer)) {
                continue;
            }

            char[] setBlocks = set.loadIfPresent(layer);
            if (setBlocks == null) {
                continue;
            }

            char[] getBlocks = get.load(layer);

            int baseY = layer << 4; // layer * 16

            for (int i = 0; i < setBlocks.length; i++) {
                char ordinalSet = setBlocks[i];
                if (ordinalSet == BlockTypesCache.ReservedIDs.__RESERVED__) {
                    continue; // No change at this position
                }

                char ordinalGet = getBlocks != null ? getBlocks[i] : 0;

                // Get block states
                BlockState oldState = BlockTypesCache.states[ordinalGet];
                BlockState newState = BlockTypesCache.states[ordinalSet];

                if (oldState == null || newState == null) {
                    continue;
                }

                // Skip if no actual change
                if (oldState.equals(newState)) {
                    continue;
                }

                boolean oldIsAir = oldState.getBlockType().getMaterial().isAir();
                boolean newIsAir = newState.getBlockType().getMaterial().isAir();

                // Calculate block coordinates
                int localX = i & 15;
                int localY = (i >> 8) & 15;
                int localZ = (i >> 4) & 15;

                int worldX = (chunkX << 4) + localX;
                int worldY = baseY + localY;
                int worldZ = (chunkZ << 4) + localZ;

                Location location = new Location(bukkitWorld, worldX, worldY, worldZ);

                // Log block removal (break) only if replacing with air (actual removal)
                if (!oldIsAir && newIsAir && logBreak) {
                    BlockData oldBlockData = BukkitAdapter.adapt(oldState);
                    String oldTranslationKey = oldBlockData.getMaterial().getBlockTranslationKey();

                    var breakAction = new PaperBlockAction(
                        PaperActionTypeRegistry.WORLDEDIT_BREAK,
                        oldBlockData,
                        oldTranslationKey,
                        null,
                        null
                    );

                    var breakActivity = PaperActivity.builder()
                        .action(breakAction)
                        .location(location)
                        .cause(cause)
                        .build();

                    recordingService.addToQueue(breakActivity);
                }

                // Log block placement
                if (!newIsAir && logPlace) {
                    BlockData newBlockData = BukkitAdapter.adapt(newState);
                    String newTranslationKey = newBlockData.getMaterial().getBlockTranslationKey();

                    BlockData oldBlockData = null;
                    String oldTranslationKey = null;
                    if (!oldIsAir) {
                        oldBlockData = BukkitAdapter.adapt(oldState);
                        oldTranslationKey = oldBlockData.getMaterial().getBlockTranslationKey();
                    }

                    var placeAction = new PaperBlockAction(
                        PaperActionTypeRegistry.WORLDEDIT_PLACE,
                        newBlockData,
                        newTranslationKey,
                        oldBlockData,
                        oldTranslationKey
                    );

                    var placeActivity = PaperActivity.builder()
                        .action(placeAction)
                        .location(location)
                        .cause(cause)
                        .build();

                    recordingService.addToQueue(placeActivity);
                }
            }
        }

        return set;
    }

    @Override
    public Extent construct(Extent child) {
        // Return child unchanged - we don't need to wrap as an extent
        return child;
    }

    @Override
    public ProcessorScope getScope() {
        // Run after all block modifications, just reading/logging
        return ProcessorScope.READING_BLOCKS;
    }
}
