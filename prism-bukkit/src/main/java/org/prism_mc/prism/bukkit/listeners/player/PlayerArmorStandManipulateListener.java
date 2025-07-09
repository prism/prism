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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.bukkit.actions.BukkitItemStackAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.bukkit.utils.ItemUtils;
import org.prism_mc.prism.bukkit.utils.TagLib;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class PlayerArmorStandManipulateListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PlayerArmorStandManipulateListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for block harvest events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(final PlayerArmorStandManipulateEvent event) {
        Action action = null;
        if (event.getArmorStandItem().getType() == Material.AIR) {
            // Ignore if this event is disabled or the player is not holding a valid item
            if (
                !configurationService.prismConfig().actions().itemInsert() ||
                !ItemUtils.isValidItem(event.getPlayerItem()) ||
                !TagLib.ALL_ARMOR.isTagged(event.getPlayerItem().getType())
            ) {
                return;
            }

            action = new BukkitItemStackAction(BukkitActionTypeRegistry.ITEM_INSERT, event.getPlayerItem());
        } else {
            // Ignore if this event is disabled or the player is holding an item
            if (
                !configurationService.prismConfig().actions().itemRemove() ||
                !ItemUtils.nullOrAir(event.getPlayerItem()) ||
                !ItemUtils.isValidItem(event.getArmorStandItem())
            ) {
                return;
            }

            action = new BukkitItemStackAction(BukkitActionTypeRegistry.ITEM_REMOVE, event.getArmorStandItem());
        }

        var activity = BukkitActivity.builder()
            .action(action)
            .cause(event.getPlayer())
            .location(event.getRightClicked().getLocation())
            .build();

        recordingService.addToQueue(activity);
    }
}
