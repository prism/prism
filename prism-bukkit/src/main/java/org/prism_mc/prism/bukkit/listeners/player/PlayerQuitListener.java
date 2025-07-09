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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.bukkit.actions.GenericBukkitAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.bukkit.services.wands.WandService;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class PlayerQuitListener extends AbstractListener implements Listener {

    /**
     * The wand service.
     */
    private final WandService wandService;

    /**
     * The modification queue service.
     */
    private final ModificationQueueService modificationQueueService;

    /**
     * The cache service.
     */
    private final CacheService cacheService;

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     * @param wandService The wand service
     * @param modificationQueueService The modification queue service
     */
    @Inject
    public PlayerQuitListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService,
        WandService wandService,
        ModificationQueueService modificationQueueService,
        CacheService cacheService
    ) {
        super(configurationService, expectationService, recordingService);
        this.wandService = wandService;
        this.modificationQueueService = modificationQueueService;
        this.cacheService = cacheService;
    }

    /**
     * On player quit.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        // Cancel any modification queues for this player
        modificationQueueService.clearEverythingForOwner(player);

        // Deactivate any wands
        wandService.deactivateWand(player);

        if (configurationService.prismConfig().actions().playerQuit()) {
            var action = new GenericBukkitAction(BukkitActionTypeRegistry.PLAYER_QUIT);

            var activity = BukkitActivity.builder().action(action).location(player.getLocation()).cause(player).build();

            recordingService.addToQueue(activity);
        }

        // Remove cached player data
        Long playerPk = cacheService.playerUuidPkMap().getIfPresent(event.getPlayer().getUniqueId());
        if (playerPk != null) {
            // Remove the player's UUID -> PK from the cache
            cacheService.playerUuidPkMap().invalidate(event.getPlayer().getUniqueId());
        }
    }
}
