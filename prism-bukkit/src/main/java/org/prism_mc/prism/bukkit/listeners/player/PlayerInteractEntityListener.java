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
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.prism_mc.prism.bukkit.actions.BukkitItemStackAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.bukkit.services.wands.WandService;
import org.prism_mc.prism.bukkit.utils.ItemUtils;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class PlayerInteractEntityListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PlayerInteractEntityListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService,
        WandService wandService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Event listener.
     *
     * @param event Tne event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        var heldItem = player.getInventory().getItemInMainHand();

        if (event.getRightClicked() instanceof ItemFrame itemFrame) {
            if (ItemUtils.nullOrAir(itemFrame.getItem()) && !ItemUtils.nullOrAir(heldItem)) {
                recordItemInsertActivity(event.getRightClicked().getLocation(), player, heldItem, 1);
            } else if (!ItemUtils.nullOrAir(itemFrame.getItem())) {
                if (
                    !configurationService.prismConfig().actions().itemRotate() ||
                    !ItemUtils.isValidItem(itemFrame.getItem())
                ) {
                    return;
                }

                var action = new BukkitItemStackAction(BukkitActionTypeRegistry.ITEM_ROTATE, itemFrame.getItem());

                var activity = BukkitActivity.builder()
                    .action(action)
                    .player(player)
                    .location(event.getRightClicked().getLocation())
                    .build();

                recordingService.addToQueue(activity);
            }
        }
    }
}
