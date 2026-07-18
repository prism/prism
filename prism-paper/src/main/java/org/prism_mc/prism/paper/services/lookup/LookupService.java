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

package org.prism_mc.prism.paper.services.lookup;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.activities.GroupedActivity;
import org.prism_mc.prism.api.services.pagination.ListPaginationResult;
import org.prism_mc.prism.api.services.pagination.PaginationHandler;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.pagination.PaginationService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

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
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The pagination service.
     */
    private final PaginationService paginationService;

    /**
     * The scheduler.
     */
    private final PrismScheduler prismScheduler;

    /**
     * Construct the lookup service.
     *
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     * @param loggingService The logging service
     * @param paginationService The pagination service
     * @param prismScheduler The scheduler
     */
    @Inject
    public LookupService(
        MessageService messageService,
        StorageAdapter storageAdapter,
        LoggingService loggingService,
        PaginationService paginationService,
        PrismScheduler prismScheduler
    ) {
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
        this.loggingService = loggingService;
        this.paginationService = paginationService;
        this.prismScheduler = prismScheduler;
    }

    /**
     * Performs an async storage query and displays the results to the command sender in a paginated chat view.
     *
     * @param sender The command sender
     * @param query The activity query
     */
    public void lookup(CommandSender sender, ActivityQuery query) {
        prismScheduler.runAsync(() -> {
            try {
                var paginationResult = storageAdapter.queryActivitiesPaginated(query);

                showResults(sender, paginationResult, query, null);

                if (query.shareWith() != null) {
                    prismScheduler.runGlobal(() -> {
                        Player recipient = Bukkit.getPlayerExact(query.shareWith());
                        if (recipient != null && !isSameSender(sender, recipient)) {
                            showResults(recipient, paginationResult, query, sender.getName());
                        }
                    });
                }
            } catch (Exception ex) {
                loggingService.handleException(ex);

                if (sender instanceof Player player) {
                    prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(sender));
                } else {
                    prismScheduler.runGlobal(() -> messageService.errorQueryExec(sender));
                }
            }
        });
    }

    /**
     * Render a page of results to a single viewer and cache the pagination handler for them so
     * they can page independently. The query runs once in {@link #lookup(CommandSender, ActivityQuery)};
     * this only handles per-viewer rendering and scheduling.
     *
     * @param viewer The viewer to show the results to
     * @param paginationResult The already-computed page of results
     * @param query The activity query
     * @param sharedBy The name of the player who shared these results, or null when the viewer ran
     *     the query themselves
     */
    private void showResults(
        CommandSender viewer,
        ListPaginationResult<AbstractActivity> paginationResult,
        ActivityQuery query,
        String sharedBy
    ) {
        var paginationHandler = createPaginationHandler(
            viewer,
            paginationResult,
            page -> {
                // Paging is private to the viewer, so drop the share target: navigating pages
                // must never re-broadcast to the other player.
                final ActivityQuery newQuery = query
                    .toBuilder()
                    .offset(paginationResult.offsetForPage(page))
                    .shareWith(null)
                    .build();
                lookup(viewer, newQuery);
            },
            query,
            sharedBy
        );

        Runnable showTask = () -> paginationService.show(viewer, paginationHandler);
        if (viewer instanceof Player player) {
            prismScheduler.runForEntity(player, showTask);
        } else {
            prismScheduler.runGlobal(showTask);
        }
    }

    /**
     * Determine whether a resolved recipient is the same as the command sender, so a player who
     * shares with themselves is not shown the results twice.
     *
     * @param sender The command sender
     * @param recipient The resolved share recipient
     * @return True if they are the same player
     */
    private boolean isSameSender(CommandSender sender, Player recipient) {
        return sender instanceof Player player && player.getUniqueId().equals(recipient.getUniqueId());
    }

    /**
     * Performs an async count query and displays the total count to the command sender.
     *
     * @param sender The command sender
     * @param query The activity query
     */
    public void count(CommandSender sender, ActivityQuery query) {
        Bukkit.getAsyncScheduler()
            .runNow(PrismPaper.instance().loaderPlugin(), task -> {
                try {
                    int count = storageAdapter.countActivities(query);
                    Bukkit.getGlobalRegionScheduler()
                        .run(PrismPaper.instance().loaderPlugin(), t -> {
                            if (!query.defaultsUsed().isEmpty()) {
                                messageService.defaultsUsed(sender, String.join(" ", query.defaultsUsed()));
                            }

                            messageService.countResult(sender, count);

                            // The share flag additionally shows the count to another player.
                            if (query.shareWith() != null) {
                                Player recipient = Bukkit.getPlayerExact(query.shareWith());
                                if (recipient != null && !isSameSender(sender, recipient)) {
                                    messageService.sharedResults(recipient, sender.getName());
                                    messageService.countResult(recipient, count);
                                }
                            }
                        });
                } catch (Exception ex) {
                    loggingService.handleException(ex);

                    Bukkit.getGlobalRegionScheduler()
                        .run(PrismPaper.instance().loaderPlugin(), t -> {
                            messageService.errorQueryExec(sender);
                        });
                }
            });
    }

    /**
     * Performs an async storage query and passes the result to the consumer.
     *
     * @param query The activity query
     * @param consumer The result consumer
     */
    public void lookup(ActivityQuery query, Consumer<List<Activity>> consumer) {
        prismScheduler.runAsync(() -> {
            try {
                consumer.accept(storageAdapter.queryActivities(query));
            } catch (Exception ex) {
                loggingService.handleException(ex);
            }
        });
    }

    /**
     * Performs an async storage query, caches the query for the sender, and passes the result to the consumer.
     *
     * @param sender The command sender
     * @param query The activity query
     * @param consumer The result consumer
     */
    public void lookup(CommandSender sender, ActivityQuery query, Consumer<List<Activity>> consumer) {
        prismScheduler.runAsync(() -> {
            try {
                consumer.accept(storageAdapter.queryActivities(query));
            } catch (Exception ex) {
                loggingService.handleException(ex);

                if (sender instanceof Player player) {
                    prismScheduler.runForEntity(player, () -> messageService.errorQueryExec(sender));
                } else {
                    prismScheduler.runGlobal(() -> messageService.errorQueryExec(sender));
                }
            }
        });
    }

    /**
     * Display paginated lookup results to a viewer.
     *
     * @param viewer The viewer to render the results to
     * @param paginationSource The pagination source
     * @param paginator The paginator
     * @param query The activity query
     * @param sharedBy The name of the player who shared these results, or null when the viewer ran
     *     the query themselves
     */
    private PaginationHandler<AbstractActivity> createPaginationHandler(
        CommandSender viewer,
        ListPaginationResult<AbstractActivity> paginationSource,
        PaginationHandler.Paginator paginator,
        ActivityQuery query,
        String sharedBy
    ) {
        return new PaginationHandler<>(
            paginationSource,
            paginator,
            () -> {
                if (sharedBy != null) {
                    messageService.sharedResults(viewer, sharedBy);
                } else if (!query.defaultsUsed().isEmpty()) {
                    messageService.defaultsUsed(viewer, String.join(" ", query.defaultsUsed()));
                }
            },
            activity -> {
                if (activity instanceof GroupedActivity groupedActivity) {
                    if (activity.action().type().usesDescriptor() && groupedActivity.count() > 1) {
                        messageService.listActivityRowGrouped(viewer, activity);
                    } else if (activity.action().type().usesDescriptor() && groupedActivity.count() == 1) {
                        messageService.listActivityRowGroupedNoQuantity(viewer, activity);
                    } else if (!activity.action().type().usesDescriptor() && groupedActivity.count() == 1) {
                        messageService.listActivityRowGroupedNoDescriptorNoQuantity(viewer, activity);
                    } else {
                        messageService.listActivityRowGroupedNoDescriptor(viewer, activity);
                    }
                } else {
                    if (activity.action().type().usesDescriptor()) {
                        messageService.listActivityRowUngrouped(viewer, activity);
                    } else {
                        messageService.listActivityRowUngroupedNoDescriptor(viewer, activity);
                    }
                }
            }
        );
    }
}
