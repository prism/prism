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
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;
import org.prism_mc.prism.paper.services.query.QueryService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

@Command(value = "prism", alias = { "pr" })
public class RollbackCommand {

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
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

    /**
     * Construct the rollback command.
     *
     * @param configurationService The configuration service
     * @param storageAdapter The storage adapter
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param queryService The query service
     * @param loggingService The logging service
     * @param prismScheduler The scheduler
     */
    @Inject
    public RollbackCommand(
        ConfigurationService configurationService,
        StorageAdapter storageAdapter,
        MessageService messageService,
        PaperModificationQueueService modificationQueueService,
        QueryService queryService,
        LoggingService loggingService,
        PrismScheduler prismScheduler
    ) {
        this.configurationService = configurationService;
        this.storageAdapter = storageAdapter;
        this.messageService = messageService;
        this.modificationQueueService = modificationQueueService;
        this.queryService = queryService;
        this.loggingService = loggingService;
        this.prismScheduler = prismScheduler;
    }

    /**
     * Run the rollback command.
     *
     * @param sender The command sender
     * @param arguments The arguments
     */
    @CommandFlags(key = "query-flags")
    @NamedArguments("query-parameters")
    @Command(value = "rollback", alias = { "rb" })
    @Permission("prism.modify")
    public void onRollback(final CommandSender sender, final Arguments arguments) {
        // Ensure a queue is free
        if (!modificationQueueService.queueAvailable()) {
            messageService.errorQueueNotFree(sender);

            return;
        }

        var builder = queryService.queryFromArguments(sender, arguments);
        if (builder.isPresent()) {
            var queryBuilder = builder.get().rollback();

            int maxPerOperation = configurationService.prismConfig().modifications().maxPerOperation();
            if (maxPerOperation > 0) {
                queryBuilder.limit(maxPerOperation);
            }

            final ActivityQuery query = queryBuilder.build();
            prismScheduler.runAsync(() -> {
                var modifications = queryActivities(sender, query);
                if (modifications == null) {
                    return;
                }

                Runnable applyTask = () -> {
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

                    modificationQueueService
                        .newRollbackQueue(modificationRuleset, sender, query, modifications)
                        .apply();
                };

                if (sender instanceof Player player) {
                    prismScheduler.runForEntity(player, applyTask);
                } else {
                    prismScheduler.runGlobal(applyTask);
                }
            });
        }
    }

    /**
     * Query activities from storage, handling exceptions.
     *
     * @param sender The command sender
     * @param query The activity query
     * @return The list of actions, or null on failure
     */
    private List<Activity> queryActivities(CommandSender sender, ActivityQuery query) {
        try {
            return storageAdapter.queryActivities(query);
        } catch (Exception e) {
            loggingService.handleException(e);

            if (sender instanceof Player player) {
                prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(sender));
            } else {
                prismScheduler.runGlobal(() -> messageService.errorQueryExec(sender));
            }
        }

        return null;
    }
}
