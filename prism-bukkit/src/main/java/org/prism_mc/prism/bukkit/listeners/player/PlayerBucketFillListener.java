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

package org.prism_mc.prism.bukkit.listeners.player;

import com.google.inject.Inject;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.prism_mc.prism.bukkit.actions.BukkitBlockAction;
import org.prism_mc.prism.bukkit.actions.BukkitItemStackAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.bukkit.utils.ItemUtils;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class PlayerBucketFillListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PlayerBucketFillListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens to player bucket fill events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
        // Ignore if this event is disabled
        if (
            !configurationService.prismConfig().actions().bucketFill() || !ItemUtils.isValidItem(event.getItemStack())
        ) {
            return;
        }

        var bucketEmptyAction = new BukkitItemStackAction(BukkitActionTypeRegistry.BUCKET_FILL, event.getItemStack());

        var bucketEmptyActivity = BukkitActivity.builder()
            .action(bucketEmptyAction)
            .location(event.getBlock().getLocation())
            .player(event.getPlayer())
            .build();

        recordingService.addToQueue(bucketEmptyActivity);

        // No block data
        if (event.getBlock().getType().equals(Material.AIR)) {
            return;
        }

        BlockData blockData = event.getBlock().getBlockData();
        if (event.getBlockClicked().getBlockData() instanceof Waterlogged waterlogged) {
            blockData = waterlogged;

            // Fake the waterlogged block now being dry
            waterlogged.setWaterlogged(false);
        }

        var blockPlaceAction = new BukkitBlockAction(
            BukkitActionTypeRegistry.BLOCK_BREAK,
            blockData,
            event.getBlock().translationKey(),
            null,
            null
        );

        var blockPlaceActivity = BukkitActivity.builder()
            .action(blockPlaceAction)
            .location(event.getBlock().getLocation())
            .player(event.getPlayer())
            .build();

        recordingService.addToQueue(blockPlaceActivity);
    }
}
