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
import dev.triumphteam.cmd.core.annotations.CommandFlags;
import dev.triumphteam.cmd.core.annotations.NamedArguments;
import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.services.lookup.LookupService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.query.QueryService;
import org.prism_mc.prism.paper.services.translation.PaperTranslationService;

@Command(value = "prism", alias = { "pr" })
public class VaultCommand {

    /**
     * The action registry.
     */
    private final PaperActionTypeRegistry actionRegistry;

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
     * The query service.
     */
    private final QueryService queryService;

    /**
     * Keys reversed while the gui is open.
     */
    private final List<Long> reversedKeys = new ArrayList<>();

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The task chain provider.
     */
    private final TaskChainProvider taskChainProvider;

    /**
     * The translation service.
     */
    private final PaperTranslationService translationService;

    /**
     * Construct the near command.
     *
     * @param actionRegistry The action registry
     * @param loggingService The logging service
     * @param lookupService The lookup service
     * @param messageService The message service
     * @param queryService The query service
     * @param storageAdapter The storage adapter
     * @param taskChainProvider The task chain provider
     * @param translationService The translation service
     */
    @Inject
    public VaultCommand(
        PaperActionTypeRegistry actionRegistry,
        LoggingService loggingService,
        LookupService lookupService,
        MessageService messageService,
        QueryService queryService,
        StorageAdapter storageAdapter,
        TaskChainProvider taskChainProvider,
        PaperTranslationService translationService
    ) {
        this.actionRegistry = actionRegistry;
        this.loggingService = loggingService;
        this.lookupService = lookupService;
        this.messageService = messageService;
        this.queryService = queryService;
        this.storageAdapter = storageAdapter;
        this.taskChainProvider = taskChainProvider;
        this.translationService = translationService;
    }

    /**
     * Open a virtual inventory of items.
     *
     * @param player The player
     * @param arguments The arguments
     */
    @CommandFlags(key = "query-flags")
    @NamedArguments("query-parameters")
    @Command(value = "vault", alias = "v")
    @Permission("prism.modification")
    public void onVault(final Player player, final Arguments arguments) {
        var optionalBuilder = queryService.queryFromArguments(player, arguments);
        optionalBuilder.ifPresent(builder -> {
            // If not action parameter, use our defaults
            if (arguments.getListArgument("a", String.class).isEmpty()) {
                builder.actionTypes(
                    List.of(
                        PaperActionTypeRegistry.ITEM_DISPENSE,
                        PaperActionTypeRegistry.ITEM_REMOVE,
                        PaperActionTypeRegistry.ITEM_ROTATE,
                        PaperActionTypeRegistry.ITEM_DROP,
                        PaperActionTypeRegistry.ITEM_INSERT,
                        PaperActionTypeRegistry.ITEM_PICKUP,
                        PaperActionTypeRegistry.ITEM_THROW
                    )
                );
            } else {
                // Validate that only item actions were provided
                for (var actionKey : arguments.getListArgument("a", String.class).get()) {
                    if (actionKey.contains("-") && !actionKey.startsWith("item-")) {
                        messageService.errorNonItemAction(player);

                        return;
                    } else {
                        for (var actionType : actionRegistry.actionTypesInFamily(
                            actionKey.toLowerCase(Locale.ENGLISH)
                        )) {
                            if (!actionType.key().startsWith("item-")) {
                                messageService.errorNonItemAction(player);

                                return;
                            }
                        }
                    }
                }
            }

            var query = builder.lookup(false).grouped(false).build();
            lookupService.lookup(player, query, results -> {
                if (results.isEmpty()) {
                    messageService.noResults(player);
                } else {
                    messageService.vaultHeader(player, results.size());

                    if (!query.defaultsUsed().isEmpty()) {
                        messageService.defaultsUsed(player, String.join(" ", query.defaultsUsed()));
                    }

                    var title = MiniMessage.miniMessage()
                        .deserialize(translationService.messageOf(player, "prism.vault-gui-title"));

                    var next = MiniMessage.miniMessage()
                        .deserialize(translationService.messageOf(player, "prism.vault-gui-next"));

                    var prev = MiniMessage.miniMessage()
                        .deserialize(translationService.messageOf(player, "prism.vault-gui-previous"));

                    Bukkit.getServer()
                        .getScheduler()
                        .runTask(PrismPaper.instance().loaderPlugin(), () -> {
                            var gui = Gui.paginated()
                                .title(title)
                                .disableAllInteractions()
                                .enableItemTake()
                                .rows(6)
                                .create();

                            for (var activity : results) {
                                if (activity.action() instanceof PaperItemStackAction itemStackAction) {
                                    gui.addItem(
                                        new GuiItem(itemStackAction.itemStack(), event -> {
                                            reversedKeys.add((Long) activity.primaryKey());
                                            loggingService.info(
                                                "{0} took {1} for activity #{2} from the vault inventory",
                                                player.getName(),
                                                itemStackAction
                                                    .itemStack()
                                                    .getType()
                                                    .toString()
                                                    .toLowerCase(Locale.ENGLISH),
                                                activity.primaryKey()
                                            );
                                        })
                                    );
                                }
                            }

                            for (var column = 1; column <= 9; column++) {
                                gui.setItem(
                                    6,
                                    column,
                                    new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> {
                                        event.setCancelled(true);
                                    })
                                );
                            }

                            updateGuiButtons(gui, prev, next);

                            gui.open(player);

                            gui.setCloseGuiAction(event -> {
                                var keys = new ArrayList<>(reversedKeys);
                                taskChainProvider
                                    .newChain()
                                    .async(() -> {
                                        storageAdapter.markReversed(keys, true);
                                    })
                                    .execute();

                                reversedKeys.clear();
                            });
                        });
                }
            });
        });
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
                new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> {
                    event.setCancelled(true);
                })
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
                new GuiItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), event -> {
                    event.setCancelled(true);
                })
            );
        }

        gui.update();
    }
}
