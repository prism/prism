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

package org.prism_mc.prism.paper.services.wands;

import com.google.inject.Inject;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.services.messages.MessageService;

public abstract class AbstractModificationWand {

    /**
     * The configuration service.
     */
    protected final ConfigurationService configurationService;

    /**
     * The storage adapter.
     */
    protected final StorageAdapter storageAdapter;

    /**
     * The message service.
     */
    protected final MessageService messageService;

    /**
     * The modification queue service.
     */
    protected final ModificationQueueService modificationQueueService;

    /**
     * The logging service.
     */
    protected final LoggingService loggingService;

    /**
     * The owner.
     */
    protected Object owner;

    /**
     * Construct a new inspection wand.
     *
     * @param configurationService The configuration service
     * @param storageAdapter The storage adapter
     * @param messageService The message service
     * @param modificationQueueService The modification queue service
     * @param loggingService The logging service
     */
    @Inject
    public AbstractModificationWand(
        ConfigurationService configurationService,
        StorageAdapter storageAdapter,
        MessageService messageService,
        ModificationQueueService modificationQueueService,
        LoggingService loggingService
    ) {
        this.configurationService = configurationService;
        this.storageAdapter = storageAdapter;
        this.messageService = messageService;
        this.modificationQueueService = modificationQueueService;
        this.loggingService = loggingService;
    }

    /**
     * Activate the wand using a given query and modification class.
     *
     * @param query The query
     * @param clazz The modification class
     */
    protected void use(ActivityQuery query, Class<? extends ModificationQueue> clazz) {
        // Ensure a queue is free
        if (!modificationQueueService.queueAvailable()) {
            messageService.errorQueueNotFree((CommandSender) owner);

            return;
        }

        Bukkit.getAsyncScheduler()
            .runNow(PrismPaper.instance().loaderPlugin(), task -> {
                List<Activity> modifications;
                try {
                    modifications = storageAdapter.queryActivities(query);
                } catch (Exception e) {
                    loggingService.handleException(e);

                    Bukkit.getGlobalRegionScheduler()
                        .run(PrismPaper.instance().loaderPlugin(), t -> {
                            messageService.errorQueryExec((CommandSender) owner);
                        });

                    return;
                }

                Bukkit.getGlobalRegionScheduler()
                    .run(PrismPaper.instance().loaderPlugin(), t -> {
                        if (modifications.isEmpty()) {
                            messageService.noResults((Player) owner);

                            return;
                        }

                        ModificationRuleset modificationRuleset = configurationService
                            .prismConfig()
                            .modifications()
                            .toRulesetBuilder()
                            .build();

                        modificationQueueService
                            .newQueue(clazz, modificationRuleset, owner, query, modifications)
                            .apply();
                    });
            });
    }
}
