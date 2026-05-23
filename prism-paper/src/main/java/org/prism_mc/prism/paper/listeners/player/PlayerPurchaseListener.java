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
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;

public class PlayerPurchaseListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public PlayerPurchaseListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * On player trade with a villager or wandering trader.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTrade(final PlayerTradeEvent event) {
        var result = event.getTrade().getResult();
        if (!shouldRecordItem(configurationService.prismConfig().actions().itemTrade(), result)) {
            return;
        }

        var action = new PaperItemStackAction(PaperActionTypeRegistry.ITEM_TRADE, result);

        var activity = PaperActivity.builder()
            .action(action)
            .location(event.getVillager().getLocation())
            .cause(event.getPlayer())
            .build();

        recordingService.addToQueue(activity);
    }
}
