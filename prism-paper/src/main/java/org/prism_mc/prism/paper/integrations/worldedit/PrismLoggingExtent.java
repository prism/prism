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

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
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
 * An extent that logs block changes made through regular WorldEdit to Prism.
 *
 * <p>This extent intercepts all block changes made via WorldEdit operations
 * (//set, //cut, //paste, //replace, //undo, //redo, etc.) and logs them
 * as worldedit-break and worldedit-place activities.</p>
 *
 * <p>Note: This extent is only used for regular WorldEdit. For FastAsyncWorldEdit (FAWE),
 * see {@link PrismBlockChangeProcessor} which uses FAWE's batch processor API.</p>
 */
public class PrismLoggingExtent extends AbstractDelegateExtent {

    /**
     * The Bukkit player performing the WorldEdit operation.
     */
    private final Player bukkitPlayer;

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
     * Construct a PrismLoggingExtent.
     *
     * @param extent The extent to wrap
     * @param bukkitPlayer The Bukkit player performing the operation (may be null)
     * @param bukkitWorld The Bukkit world
     * @param recordingService The recording service
     * @param configurationService The configuration service
     */
    public PrismLoggingExtent(
        Extent extent,
        Player bukkitPlayer,
        World bukkitWorld,
        PaperRecordingService recordingService,
        ConfigurationService configurationService
    ) {
        super(extent);
        this.bukkitPlayer = bukkitPlayer;
        this.bukkitWorld = bukkitWorld;
        this.recordingService = recordingService;
        this.configurationService = configurationService;
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
        // Get the old block state BEFORE making changes
        BlockState oldWEState = getBlock(position);

        // Perform the actual block change first
        boolean result = super.setBlock(position, block);

        // Only log if the change actually happened
        if (!result) {
            return false;
        }

        // Determine the cause for logging
        Object cause = bukkitPlayer != null ? bukkitPlayer : "WorldEdit";

        // Determine what type of change this is using WorldEdit's block types
        boolean oldBlockIsAir = oldWEState.getBlockType().getMaterial().isAir();
        boolean newBlockIsAir = block.getBlockType().getMaterial().isAir();

        // Check if logging is enabled for these action types
        boolean logBreak = configurationService.prismConfig().actions().worldeditBreak();
        boolean logPlace = configurationService.prismConfig().actions().worldeditPlace();

        Location location = new Location(bukkitWorld, position.x(), position.y(), position.z());

        // Log block removal (break) only if replacing with air (actual removal)
        if (!oldBlockIsAir && newBlockIsAir && logBreak) {
            BlockData oldBlockData = BukkitAdapter.adapt(oldWEState);
            String oldTranslationKey = oldBlockData.getMaterial().getBlockTranslationKey();

            var breakAction = new PaperBlockAction(
                PaperActionTypeRegistry.WORLDEDIT_BREAK,
                oldBlockData,
                oldTranslationKey,
                null,
                null
            );

            var breakActivity = PaperActivity.builder().action(breakAction).location(location).cause(cause).build();

            recordingService.addToQueue(breakActivity);
        }

        // Log block placement if new block is not air
        if (!newBlockIsAir && logPlace) {
            BlockData newBlockData = BukkitAdapter.adapt(block);
            String newTranslationKey = newBlockData.getMaterial().getBlockTranslationKey();

            // For the replaced state, use the old state if it wasn't air
            BlockData oldBlockData = null;
            String oldTranslationKey = null;
            if (!oldBlockIsAir) {
                oldBlockData = BukkitAdapter.adapt(oldWEState);
                oldTranslationKey = oldBlockData.getMaterial().getBlockTranslationKey();
            }

            var placeAction = new PaperBlockAction(
                PaperActionTypeRegistry.WORLDEDIT_PLACE,
                newBlockData,
                newTranslationKey,
                oldBlockData,
                oldTranslationKey
            );

            var placeActivity = PaperActivity.builder().action(placeAction).location(location).cause(cause).build();

            recordingService.addToQueue(placeActivity);
        }

        return true;
    }
}
