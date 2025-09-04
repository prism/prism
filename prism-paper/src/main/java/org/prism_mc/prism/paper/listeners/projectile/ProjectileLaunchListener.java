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

package org.prism_mc.prism.paper.listeners.projectile;

import static org.prism_mc.prism.paper.api.activities.PaperActivity.enumNameToString;

import com.google.inject.Inject;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.GenericPaperAction;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

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
        PaperRecordingService recordingService
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
        if (event.getEntity() instanceof Arrow) {
            return;
        }

        Action action = null;
        String descriptor = enumNameToString(event.getEntity().getType().name());
        if (event.getEntity() instanceof ThrowableProjectile throwableProjectile) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().itemThrow()) {
                return;
            }

            action = new PaperItemStackAction(PaperActionTypeRegistry.ITEM_THROW, throwableProjectile.getItem());
        } else if (event.getEntity() instanceof Firework) {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().fireworkLaunch()) {
                return;
            }

            action = new GenericPaperAction(PaperActionTypeRegistry.FIREWORK_LAUNCH, descriptor);
        } else {
            // Ignore if this event is disabled
            if (!configurationService.prismConfig().actions().itemThrow()) {
                return;
            }

            action = new GenericPaperAction(PaperActionTypeRegistry.ITEM_THROW, descriptor);
        }

        var builder = PaperActivity.builder()
            .action(action)
            .location(event.getLocation())
            .cause(event.getEntity().getShooter());

        recordingService.addToQueue(builder.build());
    }
}
