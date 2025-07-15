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
import java.io.IOException;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.bukkit.services.alerts.BukkitAlertService;
import org.prism_mc.prism.bukkit.services.filters.BukkitFilterService;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.translation.BukkitTranslationService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Command(value = "prism", alias = { "pr" })
public class ConfigsCommand {

    /**
     * The alert service.
     */
    private final BukkitAlertService alertService;

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
     * The translation service.
     */
    private final BukkitTranslationService translationService;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The filter service.
     */
    private final BukkitFilterService filterService;

    /**
     * Construct the command.
     *
     * @param loggingService The logging service
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     */
    @Inject
    public ConfigsCommand(
        BukkitAlertService alertService,
        LoggingService loggingService,
        MessageService messageService,
        StorageAdapter storageAdapter,
        BukkitTranslationService translationService,
        ConfigurationService configurationService,
        BukkitFilterService filterService
    ) {
        this.alertService = alertService;
        this.loggingService = loggingService;
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
        this.translationService = translationService;
        this.configurationService = configurationService;
        this.filterService = filterService;
    }

    @Command("configs")
    @Permission("prism.admin")
    public class ConfigsSubCommand {

        @Command("prism")
        public class ConfigSubCommand {

            /**
             * Reload the config.
             *
             * @param sender The command sender
             */
            @Command("reload")
            @Permission("prism.admin")
            public void onReloadConfig(final CommandSender sender) {
                configurationService.loadConfigurations();

                filterService.loadFilters();
                alertService.loadAlerts();

                messageService.reloadedConfig(sender);
            }
        }

        @Command("locales")
        public class LocalesSubCommand {

            /**
             * Reload the locale files.
             *
             * @param sender The command sender
             */
            @Command("reload")
            @Permission("prism.admin")
            public void onReloadLocales(final CommandSender sender) {
                try {
                    translationService.reloadTranslations();

                    messageService.reloadedLocales(sender);
                } catch (IOException e) {
                    messageService.errorReloadLocale(sender);
                    loggingService.handleException(e);
                }
            }
        }

        @Command("storage")
        public class StorageConfigSubCommand {

            /**
             * Run the command to write a hikari properties file.
             *
             * @param sender The command sender
             */
            @Command("write-hikari")
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
}
