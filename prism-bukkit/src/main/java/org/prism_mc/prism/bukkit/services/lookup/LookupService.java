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

package org.prism_mc.prism.bukkit.services.lookup;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.PaginatedResults;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.activities.GroupedActivity;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.bukkit.providers.TaskChainProvider;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.translation.BukkitTranslationService;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class LookupService {

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The translation service.
     */
    private final BukkitTranslationService translationService;

    /**
     * The task chain provider.
     */
    private final TaskChainProvider taskChainProvider;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Cache recent queries.
     */
    @Getter
    private final Cache<CommandSender, ActivityQuery> recentQueries;

    /**
     * Construct the lookup service.
     *
     * @param cacheService The cache service
     * @param configurationService The configuration service
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     * @param translationService The translation service
     * @param taskChainProvider The task chain provider
     * @param loggingService The logging service
     */
    @Inject
    public LookupService(
        CacheService cacheService,
        ConfigurationService configurationService,
        MessageService messageService,
        StorageAdapter storageAdapter,
        BukkitTranslationService translationService,
        TaskChainProvider taskChainProvider,
        LoggingService loggingService
    ) {
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
        this.translationService = translationService;
        this.taskChainProvider = taskChainProvider;
        this.loggingService = loggingService;

        CacheConfiguration cacheConfiguration = configurationService.prismConfig().cache();

        var cacheBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.lookupExpiration().maxSize())
            .expireAfterAccess(
                cacheConfiguration.lookupExpiration().expiresAfterAccess().duration(),
                cacheConfiguration.lookupExpiration().expiresAfterAccess().timeUnit()
            )
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting activity query from cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing activity query from cache: Key: {0} Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (cacheConfiguration.recordStats()) {
            cacheBuilder.recordStats();
        }

        recentQueries = cacheBuilder.build();
        cacheService.caches().put("lookupQueries", recentQueries);
    }

    /**
     * Get the last query for a command sender.
     *
     * @param sender The sender
     * @return The last query, if any
     */
    public Optional<ActivityQuery> lastQuery(CommandSender sender) {
        return Optional.ofNullable(recentQueries.getIfPresent(sender));
    }

    /**
     * Performs an async storage query and displays the results to the command sender in a paginated chat view.
     *
     * @param sender The command sender
     * @param query The activity query
     */
    public void lookup(CommandSender sender, ActivityQuery query) {
        taskChainProvider
            .newChain()
            .async(() -> {
                try {
                    show(sender, storageAdapter.queryActivitiesPaginated(query), query);

                    // Cache this senders' most recent query
                    recentQueries.put(sender, query);
                } catch (Exception ex) {
                    messageService.errorQueryExec(sender);
                    loggingService.handleException(ex);
                }
            })
            .execute();
    }

    /**
     * Performs an async storage query and passes the result to the consumer.
     *
     * @param query The activity query
     * @param consumer The result consumer
     */
    public void lookup(ActivityQuery query, Consumer<List<Activity>> consumer) {
        taskChainProvider
            .newChain()
            .async(() -> {
                try {
                    consumer.accept(storageAdapter.queryActivities(query));
                } catch (Exception ex) {
                    loggingService.handleException(ex);
                }
            })
            .execute();
    }

    /**
     * Performs an async storage query, caches the query for the sender, and passes the result to the consumer.
     *
     * @param sender The command sender
     * @param query The activity query
     * @param consumer The result consumer
     */
    public void lookup(CommandSender sender, ActivityQuery query, Consumer<List<Activity>> consumer) {
        taskChainProvider
            .newChain()
            .async(() -> {
                try {
                    consumer.accept(storageAdapter.queryActivities(query));

                    // Cache this senders' most recent query
                    recentQueries.put(sender, query);
                } catch (Exception ex) {
                    messageService.errorQueryExec(sender);
                    loggingService.handleException(ex);
                }
            })
            .execute();
    }

    /**
     * Display paginated results to a command sender.
     *
     * @param sender The command sender
     * @param results The paginated results
     * @param query The original query
     */
    private void show(CommandSender sender, PaginatedResults<AbstractActivity> results, ActivityQuery query) {
        messageService.paginationHeader(sender, results);

        if (!query.defaultsUsed().isEmpty()) {
            messageService.defaultsUsed(sender, String.join(" ", query.defaultsUsed()));
        }

        if (results.isEmpty()) {
            messageService.noResults(sender);
        } else {
            for (var activity : results.results()) {
                if (activity instanceof GroupedActivity groupedActivity) {
                    if (activity.action().descriptor() != null && groupedActivity.count() > 1) {
                        messageService.listActivityRowGrouped(sender, activity);
                    } else if (activity.action().descriptor() != null && groupedActivity.count() == 1) {
                        messageService.listActivityRowGroupedNoQuantity(sender, activity);
                    } else if (activity.action().descriptor() == null && groupedActivity.count() == 1) {
                        messageService.listActivityRowGroupedNoDescriptorNoQuantity(sender, activity);
                    } else {
                        messageService.listActivityRowGroupedNoDescriptor(sender, activity);
                    }
                } else {
                    if (activity.action().descriptor() != null) {
                        messageService.listActivityRowUngrouped(sender, activity);
                    } else {
                        messageService.listActivityRowUngroupedNoDescriptor(sender, activity);
                    }
                }
            }

            if (results.hasPrevPage() || results.hasNextPage()) {
                var builder = Component.text();

                if (results.hasPrevPage()) {
                    String cmd = "/pr page " + (results.currentPage() - 1);

                    builder.append(
                        Component.translatable("prism.page-prev")
                            .hoverEvent(
                                HoverEvent.hoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.text(translationService.messageOf(sender, "prism.page-prev-hover"))
                                )
                            )
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                    );
                }

                if (results.hasPrevPage() && results.hasNextPage()) {
                    builder.append(Component.translatable("prism.page-separator"));
                }

                if (results.hasNextPage()) {
                    String cmd = "/pr page " + (results.currentPage() + 1);

                    builder.append(
                        Component.translatable("prism.page-next")
                            .hoverEvent(
                                HoverEvent.hoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.text(translationService.messageOf(sender, "prism.page-next-hover"))
                                )
                            )
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                    );
                }

                sender.sendMessage(builder.build());
            }
        }
    }
}
