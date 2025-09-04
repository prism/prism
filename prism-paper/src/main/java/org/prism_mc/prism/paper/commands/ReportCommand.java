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
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.modifications.ModificationResultStatus;
import org.prism_mc.prism.api.services.pagination.ListPaginationResult;
import org.prism_mc.prism.api.services.pagination.PaginationHandler;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.pagination.PaginationService;

@Command(value = "prism", alias = { "pr" })
public class ReportCommand {

    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The modification queue service.
     */
    private final ModificationQueueService modificationQueueService;

    /**
     * The pagination service.
     */
    private final PaginationService paginationService;

    /**
     * Construct the command.
     *
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param paginationService The pagination service
     */
    @Inject
    public ReportCommand(
        ConfigurationService configurationService,
        MessageService messageService,
        ModificationQueueService modificationQueueService,
        PaginationService paginationService
    ) {
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.modificationQueueService = modificationQueueService;
        this.paginationService = paginationService;
    }

    @Command("report")
    @Permission("prism.modify")
    public class ReportSubCommand {

        /**
         * Run the modification queue report command.
         *
         * @param sender The command sender
         */
        @Command("modification-queue")
        public void onModificationQueueReport(final CommandSender sender) {
            if (modificationQueueService.currentQueue() == null) {
                messageService.errorQueueReportEmpty(sender);

                return;
            }

            var owner = "console";
            if (modificationQueueService.currentQueue().owner() instanceof Player player) {
                owner = player.getName();
            }

            messageService.modificationsReportQueueHeader(sender);
            messageService.modificationsReportQueueEntry(
                sender,
                modificationQueueService.currentQueue().queueSize(),
                owner
            );
        }

        /**
         * Run the modification skips report command.
         *
         * @param sender The command sender
         */
        @Command("partial")
        public void onPartialReport(final CommandSender sender) {
            Optional<ModificationQueueResult> resultOptional = modificationQueueService.queueResultForOwner(sender);
            if (resultOptional.isEmpty()) {
                messageService.errorQueueResultMissing(sender);

                return;
            }

            ModificationQueueResult queueResult = resultOptional.get();

            messageService.modificationsReportPartialHeader(sender);

            var partialResults = queueResult
                .results()
                .stream()
                .filter(result -> result.status().equals(ModificationResultStatus.PARTIAL))
                .toList();

            var paginationResult = new ListPaginationResult<>(
                partialResults,
                configurationService.prismConfig().defaults().perPage()
            );

            var paginationHandler = new PaginationHandler<>(
                paginationResult,
                page -> {
                    paginationResult.currentPage(page);
                    paginationService.show(sender);
                },
                (result -> {
                        messageService.modificationsReportSkippedActivity(sender, result.activity(), result);
                    })
            );

            paginationService.show(sender, paginationHandler);
        }

        /**
         * Run the modification skips report command.
         *
         * @param sender The command sender
         */
        @Command("skips")
        public void onSkipsReport(final CommandSender sender) {
            Optional<ModificationQueueResult> resultOptional = modificationQueueService.queueResultForOwner(sender);
            if (resultOptional.isEmpty()) {
                messageService.errorQueueResultMissing(sender);

                return;
            }

            ModificationQueueResult queueResult = resultOptional.get();

            messageService.modificationsReportSkippedHeader(sender);

            var skippedResults = queueResult
                .results()
                .stream()
                .filter(result -> result.status().equals(ModificationResultStatus.SKIPPED))
                .toList();

            var paginationResult = new ListPaginationResult<>(
                skippedResults,
                configurationService.prismConfig().defaults().perPage()
            );

            var paginationHandler = new PaginationHandler<>(
                paginationResult,
                page -> {
                    paginationResult.currentPage(page);
                    paginationService.show(sender);
                },
                (result -> {
                        messageService.modificationsReportSkippedActivity(sender, result.activity(), result);
                    })
            );

            paginationService.show(sender, paginationHandler);
        }
    }
}
