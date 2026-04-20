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
import dev.triumphteam.cmd.core.annotations.NamedArguments;
import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.Previewable;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;
import org.prism_mc.prism.paper.services.modifications.PaperRestore;
import org.prism_mc.prism.paper.services.modifications.PaperRollback;
import org.prism_mc.prism.paper.services.query.QueryService;

@Command(value = "prism", alias = { "pr" })
public class PreviewCommand {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The modification queue service.
     */
    private final PaperModificationQueueService modificationQueueService;

    /**
     * The query service.
     */
    private final QueryService queryService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Construct the rollback command.
     *
     * @param configurationService The configuration service
     * @param storageAdapter The storage adapter
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param queryService The query service
     * @param loggingService The logging service
     */
    @Inject
    public PreviewCommand(
        ConfigurationService configurationService,
        StorageAdapter storageAdapter,
        MessageService messageService,
        PaperModificationQueueService modificationQueueService,
        QueryService queryService,
        LoggingService loggingService
    ) {
        this.configurationService = configurationService;
        this.storageAdapter = storageAdapter;
        this.messageService = messageService;
        this.modificationQueueService = modificationQueueService;
        this.queryService = queryService;
        this.loggingService = loggingService;
    }

    /**
     * Run the preview apply command.
     *
     * @param player The player
     */
    @Command(value = "preview-apply")
    @Permission("prism.modify")
    public void onApply(final Player player) {
        Optional<ModificationQueue> optionalQueue = modificationQueueService.currentQueueForOwner(player);
        if (optionalQueue.isEmpty()) {
            messageService.errorQueueMissing(player);

            return;
        }

        optionalQueue.get().apply();

        messageService.previewApplied(player);
    }

    /**
     * Run the preview cancel command.
     *
     * @param player The player
     */
    @Command(value = "preview-cancel")
    @Permission("prism.modify")
    public void onCancel(final Player player) {
        Optional<ModificationQueue> optionalQueue = modificationQueueService.currentQueueForOwner(player);
        if (optionalQueue.isEmpty()) {
            messageService.errorQueueMissing(player);

            return;
        }

        modificationQueueService.clearEverythingForOwner(player);

        messageService.previewCancelled(player);
    }

    /**
     * Run the preview restore command.
     *
     * @param player The player
     * @param arguments The arguments
     */
    @NamedArguments("query-parameters")
    @Command(value = "preview-restore", alias = { "prs" })
    @Permission("prism.modify")
    public void onPreviewRestore(final Player player, final Arguments arguments) {
        var builder = queryService.queryFromArguments(player, arguments);
        if (builder.isPresent()) {
            var queryBuilder = builder.get().modification().reversed(true);

            int maxPerOperation = configurationService.prismConfig().modifications().maxPerOperation();
            if (maxPerOperation > 0) {
                queryBuilder.limit(maxPerOperation);
            }

            final ActivityQuery query = queryBuilder.build();

            // Load the modification ruleset from the configs, and apply flags
            var modificationRuleset = modificationQueueService.applyFlagsToModificationRuleset(arguments).build();

            preview(PaperRestore.class, player, query, modificationRuleset);
        }
    }

    /**
     * Run the preview rollback command.
     *
     * @param player The player
     * @param arguments The arguments
     */
    @NamedArguments("query-parameters")
    @Command(value = "preview-rollback", alias = { "prb" })
    @Permission("prism.modify")
    public void onPreviewRollback(final Player player, final Arguments arguments) {
        var builder = queryService.queryFromArguments(player, arguments);
        if (builder.isPresent()) {
            var queryBuilder = builder.get().modification().reversed(false);

            int maxPerOperation = configurationService.prismConfig().modifications().maxPerOperation();
            if (maxPerOperation > 0) {
                queryBuilder.limit(maxPerOperation);
            }

            final ActivityQuery query = queryBuilder.build();

            // Load the modification ruleset from the configs, and apply flags
            var modificationRuleset = modificationQueueService.applyFlagsToModificationRuleset(arguments).build();

            preview(PaperRollback.class, player, query, modificationRuleset);
        }
    }

    /**
     * Create a new preview.
     *
     * @param clazz The modification queue class we're previewing.
     * @param player The player
     * @param query The query
     */
    protected void preview(
        Class<? extends ModificationQueue> clazz,
        final Player player,
        final ActivityQuery query,
        final ModificationRuleset modificationRuleset
    ) {
        // Ensure a queue is free
        if (!modificationQueueService.queueAvailable()) {
            messageService.errorQueueNotFree(player);

            return;
        }

        Bukkit.getAsyncScheduler()
            .runNow(PrismPaper.instance().loaderPlugin(), task -> {
                var results = queryActivities(player, query);
                if (results == null) {
                    return;
                }

                Bukkit.getGlobalRegionScheduler()
                    .run(PrismPaper.instance().loaderPlugin(), t -> {
                        if (results.isEmpty()) {
                            messageService.noResults(player);

                            return;
                        }

                        ModificationQueue queue = modificationQueueService.newQueue(
                            clazz,
                            modificationRuleset,
                            player,
                            query,
                            results
                        );
                        if (queue instanceof Previewable previewable) {
                            previewable.preview();
                        } else {
                            messageService.errorNotPreviewable(player);
                        }
                    });
            });
    }

    /**
     * Query activities from storage, handling exceptions.
     *
     * @param player The player
     * @param query The activity query
     * @return The list of actions, or null on failure
     */
    private List<Activity> queryActivities(Player player, ActivityQuery query) {
        try {
            return storageAdapter.queryActivities(query);
        } catch (Exception e) {
            loggingService.handleException(e);

            Bukkit.getGlobalRegionScheduler()
                .run(PrismPaper.instance().loaderPlugin(), t -> {
                    messageService.errorQueryExec(player);
                });
        }

        return null;
    }
}
