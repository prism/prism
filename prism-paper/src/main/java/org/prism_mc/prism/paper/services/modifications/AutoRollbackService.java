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

package org.prism_mc.prism.paper.services.modifications;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.AutoRollbackConfiguration;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.utils.DateUtils;

@Singleton
public class AutoRollbackService {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The modification queue service.
     */
    private final PaperModificationQueueService modificationQueueService;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The task chain provider.
     */
    private final TaskChainProvider taskChainProvider;

    /**
     * Construct the auto rollback service.
     *
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @param modificationQueueService The modification queue service
     * @param storageAdapter The storage adapter
     * @param taskChainProvider The task chain provider
     */
    @Inject
    public AutoRollbackService(
        ConfigurationService configurationService,
        LoggingService loggingService,
        PaperModificationQueueService modificationQueueService,
        StorageAdapter storageAdapter,
        TaskChainProvider taskChainProvider
    ) {
        this.configurationService = configurationService;
        this.loggingService = loggingService;
        this.modificationQueueService = modificationQueueService;
        this.storageAdapter = storageAdapter;
        this.taskChainProvider = taskChainProvider;
    }

    /**
     * Trigger an automatic rollback for a banned player.
     *
     * @param playerName The name of the banned player
     */
    public void rollbackPlayer(String playerName) {
        AutoRollbackConfiguration config = configurationService.prismConfig().autoRollback();

        Long afterTimestamp = DateUtils.parseTimestamp(config.timeParameter());
        if (afterTimestamp == null) {
            loggingService.error("Auto-rollback: invalid time window \"{0}\", skipping.", config.timeParameter());

            return;
        }

        if (!modificationQueueService.queueAvailable()) {
            loggingService.warn("Auto-rollback: queue is busy, skipping rollback for banned player {0}.", playerName);

            return;
        }

        int maxPerOperation = configurationService.prismConfig().modifications().maxPerOperation();

        var queryBuilder = PaperActivityQuery.builder().causePlayerName(playerName).after(afterTimestamp).rollback();

        if (maxPerOperation > 0) {
            queryBuilder.limit(maxPerOperation);
        }

        final ActivityQuery query = queryBuilder.build();

        loggingService.info("Auto-rollback: querying activities for banned player {0}...", playerName);

        taskChainProvider
            .newChain()
            .asyncFirst(() -> {
                try {
                    return storageAdapter.queryActivities(query);
                } catch (Exception e) {
                    loggingService.error("Auto-rollback: query failed for player {0}.", playerName);
                    loggingService.handleException(e);
                }

                return null;
            })
            .abortIfNull()
            .syncLast(modifications -> {
                if (modifications.isEmpty()) {
                    loggingService.info(
                        "Auto-rollback: no activities found for player {0}, nothing to rollback.",
                        playerName
                    );

                    return;
                }

                if (!modificationQueueService.queueAvailable()) {
                    loggingService.warn(
                        "Auto-rollback: queue became busy, skipping rollback for player {0}.",
                        playerName
                    );

                    return;
                }

                loggingService.info(
                    "Auto-rollback: rolling back {0} activities for banned player {1}.",
                    modifications.size(),
                    playerName
                );

                var modificationRuleset = configurationService.prismConfig().modifications().toRulesetBuilder().build();

                modificationQueueService
                    .newRollbackQueue(modificationRuleset, Bukkit.getConsoleSender(), query, modifications)
                    .apply();
            })
            .execute();
    }
}
