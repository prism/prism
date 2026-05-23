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

package org.prism_mc.prism.paper.commands;

import com.google.inject.Inject;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.Command;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.services.airtags.AirtagIdGenerator;
import org.prism_mc.prism.paper.services.airtags.AirtagService;
import org.prism_mc.prism.paper.services.airtags.Airtags;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

@Command(value = "prism", alias = { "pr" })
public class AirtagCommand {

    /**
     * Number of attempts to find a free airtag id before giving up.
     */
    private static final int GENERATION_ATTEMPTS = 10;

    /**
     * The airtag service.
     */
    private final AirtagService airtagService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

    /**
     * The recording service.
     */
    private final PaperRecordingService recordingService;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * Construct the airtag command.
     *
     * @param airtagService The airtag service
     * @param loggingService The logging service
     * @param messageService The message service
     * @param prismScheduler The scheduler
     * @param recordingService The recording service
     * @param storageAdapter The storage adapter
     */
    @Inject
    public AirtagCommand(
        AirtagService airtagService,
        LoggingService loggingService,
        MessageService messageService,
        PrismScheduler prismScheduler,
        PaperRecordingService recordingService,
        StorageAdapter storageAdapter
    ) {
        this.airtagService = airtagService;
        this.loggingService = loggingService;
        this.messageService = messageService;
        this.prismScheduler = prismScheduler;
        this.recordingService = recordingService;
        this.storageAdapter = storageAdapter;
    }

    /**
     * Attach an airtag to the held item.
     *
     * @param player The player
     */
    @Command("airtag")
    @Permission("prism.airtag")
    public void onAirtag(final Player player) {
        if (!validateAirtaggable(player, player.getInventory().getItemInMainHand())) {
            return;
        }

        final int limit = airtagService.airtagLimit(player);
        if (limit == 0) {
            messageService.errorAirtagLimitReached(player, 0);
            return;
        }

        prismScheduler.runAsync(() -> {
            try {
                if (limit != AirtagService.UNLIMITED && !airtagService.withinLimit(limit, countAirtags(player))) {
                    prismScheduler.runForEntity(player, () -> messageService.errorAirtagLimitReached(player, limit));
                    return;
                }

                String airtag = generateUniqueAirtag();
                if (airtag == null) {
                    prismScheduler.runForEntity(player, () -> messageService.errorAirtagGenerationFailed(player));
                    return;
                }

                storageAdapter.createAirtag(airtag, player.getUniqueId(), player.getName());

                prismScheduler.runForEntity(player, () -> applyAirtag(player, airtag));
            } catch (Exception ex) {
                loggingService.handleException(ex);
                prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(player));
            }
        });
    }

    /**
     * Count the airtags a player currently owns.
     *
     * @param player The player
     * @return The number of airtags owned
     * @throws Exception Storage layer exception
     */
    private int countAirtags(Player player) throws Exception {
        return storageAdapter.countAirtagsForPlayer(player.getUniqueId());
    }

    /**
     * Validate that the held item is eligible to be airtagged, messaging the player on failure.
     *
     * @param player The player
     * @param itemStack The held item
     * @return True if the item may be airtagged
     */
    private boolean validateAirtaggable(Player player, ItemStack itemStack) {
        if (itemStack.getType() == Material.AIR) {
            messageService.errorNoItemInHand(player);
            return false;
        }

        if (!airtagService.isAirtaggable(itemStack)) {
            messageService.errorItemNotAirtaggable(player);
            return false;
        }

        if (Airtags.has(itemStack)) {
            messageService.itemAlreadyAirtagged(player, Airtags.get(itemStack));
            return false;
        }

        return true;
    }

    /**
     * Generate an airtag id that isn't already present in storage.
     *
     * @return A unique id, or null if a free id couldn't be found within the attempt budget
     * @throws Exception Storage layer exception
     */
    private String generateUniqueAirtag() throws Exception {
        for (int attempt = 0; attempt < GENERATION_ATTEMPTS; attempt++) {
            String candidate = AirtagIdGenerator.generate();
            if (!storageAdapter.airtagExists(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Apply the reserved airtag to the held item and record the action, on the main thread.
     *
     * <p>The held item is re-validated because it may have changed during the asynchronous id
     * reservation.</p>
     *
     * @param player The player
     * @param airtag The reserved airtag id
     */
    private void applyAirtag(Player player, String airtag) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (!validateAirtaggable(player, itemStack)) {
            // The reservation is no longer going to be used; release it so it doesn't consume a slot.
            prismScheduler.runAsync(() -> {
                try {
                    storageAdapter.deleteAirtag(airtag, player.getUniqueId());
                } catch (Exception ex) {
                    loggingService.handleException(ex);
                }
            });

            return;
        }

        var meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(Airtags.KEY, PersistentDataType.STRING, airtag);
        itemStack.setItemMeta(meta);

        messageService.itemAirtagged(player, airtag);

        var action = new PaperItemStackAction(PaperActionTypeRegistry.ITEM_AIRTAG, itemStack);
        var activity = PaperActivity.builder().action(action).location(player.getLocation()).cause(player).build();
        recordingService.addToQueue(activity);
    }
}
