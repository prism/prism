/*
 * Prism (Refracted)
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
import java.io.IOException;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Command(value = "prism", alias = { "pr" })
public class ConfigCommand {

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The storage service.
     */
    private final StorageAdapter storageAdapter;

    /**
     * Construct the command.
     *
     * @param loggingService The logging service
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     */
    @Inject
    public ConfigCommand(LoggingService loggingService, MessageService messageService, StorageAdapter storageAdapter) {
        this.loggingService = loggingService;
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
    }

    @Command("storageconfig")
    @Permission("prism.admin")
    public class StorageConfigSubCommand {

        /**
         * Run the command to write a hikari properties file.
         *
         * @param sender The command sender
         */
        @Command("writehikari")
        public void onHikariWrite(final CommandSender sender) {
            try {
                storageAdapter.writeHikariPropertiesFile();

                messageService.hikariFileWritten(sender);
            } catch (IOException e) {
                messageService.errorWriteHikari(sender);
                loggingService.handleException(e);
            }
        }
    }
}
