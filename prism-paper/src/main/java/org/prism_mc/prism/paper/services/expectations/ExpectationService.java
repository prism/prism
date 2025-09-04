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

package org.prism_mc.prism.paper.services.expectations;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class ExpectationService {

    /**
     * Cache expectations and wipe them if our expectations aren't met.
     */
    Cache<Object, Object> detachExpectations;

    /**
     * Constructor.
     *
     * @param loggingService The logging service
     */
    @Inject
    public ExpectationService(
        CacheService cacheService,
        ConfigurationService configurationService,
        LoggingService loggingService
    ) {
        CacheConfiguration cacheConfiguration = configurationService.prismConfig().cache();

        var cacheBuilder = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting expectations cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing expectations cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (cacheConfiguration.recordStats()) {
            cacheBuilder.recordStats();
        }

        detachExpectations = cacheBuilder.build();
        cacheService.caches().put("detachExpectations", detachExpectations);
    }

    /**
     * Get the expectation.
     *
     * @param target The target
     * @return The cause, if target present
     */
    public Optional<Object> detachExpectation(Object target) {
        return Optional.ofNullable(detachExpectations.getIfPresent(target));
    }

    /**
     * Add a target object and a cause we expect an event will "claim".
     *
     * @param target The target object
     * @param cause The cause
     */
    public void expectDetach(Object target, Object cause) {
        detachExpectations.put(target, cause);
    }

    /**
     * Mark a target as a met expectation. This immediately
     * removes it from the expectations cache and avoids
     * auto-expiring.
     *
     * @param target The target
     */
    public void metDetachExpectation(Object target) {
        detachExpectations.invalidate(target);
    }
}
