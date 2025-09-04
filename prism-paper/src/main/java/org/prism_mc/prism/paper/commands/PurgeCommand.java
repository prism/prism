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
import org.prism_mc.prism.api.services.purges.PurgeQueue;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.purge.PurgeService;
import org.prism_mc.prism.paper.services.query.QueryService;

@Command(value = "prism", alias = { "pr" })
public class PurgeCommand {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The query service.
     */
    private final QueryService queryService;

    /**
     * The task chain provider.
     */
    private final PurgeService purgeService;

    /**
     * Construct the purge command.
     *
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param queryService The query service
     * @param purgeService The task chain provider
     */
    @Inject
    public PurgeCommand(
        ConfigurationService configurationService,
        MessageService messageService,
        QueryService queryService,
        PurgeService purgeService
    ) {
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.queryService = queryService;
        this.purgeService = purgeService;
    }

    @Command("purge")
    @Permission("prism.purge")
    public class PurgeSubCommand {

        /**
         * Run a purge.
         *
         * @param sender The command sender
         * @param arguments The arguments
         */
        @CommandFlags(key = "query-flags")
        @NamedArguments("query-parameters")
        @Command("start")
        public void onPurgeStart(final CommandSender sender, final Arguments arguments) {
            if (!purgeService.queueFree()) {
                messageService.errorPurgeQueryNotFree(sender);
            }

            var builder = queryService.queryFromArguments(sender, arguments);
            if (builder.isPresent()) {
                final ActivityQuery query = builder
                    .get()
                    .limit(configurationService.prismConfig().purges().limit())
                    .build();

                if (!query.defaultsUsed().isEmpty()) {
                    messageService.defaultsUsed(sender, String.join(" ", query.defaultsUsed()));
                }

                PurgeQueue purgeQueue = purgeService.newQueue(
                    result -> {
                        messageService.purgeCycle(sender, result);
                    },
                    result -> messageService.purgeComplete(sender, result.deleted())
                );

                purgeQueue.add(query);
                purgeQueue.start();

                messageService.purgeStarting(sender);
            }
        }

        @Command("stop")
        public void onPurgeStop(final CommandSender sender) {
            if (purgeService.queueFree()) {
                messageService.errorPurgeNotRunning(sender);
            }

            purgeService.stop();

            messageService.purgeStopped(sender);
        }
    }
}
