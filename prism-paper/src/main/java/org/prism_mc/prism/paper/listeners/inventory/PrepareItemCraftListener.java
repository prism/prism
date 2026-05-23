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

package org.prism_mc.prism.paper.listeners.inventory;

import com.google.inject.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.airtags.Airtags;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class PrepareItemCraftListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PrepareItemCraftListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Strip the airtag PDC from any crafted result.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(final PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !Airtags.has(result)) {
            return;
        }

        var meta = result.getItemMeta();
        meta.getPersistentDataContainer().remove(Airtags.KEY);
        result.setItemMeta(meta);

        event.getInventory().setResult(result);
    }
}
