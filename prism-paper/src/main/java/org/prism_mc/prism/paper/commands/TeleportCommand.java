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
import dev.triumphteam.cmd.core.annotations.Suggestion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.services.lookup.LookupService;
import org.prism_mc.prism.paper.services.messages.MessageService;

@Command(value = "prism", alias = { "pr" })
public class TeleportCommand {

    /**
     * The lookup service.
     */
    private final LookupService lookupService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * Construct the teleport command.
     *
     * @param lookupService The lookup service
     * @param messageService The message service
     */
    @Inject
    public TeleportCommand(LookupService lookupService, MessageService messageService) {
        this.lookupService = lookupService;
        this.messageService = messageService;
    }

    @Command("teleport")
    public class TeleportSubCommand {

        @Command("id")
        @Permission("prism.lookup")
        public class TeleportToActivity {

            /**
             * Run the teleport command. Teleports to the world/location of a specific record id.
             *
             * @param player The player
             */
            @Command
            public void onTeleport(final Player player, Integer activityId) {
                final ActivityQuery query = ActivityQuery.builder().activityId(activityId).limit(1).build();
                lookupService.lookup(player, query, results -> {
                    if (results.isEmpty()) {
                        messageService.noResults(player);
                    } else {
                        var activity = results.getFirst();

                        messageService.teleportingToActivity(player, activity);

                        World world = Bukkit.getServer().getWorld(activity.world().key());
                        Bukkit.getServer()
                            .getScheduler()
                            .runTask(PrismPaper.instance().loaderPlugin(), () -> {
                                player.teleport(
                                    new Location(
                                        world,
                                        activity.coordinate().intX(),
                                        activity.coordinate().intY(),
                                        activity.coordinate().intZ()
                                    )
                                );
                            });
                    }
                });
            }
        }

        @Command("loc")
        @Permission("prism.lookup")
        public class TeleportToLocation {

            /**
             * Run the teleport command. Teleports to the world/location of a specific record id.
             *
             * @param player The player
             */
            @Command
            public void onTeleport(
                final CommandSender sender,
                final Player player,
                @Suggestion("worlds") String worldName,
                Integer x,
                Integer y,
                Integer z
            ) {
                World world = Bukkit.getServer().getWorld(worldName);
                if (world == null) {
                    messageService.errorParamInvalidWorld(player);

                    return;
                }

                messageService.teleportingTo(sender, worldName, x, y, z);
                if (sender != player) {
                    messageService.teleportingTo(player, worldName, x, y, z);
                }

                player.teleport(new Location(world, x, y, z));
            }
        }
    }
}
