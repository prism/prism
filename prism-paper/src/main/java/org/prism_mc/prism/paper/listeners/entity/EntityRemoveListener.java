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

package org.prism_mc.prism.paper.listeners.entity;

import com.google.inject.Inject;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.listeners.AbstractListener;
import org.prism_mc.prism.paper.services.airtags.Airtags;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.utils.ItemUtils;

public class EntityRemoveListener extends AbstractListener implements Listener {

    /**
     * Construct the listener.
     *
     * @param configurationService The configuration service
     * @param expectationService The expectation service
     * @param recordingService The recording service
     */
    @Inject
    public EntityRemoveListener(
        ConfigurationService configurationService,
        ExpectationService expectationService,
        PaperRecordingService recordingService
    ) {
        super(configurationService, expectationService, recordingService);
    }

    /**
     * Entity remove event listener.
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(final EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }

        ItemStack itemStack = item.getItemStack();
        if (!ItemUtils.isValidItem(itemStack)) {
            return;
        }

        ActionType actionType;

        switch (event.getCause()) {
            case DESPAWN -> actionType = PaperActionTypeRegistry.ITEM_DESPAWN;
            case OUT_OF_WORLD, DEATH, EXPLODE -> actionType = PaperActionTypeRegistry.ITEM_DESTROY;
            default -> {
                return;
            }
        }

        if (!Airtags.has(itemStack)) {
            return;
        }

        // Resolve the cause label only once we know the activity will be recorded.
        String causeLabel =
            switch (event.getCause()) {
                case DESPAWN -> "despawn";
                case OUT_OF_WORLD -> "void";
                case DEATH -> labelFromLastDamage(item);
                case EXPLODE -> "explosion";
                default -> "damage";
            };

        var action = new PaperItemStackAction(actionType, itemStack);
        var activity = PaperActivity.builder().action(action).location(item.getLocation()).cause(causeLabel).build();
        recordingService.addToQueue(activity);
    }

    /**
     * Convert the item's last damage cause into a short label for the activity cause.
     *
     * @param item The item entity
     * @return The cause label
     */
    private String labelFromLastDamage(Item item) {
        EntityDamageEvent last = item.getLastDamageCause();
        if (last == null) {
            return "damage";
        }

        return switch (last.getCause()) {
            case LAVA -> "lava";
            case FIRE, FIRE_TICK -> "fire";
            case CONTACT -> "cactus";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "explosion";
            default -> "damage";
        };
    }
}
