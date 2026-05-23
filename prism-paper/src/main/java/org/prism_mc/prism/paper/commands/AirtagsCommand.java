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
import de.tr7zw.nbtapi.NBT;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.Command;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.airtags.AirtagSummary;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivity;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.services.airtags.Airtags;
import org.prism_mc.prism.paper.services.lookup.LookupService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;
import org.prism_mc.prism.paper.services.translation.PaperTranslationService;

@Command(value = "prism", alias = { "pr" })
public class AirtagsCommand {

    /**
     * Airtags query limit.
     */
    private static final int AIRTAGS_QUERY_LIMIT = 180;

    /**
     * Lore date format.
     */
    private static final DateTimeFormatter LORE_DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm"
    ).withZone(ZoneId.systemDefault());

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The lookup service.
     */
    private final LookupService lookupService;

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
     * The translation service.
     */
    private final PaperTranslationService translationService;

    /**
     * Construct the airtags command.
     *
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @param lookupService The lookup service
     * @param messageService The message service
     * @param prismScheduler The scheduler
     * @param recordingService The recording service
     * @param storageAdapter The storage adapter
     * @param translationService The translation service
     */
    @Inject
    public AirtagsCommand(
        ConfigurationService configurationService,
        LoggingService loggingService,
        LookupService lookupService,
        MessageService messageService,
        PrismScheduler prismScheduler,
        PaperRecordingService recordingService,
        StorageAdapter storageAdapter,
        PaperTranslationService translationService
    ) {
        this.configurationService = configurationService;
        this.loggingService = loggingService;
        this.lookupService = lookupService;
        this.messageService = messageService;
        this.prismScheduler = prismScheduler;
        this.recordingService = recordingService;
        this.storageAdapter = storageAdapter;
        this.translationService = translationService;
    }

    @Command("airtags")
    public class AirtagsSubCommand {

        /**
         * Open a paginated GUI listing the player's own airtagged items.
         *
         * @param player The player invoking the command
         */
        @Permission("prism.airtags")
        @Command(Command.DEFAULT_CMD_NAME)
        public void onAirtags(final Player player) {
            showAirtags(player, null);
        }

        /**
         * Open a paginated GUI listing another player's airtagged items.
         *
         * @param player The player invoking the command
         * @param target The target player whose airtags to display
         */
        @Permission("prism.airtags")
        @Command("view")
        public void onView(final Player player, final OfflinePlayer target) {
            showAirtags(player, target);
        }

        /**
         * Remove the airtag from an item.
         *
         * @param player The player invoking the command
         */
        @Permission("prism.airtags")
        @Command("untag")
        public void onUntag(final Player player) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();

            if (itemStack.getType() == Material.AIR) {
                messageService.errorNoItemInHand(player);
                return;
            }

            String airtag = Airtags.get(itemStack);
            if (airtag == null) {
                messageService.errorItemNotAirtagged(player);
                return;
            }

            var action = new PaperItemStackAction(PaperActionTypeRegistry.ITEM_UNTAG, itemStack.clone());
            var activity = PaperActivity.builder().action(action).location(player.getLocation()).cause(player).build();
            recordingService.addToQueue(activity);

            var meta = itemStack.getItemMeta();
            meta.getPersistentDataContainer().remove(Airtags.KEY);
            itemStack.setItemMeta(meta);

            messageService.itemAirtagRemoved(player, airtag);

            var prompt = MiniMessage.miniMessage()
                .deserialize(
                    translationService.messageOf(player, "prism.airtag-delete-prompt"),
                    Placeholder.parsed("prefix", translationService.messageOf(player, "prism.prefix"))
                )
                .hoverEvent(
                    HoverEvent.hoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.text(translationService.messageOf(player, "prism.airtag-delete-prompt-hover"))
                    )
                )
                .clickEvent(ClickEvent.runCommand("/pr airtags delete " + airtag));

            player.sendMessage(prompt);
        }

        /**
         * Delete airtags from storage.
         *
         * @param player The player invoking the command
         * @param airtag The airtag id to delete
         */
        @Permission("prism.airtags")
        @Command("delete")
        public void onDelete(final Player player, final String airtag) {
            String normalized = airtag.toUpperCase(Locale.ROOT);
            UUID ownerFilter = player.hasPermission("prism.airtags.others") ? null : player.getUniqueId();

            prismScheduler.runAsync(() -> {
                int deleted;
                try {
                    deleted = storageAdapter.deleteAirtag(normalized, ownerFilter);
                } catch (Exception ex) {
                    loggingService.handleException(ex);
                    prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(player));
                    return;
                }

                prismScheduler.runForEntity(player, () -> {
                    if (deleted > 0) {
                        messageService.airtagDeleted(player, normalized);
                    } else {
                        messageService.errorAirtagNotFound(player);
                    }
                });
            });
        }

        /**
         * Open a vault of the airtagged items.
         *
         * @param player The player invoking the command
         */
        @Permission("prism.airtags.vault")
        @Command("vault")
        public void onVault(final Player player) {
            prismScheduler.runAsync(() -> {
                List<AirtagSummary> airtags;
                try {
                    airtags = storageAdapter.queryAirtagsForPlayer(player.getUniqueId(), AIRTAGS_QUERY_LIMIT);
                } catch (Exception ex) {
                    loggingService.handleException(ex);
                    prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(player));
                    return;
                }

                if (airtags.isEmpty()) {
                    prismScheduler.runForEntity(player, () -> messageService.noAirtagsFound(player));
                    return;
                }

                prismScheduler.runForEntity(player, () -> openVault(player, airtags));
            });
        }
    }

    /**
     * Open the airtags GUI.
     *
     * @param player The player invoking the command
     * @param target The target player whose airtags to display, or {@code null} for self
     */
    private void showAirtags(final Player player, final OfflinePlayer target) {
        OfflinePlayer effectiveTarget = target == null ? player : target;
        boolean self = effectiveTarget.getUniqueId().equals(player.getUniqueId());

        if (!self && !player.hasPermission("prism.airtags.others")) {
            messageService.errorInsufficientPermission(player);
            return;
        }

        String targetName = effectiveTarget.getName() != null
            ? effectiveTarget.getName()
            : effectiveTarget.getUniqueId().toString();

        prismScheduler.runAsync(() -> {
            List<AirtagSummary> airtags;
            try {
                airtags = storageAdapter.queryAirtagsForPlayer(effectiveTarget.getUniqueId(), AIRTAGS_QUERY_LIMIT);
            } catch (Exception ex) {
                loggingService.handleException(ex);
                prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(player));
                return;
            }

            if (airtags.isEmpty()) {
                prismScheduler.runForEntity(player, () -> messageService.noAirtagsFound(player));
                return;
            }

            prismScheduler.runForEntity(player, () -> openGui(player, targetName, self, airtags));
        });
    }

    /**
     * Open the vault GUI.
     *
     * @param player The player
     * @param airtags The airtag summaries to display
     */
    private void openVault(Player player, List<AirtagSummary> airtags) {
        var title = MiniMessage.miniMessage()
            .deserialize(translationService.messageOf(player, "prism.airtags-vault-gui-title"));
        var next = MiniMessage.miniMessage()
            .deserialize(translationService.messageOf(player, "prism.airtags-gui-next"));
        var prev = MiniMessage.miniMessage()
            .deserialize(translationService.messageOf(player, "prism.airtags-gui-previous"));

        var gui = Gui.paginated().title(title).disableAllInteractions().enableItemTake().rows(6).create();

        List<String> taken = new ArrayList<>();
        int displayed = 0;
        for (var airtag : airtags) {
            ItemStack item = buildVaultItem(airtag);
            if (item == null) {
                continue;
            }

            displayed++;
            String code = airtag.airtag();
            gui.addItem(
                new GuiItem(item, event -> {
                    // The take proceeds natively; record it so the airtag is consumed on close.
                    taken.add(code);
                    messageService.airtagsVaultReminder(player);
                    loggingService.info("{0} recovered airtagged item {1} from the vault", player.getName(), code);
                })
            );
        }

        if (displayed == 0) {
            messageService.noAirtagsFound(player);

            return;
        }

        messageService.airtagsVaultHeader(player, displayed);

        for (var column = 1; column <= 9; column++) {
            gui.setItem(
                6,
                column,
                new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> event.setCancelled(true))
            );
        }

        updateGuiButtons(gui, prev, next);

        gui.open(player);

        UUID owner = player.getUniqueId();
        gui.setCloseGuiAction(event -> {
            if (taken.isEmpty()) {
                return;
            }

            // Recovering an airtagged item consumes the airtag so it can't be recovered again.
            var codes = new ArrayList<>(taken);
            taken.clear();

            prismScheduler.runAsync(() -> {
                for (String code : codes) {
                    try {
                        storageAdapter.deleteAirtag(code, owner);
                    } catch (Exception ex) {
                        loggingService.handleException(ex);
                    }
                }
            });
        });
    }

    /**
     * Reconstruct an airtagged item at its latest version for recovery.
     *
     * @param airtag The airtag summary
     * @return The clean item, or {@code null} if reconstruction failed
     */
    private ItemStack buildVaultItem(AirtagSummary airtag) {
        ItemStack item;
        try {
            if (airtag.itemData() != null) {
                item = NBT.itemStackFromNBT(NBT.parseNBT(airtag.itemData()));
            } else if (airtag.itemMaterial() != null) {
                item = new ItemStack(Material.valueOf(airtag.itemMaterial()));
            } else {
                return null;
            }
        } catch (Exception ex) {
            loggingService.handleException(ex);
            return null;
        }

        if (item == null) {
            return null;
        }

        if (Airtags.has(item)) {
            var meta = item.getItemMeta();
            meta.getPersistentDataContainer().remove(Airtags.KEY);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Open the paginated GUI on the main thread.
     *
     * @param player The viewing player
     * @param targetName The target player's name (used in headers)
     * @param self Whether the target is the viewer
     * @param airtags The airtag summaries to display
     */
    private void openGui(Player player, String targetName, boolean self, List<AirtagSummary> airtags) {
        var title = MiniMessage.miniMessage()
            .deserialize(translationService.messageOf(player, "prism.airtags-gui-title"));
        var next = MiniMessage.miniMessage()
            .deserialize(translationService.messageOf(player, "prism.airtags-gui-next"));
        var prev = MiniMessage.miniMessage()
            .deserialize(translationService.messageOf(player, "prism.airtags-gui-previous"));

        var gui = Gui.paginated().title(title).disableAllInteractions().rows(6).create();

        int displayed = 0;
        for (var airtag : airtags) {
            ItemStack displayItem = buildDisplayItem(player, airtag);
            if (displayItem == null) {
                continue;
            }

            displayed++;
            gui.addItem(
                new GuiItem(displayItem, event -> {
                    event.setCancelled(true);
                    openHistoryLookup(player, airtag.airtag());
                })
            );
        }

        if (displayed == 0) {
            messageService.noAirtagsFound(player);

            return;
        }

        if (self) {
            messageService.airtagsHeader(player, displayed);
        } else {
            messageService.airtagsHeaderOther(player, targetName, displayed);
        }

        for (var column = 1; column <= 9; column++) {
            gui.setItem(
                6,
                column,
                new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> event.setCancelled(true))
            );
        }

        updateGuiButtons(gui, prev, next);

        gui.open(player);
    }

    /**
     * Close the inventory and run a paginated history lookup for the given airtag.
     *
     * @param player The viewing player
     * @param airtag The airtag ID
     */
    private void openHistoryLookup(Player player, String airtag) {
        player.closeInventory();

        final ActivityQuery query = PaperActivityQuery.builder()
            .airtag(airtag)
            .limit(configurationService.prismConfig().defaults().perPage())
            .build();

        lookupService.lookup(player, query);
    }

    /**
     * Reconstruct the airtagged item and attach airtag-id / creation-time lore.
     *
     * @param player The viewing player, for translation context
     * @param airtag The airtag summary
     * @return The display item, or {@code null} if reconstruction failed
     */
    private ItemStack buildDisplayItem(Player player, AirtagSummary airtag) {
        ItemStack displayItem;
        try {
            if (airtag.itemData() != null) {
                displayItem = NBT.itemStackFromNBT(NBT.parseNBT(airtag.itemData()));
            } else if (airtag.itemMaterial() != null) {
                displayItem = new ItemStack(Material.valueOf(airtag.itemMaterial()));
            } else {
                return null;
            }
        } catch (Exception ex) {
            loggingService.handleException(ex);
            return null;
        }

        var meta = displayItem.getItemMeta();
        if (meta != null) {
            var airtagLore = MiniMessage.miniMessage()
                .deserialize(
                    translationService.messageOf(player, "prism.airtags-gui-lore-airtag"),
                    Placeholder.unparsed("airtag", airtag.airtag())
                );
            var taggedLore = MiniMessage.miniMessage()
                .deserialize(
                    translationService.messageOf(player, "prism.airtags-gui-lore-tagged"),
                    Placeholder.unparsed(
                        "date",
                        LORE_DATE_FORMATTER.format(Instant.ofEpochSecond(airtag.createdAtSeconds()))
                    )
                );

            meta.lore(List.of(airtagLore, taggedLore));
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    /**
     * Update the gui prev/next buttons.
     *
     * @param gui The GUI
     * @param prev The previous button component
     * @param next The next button component
     */
    protected void updateGuiButtons(PaginatedGui gui, Component prev, Component next) {
        if (gui.getCurrentPageNum() > 1) {
            var prevItem = new ItemStack(Material.PAPER);
            var prevItemMeta = prevItem.getItemMeta();
            prevItemMeta.displayName(prev);
            prevItem.setItemMeta(prevItemMeta);

            gui.setItem(
                6,
                3,
                new GuiItem(prevItem, event -> {
                    event.setCancelled(true);

                    gui.previous();

                    updateGuiButtons(gui, prev, next);
                })
            );
        } else {
            gui.setItem(
                6,
                3,
                new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> event.setCancelled(true))
            );
        }

        if (gui.getCurrentPageNum() < gui.getPagesNum()) {
            var nextItem = new ItemStack(Material.PAPER);
            var nextItemMeta = nextItem.getItemMeta();
            nextItemMeta.displayName(next);
            nextItem.setItemMeta(nextItemMeta);

            gui.setItem(
                6,
                7,
                new GuiItem(nextItem, event -> {
                    event.setCancelled(true);

                    gui.next();

                    updateGuiButtons(gui, prev, next);
                })
            );
        } else {
            gui.setItem(
                6,
                7,
                new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> event.setCancelled(true))
            );
        }

        gui.update();
    }
}
