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

package org.prism_mc.prism.bukkit.listeners.entity;

import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.bukkit.actions.BukkitBlockAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class EntityChangeBlockListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public EntityChangeBlockListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for entity change block events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        // The event seems to fire for certain blocks changing state, which is
        // useless when we're only given the material it's changing to.
        // Ex: DEEPSLATE_REDSTONE_ORE changes to DEEPSLATE_REDSTONE_ORE because of SKELETON
        if (event.getBlock().getType().equals(event.getTo())) {
            return;
        }

        // Ignore players, because those should be recorded as a block place event
        if (event.getEntity() instanceof Player) {
            return;
        }

        BlockState oldState = event.getBlock().getState();
        BlockState newState = event.getBlock().getState();
        newState.setType(event.getTo());

        // If the entity is a sheep, it's eating
        if (event.getEntityType().equals(EntityType.SHEEP)) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().entityEat()) {
                return;
            }

            // The event.getTo method return AIR for some insane reason. Since we know sheep can only
            // turn grass into dirt, we'll just fake the "new" material.
            var action = new BukkitBlockAction(
                BukkitActionTypeRegistry.ENTITY_EAT,
                event.getBlock().getBlockData(),
                event.getBlock().translationKey(),
                Bukkit.createBlockData(Material.DIRT),
                Material.DIRT.translationKey()
            );

            var activity = BukkitActivity.builder()
                .action(action)
                .location(event.getBlock().getLocation())
                .cause(event.getEntity())
                .build();

            recordingService.addToQueue(activity);

            return;
        }

        // This will handle:
        // - gravity breaking/placing blocks that fall
        // - enderman griefing
        // - etc
        Action action;
        if (event.getTo().equals(Material.AIR)) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().blockBreak()) {
                return;
            }

            action = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_BREAK, oldState);
        } else {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().blockPlace()) {
                return;
            }

            action = new BukkitBlockAction(BukkitActionTypeRegistry.BLOCK_PLACE, newState);
        }

        var activity = BukkitActivity.builder()
            .action(action)
            .location(event.getBlock().getLocation())
            .cause(event.getEntity())
            .build();

        recordingService.addToQueue(activity);
    }
}
