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
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.services.messages.MessageService;

@Command(value = "prism", alias = { "pr" })
public class CacheCommand {

    /**
     * The cache service.
     */
    private final CacheService cacheService;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * Constructor.
     *
     * @param cacheService The cache service
     * @param configurationService The configuration service
     * @param messageService The message service
     */
    @Inject
    public CacheCommand(
        CacheService cacheService,
        ConfigurationService configurationService,
        MessageService messageService
    ) {
        this.cacheService = cacheService;
        this.configurationService = configurationService;
        this.messageService = messageService;
    }

    @Command("cache")
    public class CacheSubCommand {

        @Command("list")
        @Permission("prism.admin")
        public class ListCaches {

            /**
             * Run the command.
             *
             * @param sender The command sender
             */
            @Command
            public void onListCaches(final CommandSender sender) {
                if (!configurationService.prismConfig().cache().recordStats()) {
                    messageService.errorRecordStats(sender);

                    return;
                }

                messageService.cacheListHeader(sender);

                for (var entry : cacheService.primaryKeyCaches().entrySet()) {
                    messageService.cacheListEntry(
                        sender,
                        entry.getKey(),
                        entry.getValue().estimatedSize(),
                        entry.getValue().stats().hitCount()
                    );
                }

                for (var entry : cacheService.caches().entrySet()) {
                    messageService.cacheListEntry(
                        sender,
                        entry.getKey(),
                        entry.getValue().estimatedSize(),
                        entry.getValue().stats().hitCount()
                    );
                }
            }
        }
    }
}
