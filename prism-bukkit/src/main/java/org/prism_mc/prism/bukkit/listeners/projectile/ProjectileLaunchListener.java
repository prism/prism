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

package org.prism_mc.prism.bukkit.listeners.projectile;

import com.google.inject.Inject;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.bukkit.actions.BukkitItemStackAction;
import org.prism_mc.prism.bukkit.actions.GenericBukkitAction;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivity;
import org.prism_mc.prism.bukkit.listeners.AbstractListener;
import org.prism_mc.prism.bukkit.services.expectations.ExpectationService;
import org.prism_mc.prism.bukkit.services.recording.BukkitRecordingService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class ProjectileLaunchListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public ProjectileLaunchListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        BukkitRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * On item throw.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileLaunch(final ProjectileLaunchEvent event) {
        // Ignore if this event is disabled
        if (!configurationService.prismConfig().actions().itemThrow()) {
            return;
        }

        if (event.getEntity() instanceof Arrow) {
            return;
        }

        Action action = null;
        if (event.getEntity() instanceof ThrowableProjectile throwableProjectile) {
            action = new BukkitItemStackAction(BukkitActionTypeRegistry.ITEM_THROW, throwableProjectile.getItem());
        } else if (event.getEntity() instanceof Firework) {
            action = new GenericBukkitAction(
                BukkitActionTypeRegistry.FIREWORK_LAUNCH,
                nameFromCause(event.getEntity())
            );
        } else {
            action = new GenericBukkitAction(BukkitActionTypeRegistry.ITEM_THROW, nameFromCause(event.getEntity()));
        }

        var builder = BukkitActivity.builder().action(action).location(event.getLocation());

        if (event.getEntity().getShooter() instanceof Player player) {
            builder.player(player);
        } else {
            builder.cause(nameFromCause(event.getEntity().getShooter()));
        }

        recordingService.addToQueue(builder.build());
    }
}
