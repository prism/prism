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

package network.darkhelmet.prism.bukkit.listeners.player;

import com.google.inject.Inject;

import network.darkhelmet.prism.bukkit.actions.BukkitBlockAction;
import network.darkhelmet.prism.bukkit.actions.BukkitItemStackAction;
import network.darkhelmet.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import network.darkhelmet.prism.bukkit.api.activities.BukkitActivity;
import network.darkhelmet.prism.bukkit.listeners.AbstractListener;
import network.darkhelmet.prism.bukkit.services.expectations.ExpectationService;
import network.darkhelmet.prism.bukkit.services.recording.BukkitRecordingService;
import network.darkhelmet.prism.bukkit.utils.BlockUtils;
import network.darkhelmet.prism.bukkit.utils.TagLib;
import network.darkhelmet.prism.loader.services.configuration.ConfigurationService;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerBucketEmptyListener extends AbstractListener implements Listener {
    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PlayerBucketEmptyListener(
            ConfigurationService configurationService,
            ExpectationService expectationService,
            BukkitRecordingService recordingService) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens to player bucket empty events.
     *
     * <p>This event never fires for powdered snow or milk buckets.</p>
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().bucketEmpty()) {
            return;
        }

        var action = new BukkitItemStackAction(
            BukkitActionTypeRegistry.BUCKET_EMPTY, new ItemStack(event.getBucket()));

        var bucketEmptyActivity = BukkitActivity.builder()
            .action(action)
            .location(event.getBlock().getLocation())
            .player(event.getPlayer())
            .build();

        recordingService.addToQueue(bucketEmptyActivity);

        Material newMaterial = BlockUtils.blockMaterialFromBucket(event.getBucket());
        if (newMaterial == null) {
            return;
        }

        // Stop if emptying bucket into existing, matching material (water into water, lava into lava)
        if (event.getBlock().getType().equals(newMaterial)) {
            return;
        }

        // Stop if the bucket is water and the target is a "water-like" block
        if (event.getBucket().equals(Material.WATER_BUCKET)
                && TagLib.WATER_BLOCKS.isTagged(event.getBlockClicked().getType())) {
            return;
        }

        BlockData oldData = event.getBlock().getBlockData();
        BlockData newData;
        if (event.getBlockClicked().getBlockData() instanceof Waterlogged waterlogged) {
            oldData = waterlogged;
            newData = oldData.clone();

            // Stop if the block is water logged
            if (waterlogged.isWaterlogged()) {
                return;
            }

            // If the block can be waterlogged, it isn't yet, so we need to fake it
            waterlogged.setWaterlogged(true);
        } else {
            newData = Bukkit.createBlockData(newMaterial);
        }

        var blockPlaceAction = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_PLACE, newData, oldData);

        var activity = BukkitActivity.builder()
            .action(blockPlaceAction)
            .location(event.getBlock().getLocation())
            .player(event.getPlayer())
            .build();

        recordingService.addToQueue(activity);
    }
}
