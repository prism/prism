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

package org.prism_mc.prism.bukkit.services.pagination;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.services.pagination.PaginationHandler;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.translation.BukkitTranslationService;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class PaginationService {

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The translation service.
     */
    private final BukkitTranslationService translationService;

    /**
     * Cache pagination handler per sender.
     */
    @Getter
    private final Cache<CommandSender, PaginationHandler<?>> cache;

    /**
     * Constructor.
     *
     * @param cacheService The cache service
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @param messageService The message service
     * @param translationService The translation service
     */
    @Inject
    public PaginationService(
        CacheService cacheService,
        ConfigurationService configurationService,
        LoggingService loggingService,
        MessageService messageService,
        BukkitTranslationService translationService
    ) {
        this.messageService = messageService;
        this.translationService = translationService;

        CacheConfiguration cacheConfiguration = configurationService.prismConfig().cache();

        var cacheBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.lookupExpiration().maxSize())
            .expireAfterAccess(
                cacheConfiguration.lookupExpiration().expiresAfterAccess().duration(),
                cacheConfiguration.lookupExpiration().expiresAfterAccess().timeUnit()
            )
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting paginated results from cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing paginated results from cache: Key: {0} Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (cacheConfiguration.recordStats()) {
            cacheBuilder.recordStats();
        }

        cache = cacheBuilder.build();
        cacheService.caches().put("paginationServiceCache", cache);
    }

    /**
     * Show the current page of the pagination handler.
     *
     * @param sender The sender
     * @param <T> The paginated result type
     */
    public <T> void show(CommandSender sender) {
        var paginationHandler = cache.getIfPresent(sender);

        if (paginationHandler != null) {
            show(sender, paginationHandler);
        } else {
            messageService.errorNothingToPaginate(sender);
        }
    }

    /**
     * Show the current page of the pagination handler.
     *
     * @param sender The sender
     * @param paginationHandler The pagination handler
     * @param <T> The paginated result type
     */
    public <T> void show(CommandSender sender, PaginationHandler<T> paginationHandler) {
        cache.put(sender, paginationHandler);

        var paginationResult = paginationHandler.paginationResult();

        messageService.paginationHeader(sender, paginationResult);

        paginationHandler.subheader().run();

        if (paginationResult.isEmpty()) {
            messageService.noResults(sender);
        } else {
            for (var result : paginationResult.currentPageResults()) {
                paginationHandler.lineRenderer().accept(result);
            }
        }

        if (paginationResult.hasPrevPage() || paginationResult.hasNextPage()) {
            var builder = Component.text();

            if (paginationResult.hasPrevPage()) {
                String cmd = "/pr page " + (paginationResult.currentPage() - 1);

                builder.append(
                    Component.translatable("prism.page-prev")
                        .hoverEvent(
                            HoverEvent.hoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.text(translationService.messageOf(sender, "prism.page-prev-hover"))
                            )
                        )
                        .clickEvent(ClickEvent.runCommand(cmd))
                );
            }

            if (paginationResult.hasPrevPage() && paginationResult.hasNextPage()) {
                builder.append(Component.translatable("prism.page-separator"));
            }

            if (paginationResult.hasNextPage()) {
                String cmd = "/pr page " + (paginationResult.currentPage() + 1);

                builder.append(
                    Component.translatable("prism.page-next")
                        .hoverEvent(
                            HoverEvent.hoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.text(translationService.messageOf(sender, "prism.page-next-hover"))
                            )
                        )
                        .clickEvent(ClickEvent.runCommand(cmd))
                );
            }

            sender.sendMessage(builder.build());
        }
    }
}
