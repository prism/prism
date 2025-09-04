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

package org.prism_mc.prism.paper.listeners.vehicle;

import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperEntityAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class VehicleEnterListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public VehicleEnterListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Listens for vehicle enter events.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(final VehicleEnterEvent event) {
        Vehicle vehicle = event.getVehicle();

        var actionType = PaperActionTypeRegistry.VEHICLE_RIDE;
        if (vehicle instanceof Tameable) {
            actionType = PaperActionTypeRegistry.ENTITY_RIDE;
        }

        // Ignore if this event is disabled
        if (
            (actionType.equals(PaperActionTypeRegistry.VEHICLE_RIDE) &&
                !configurationService.prismConfig().actions().vehicleRide()) ||
            (actionType.equals(PaperActionTypeRegistry.ENTITY_RIDE) &&
                !configurationService.prismConfig().actions().entityRide())
        ) {
            return;
        }

        Entity entity = event.getEntered();

        var action = new PaperEntityAction(actionType, vehicle);
        var builder = PaperActivity.builder().action(action).location(vehicle.getLocation()).cause(entity);
        recordingService.addToQueue(builder.build());
    }
}
