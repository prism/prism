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
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;
import org.prism_mc.prism.paper.services.query.QueryService;

@Command(value = "prism", alias = { "pr" })
public class RestoreCommand {

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
     * Construct the restore command.
     *
     * @param storageAdapter The storage adapter
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param queryService The query service
     * @param taskChainProvider The task chain provider
     * @param loggingService The logging service
     */
    @Inject
    public RestoreCommand(
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
     * Run the restore command.
     *
     * @param sender The command sender
     */
    @CommandFlags(key = "query-flags")
    @NamedArguments("query-parameters")
    @Command(value = "restore", alias = { "rs" })
    @Permission("prism.modify")
    public void onRestore(final CommandSender sender, final Arguments arguments) {
        // Ensure a queue is free
        if (!modificationQueueService.queueAvailable()) {
            messageService.errorQueueNotFree(sender);

            return;
        }

        var builder = queryService.queryFromArguments(sender, arguments);
        if (builder.isPresent()) {
            final ActivityQuery query = builder.get().restore().build();
            taskChainProvider
                .newChain()
                .asyncFirst(() -> {
                    try {
                        return storageAdapter.queryActivities(query);
                    } catch (Exception e) {
                        messageService.errorQueryExec(sender);
                        loggingService.handleException(e);
                    }

                    return null;
                })
                .abortIfNull()
                .syncLast(modifications -> {
                    if (modifications.isEmpty()) {
                        messageService.noResults(sender);

                        return;
                    }

                    if (!query.defaultsUsed().isEmpty()) {
                        messageService.defaultsUsed(sender, String.join(" ", query.defaultsUsed()));
                    }

                    // Load the modification ruleset from the configs, and apply flags
                    var modificationRuleset = modificationQueueService
                        .applyFlagsToModificationRuleset(arguments)
                        .build();

                    modificationQueueService.newRestoreQueue(modificationRuleset, sender, query, modifications).apply();
                })
                .execute();
        }
    }
}
