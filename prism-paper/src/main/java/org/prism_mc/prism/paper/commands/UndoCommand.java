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
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.Rollback;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;

@Command(value = "prism", alias = { "pr" })
public class UndoCommand {

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
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     */
    @Inject
    public UndoCommand(MessageService messageService, PaperModificationQueueService modificationQueueService) {
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

        if (lastResult.undoEntries().isEmpty()) {
            messageService.modificationsUndoNoResult(sender);

            return;
        }

        // Replay the captured snapshots. Rollback's undo reverses to restore
        // semantics (reversed flag back to false); restore's undo flips back
        // to true. Snapshots are world-state captures from the moment the
        // original op fired, so this is independent of the activity log.
        boolean undoOfRollback = lastResult.queue() instanceof Rollback;
        modificationQueueService.applyUndo(sender, lastResult, undoOfRollback);
    }
}
