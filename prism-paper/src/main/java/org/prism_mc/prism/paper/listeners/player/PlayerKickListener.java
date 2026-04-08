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

package org.prism_mc.prism.paper.listeners.player;

import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.modifications.AutoRollbackService;

public class PlayerKickListener implements Listener {

    /**
     * The auto rollback service.
     */
    private final AutoRollbackService autoRollbackService;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * Construct the listener.
     *
     * @param autoRollbackService The auto rollback service
     * @param configurationService The configuration service
     */
    @Inject
    public PlayerKickListener(AutoRollbackService autoRollbackService, ConfigurationService configurationService) {
        this.autoRollbackService = autoRollbackService;
        this.configurationService = configurationService;
    }

    /**
     * Listen for player kicks to detect bans and trigger auto-rollback.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(final PlayerKickEvent event) {
        if (!configurationService.prismConfig().autoRollback().enabled()) {
            return;
        }

        if (event.getCause() != PlayerKickEvent.Cause.BANNED) {
            return;
        }

        final Player player = event.getPlayer();

        autoRollbackService.rollbackPlayer(player.getName());
    }
}
