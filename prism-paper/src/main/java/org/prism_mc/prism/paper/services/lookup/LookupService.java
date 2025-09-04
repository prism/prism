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
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.activities.GroupedActivity;
import org.prism_mc.prism.api.services.pagination.ListPaginationResult;
import org.prism_mc.prism.api.services.pagination.PaginationHandler;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.pagination.PaginationService;

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
     * The task chain provider.
     */
    private final TaskChainProvider taskChainProvider;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The pagination service.
     */
    private final PaginationService paginationService;

    /**
     * Construct the lookup service.
     *
     * @param messageService The message service
     * @param storageAdapter The storage adapter
     * @param taskChainProvider The task chain provider
     * @param loggingService The logging service
     * @param paginationService The pagination service
     */
    @Inject
    public LookupService(
        MessageService messageService,
        StorageAdapter storageAdapter,
        TaskChainProvider taskChainProvider,
        LoggingService loggingService,
        PaginationService paginationService
    ) {
        this.messageService = messageService;
        this.storageAdapter = storageAdapter;
        this.taskChainProvider = taskChainProvider;
        this.loggingService = loggingService;
        this.paginationService = paginationService;
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
                    var paginationResult = storageAdapter.queryActivitiesPaginated(query);
                    var paginationHandler = createPaginationHandler(
                        sender,
                        paginationResult,
                        page -> {
                            paginationResult.currentPage(page);

                            final ActivityQuery newQuery = query.toBuilder().offset(paginationResult.offset()).build();
                            lookup(sender, newQuery);
                        },
                        query
                    );

                    paginationService.show(sender, paginationHandler);
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
                } catch (Exception ex) {
                    messageService.errorQueryExec(sender);
                    loggingService.handleException(ex);
                }
            })
            .execute();
    }

    /**
     * Display paginated lookup results to a command sender.
     *
     * @param sender The command sender
     * @param paginationSource The pagination source
     */
    private PaginationHandler<AbstractActivity> createPaginationHandler(
        CommandSender sender,
        ListPaginationResult<AbstractActivity> paginationSource,
        PaginationHandler.Paginator paginator,
        ActivityQuery query
    ) {
        return new PaginationHandler<>(
            paginationSource,
            paginator,
            () -> {
                if (!query.defaultsUsed().isEmpty()) {
                    messageService.defaultsUsed(sender, String.join(" ", query.defaultsUsed()));
                }
            },
            activity -> {
                if (activity instanceof GroupedActivity groupedActivity) {
                    if (activity.action().type().usesDescriptor() && groupedActivity.count() > 1) {
                        messageService.listActivityRowGrouped(sender, activity);
                    } else if (activity.action().type().usesDescriptor() && groupedActivity.count() == 1) {
                        messageService.listActivityRowGroupedNoQuantity(sender, activity);
                    } else if (!activity.action().type().usesDescriptor() && groupedActivity.count() == 1) {
                        messageService.listActivityRowGroupedNoDescriptorNoQuantity(sender, activity);
                    } else {
                        messageService.listActivityRowGroupedNoDescriptor(sender, activity);
                    }
                } else {
                    if (activity.action().type().usesDescriptor()) {
                        messageService.listActivityRowUngrouped(sender, activity);
                    } else {
                        messageService.listActivityRowUngroupedNoDescriptor(sender, activity);
                    }
                }
            }
        );
    }
}
