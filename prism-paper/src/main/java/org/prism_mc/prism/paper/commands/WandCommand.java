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
import dev.triumphteam.cmd.core.annotations.Command;
import dev.triumphteam.cmd.core.annotations.Optional;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.services.wands.Wand;
import org.prism_mc.prism.api.services.wands.WandMode;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.wands.WandService;

@Command(value = "prism", alias = { "pr" })
public class WandCommand {

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The wand service.
     */
    private final WandService wandService;

    /**
     * Construct the wand command.
     *
     * @param messageService The message service
     * @param wandService The wand service
     */
    @Inject
    public WandCommand(MessageService messageService, WandService wandService) {
        this.messageService = messageService;
        this.wandService = wandService;
    }

    /**
     * Toggle a wand.
     *
     * @param player The player
     * @param wandMode The wand mode
     */
    @Command("wand")
    public void onWand(final Player player, @Optional WandMode wandMode) {
        // If no wand mode selected, yet player has an active wand, toggle it off
        if (wandMode == null && wandService.hasActiveWand(player)) {
            wandService.deactivateWand(player);

            return;
        }

        // Set mode if none selected
        wandMode = wandMode == null ? WandMode.INSPECT : wandMode;

        if (
            (wandMode == WandMode.INSPECT && !player.hasPermission("prism.lookup")) ||
            (wandMode == WandMode.ROLLBACK && !player.hasPermission("prism.modify")) ||
            (wandMode == WandMode.RESTORE && !player.hasPermission("prism.modify"))
        ) {
            messageService.errorInsufficientPermission(player);

            return;
        }

        java.util.Optional<Wand> activeWand = wandService.getWand(player);
        if (activeWand.isPresent()) {
            if (activeWand.get().mode().equals(wandMode)) {
                // If the wand modes match, deactivate
                wandService.deactivateWand(player);
            } else {
                // If modes differ, just switch modes
                wandService.switchMode(player, wandMode);
            }

            return;
        }

        // Activate wand
        wandService.activateWand(player, wandMode);
    }
}
