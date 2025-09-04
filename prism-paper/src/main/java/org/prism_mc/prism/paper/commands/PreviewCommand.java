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
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.Previewable;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;
import org.prism_mc.prism.paper.services.modifications.PaperRestore;
import org.prism_mc.prism.paper.services.modifications.PaperRollback;
import org.prism_mc.prism.paper.services.query.QueryService;

@Command(value = "prism", alias = { "pr" })
public class PreviewCommand {

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
     * The task chain provider.
     */
    private final TaskChainProvider taskChainProvider;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Construct the rollback command.
     *
     * @param storageAdapter The storage adapter
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param queryService The query service
     * @param taskChainProvider The taskchain provider
     * @param loggingService The logging service
     */
    @Inject
    public PreviewCommand(
        StorageAdapter storageAdapter,
        MessageService messageService,
        PaperModificationQueueService modificationQueueService,
        QueryService queryService,
        TaskChainProvider taskChainProvider,
        LoggingService loggingService
    ) {
        this.storageAdapter = storageAdapter;
        this.messageService = messageService;
        this.modificationQueueService = modificationQueueService;
        this.queryService = queryService;
        this.taskChainProvider = taskChainProvider;
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
            final ActivityQuery query = builder.get().modification().reversed(true).build();

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
            final ActivityQuery query = builder.get().modification().reversed(false).build();

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

        taskChainProvider
            .newChain()
            .asyncFirst(() -> {
                try {
                    return storageAdapter.queryActivities(query);
                } catch (Exception e) {
                    messageService.errorQueryExec(player);
                    loggingService.handleException(e);
                }

                return null;
            })
            .abortIfNull()
            .<List<Action>>sync(results -> {
                if (results.isEmpty()) {
                    messageService.noResults(player);

                    return null;
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

                return null;
            })
            .execute();
    }
}
