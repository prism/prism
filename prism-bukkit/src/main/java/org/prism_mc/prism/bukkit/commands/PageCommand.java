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

package org.prism_mc.prism.bukkit.commands;

import com.google.inject.Inject;
import dev.triumphteam.cmd.bukkit.annotation.Permission;
import dev.triumphteam.cmd.core.annotations.Command;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.pagination.PaginationService;

@Command(value = "prism", alias = { "pr" })
public class PageCommand {

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The pagination service.
     */
    private final PaginationService paginationService;

    /**
     * Construct the page command.
     *
     * @param messageService The message service
     * @param paginationService The pagination service
     */
    @Inject
    public PageCommand(MessageService messageService, PaginationService paginationService) {
        this.messageService = messageService;
        this.paginationService = paginationService;
    }

    /**
     * Change pages of a recent lookup.
     *
     * @param sender The command sender
     * @param page The new page
     */
    @Command(value = "page")
    @Permission("prism.lookup")
    public void onPage(CommandSender sender, Integer page) {
        var paginationHandler = paginationService.cache().getIfPresent(sender);

        if (paginationHandler == null) {
            messageService.errorNothingToPaginate(sender);
            return;
        }

        if (page < 1 || page > paginationHandler.paginationResult().totalPages()) {
            messageService.errorInvalidPage(sender);
            return;
        }

        paginationHandler.showPage(page);
    }
}
