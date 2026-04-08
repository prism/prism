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
import java.util.List;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationResultStatus;
import org.prism_mc.prism.api.services.modifications.Rollback;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;

@Command(value = "prism", alias = { "pr" })
public class UndoCommand {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The modification queue service.
     */
    private final PaperModificationQueueService modificationQueueService;

    /**
     * Construct the undo command.
     *
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     */
    @Inject
    public UndoCommand(
        ConfigurationService configurationService,
        MessageService messageService,
        PaperModificationQueueService modificationQueueService
    ) {
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.modificationQueueService = modificationQueueService;
    }

    /**
     * Run the undo command.
     *
     * @param sender The command sender
     */
    @Command(value = "undo")
    @Permission("prism.modify")
    public void onUndo(final CommandSender sender) {
        // Ensure a queue is free
        if (!modificationQueueService.queueAvailable()) {
            messageService.errorQueueNotFree(sender);

            return;
        }

        // Get the most recent completed result for this sender
        var optionalResult = modificationQueueService.queueResultForOwner(sender);
        if (optionalResult.isEmpty()) {
            messageService.modificationsUndoNoResult(sender);

            return;
        }

        ModificationQueueResult lastResult = optionalResult.get();

        // Only undo completed operations, not previews
        if (!lastResult.mode().equals(ModificationQueueMode.COMPLETING)) {
            messageService.modificationsUndoNoResult(sender);

            return;
        }

        // Get the activities that were successfully applied
        List<Activity> appliedActivities = lastResult
            .results()
            .stream()
            .filter(r -> r.status().equals(ModificationResultStatus.APPLIED))
            .map(r -> r.activity())
            .toList();

        if (appliedActivities.isEmpty()) {
            messageService.modificationsUndoNoResult(sender);

            return;
        }

        // Get the original query and ruleset
        var originalQuery = lastResult.queue().query();
        var modificationRuleset = configurationService.prismConfig().modifications().toRulesetBuilder().build();

        // Create the opposite queue type and apply
        if (lastResult.queue() instanceof Rollback) {
            // Last operation was a rollback, so undo with a restore
            modificationQueueService
                .newRestoreQueue(modificationRuleset, sender, originalQuery, appliedActivities)
                .apply();
        } else {
            // Last operation was a restore, so undo with a rollback
            modificationQueueService
                .newRollbackQueue(modificationRuleset, sender, originalQuery, appliedActivities)
                .apply();
        }
    }
}
