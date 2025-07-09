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

package org.prism_mc.prism.core.services.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Getter
@Singleton
public class CacheService {

    /**
     * A convenient place to reference all caches for reporting purposes.
     */
    private final Map<String, Cache<?, ?>> caches = new HashMap<>();

    /**
     * A convenient place to reference all primary key caches for reporting purposes.
     */
    private final Map<String, Cache<?, ?>> primaryKeyCaches = new HashMap<>();

    /**
     * A cache of action keys to primary keys.
     */
    private final Cache<String, Byte> actionKeyPkMap;

    /**
     * A cache of blocks to primary keys.
     */
    private final Cache<String, Integer> blockDataPkMap;

    /**
     * A cache of entity types to primary keys.
     */
    private final Cache<String, Integer> entityTypePkMap;

    /**
     * A cache of items to primary keys.
     */
    private final Cache<String, Integer> itemDataPkMap;

    /**
     * A cache of named causes to primary keys.
     */
    private final Cache<String, Long> namedCausePkMap;

    /**
     * A cache of player uuids to primary keys.
     */
    private final Cache<UUID, Long> playerUuidPkMap;

    /**
     * A cache of world uuids to primary keys.
     */
    private final Cache<UUID, Byte> worldUuidPkMap;

    /**
     * Constructor.
     *
     * @param configurationService The configuration service
     * @param loggingService The logging service
     */
    @Inject
    public CacheService(ConfigurationService configurationService, LoggingService loggingService) {
        final CacheConfiguration cacheConfiguration = configurationService.prismConfig().cache();

        // Build the action key cache
        Caffeine<String, Byte> actionBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCacheActionKey().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting action key from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing action key from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCacheActionKey().expiresAfterAccess() != null &&
            cacheConfiguration.pkCacheActionKey().expiresAfterAccess().duration() != null
        ) {
            actionBuilder.expireAfterAccess(
                cacheConfiguration.pkCacheActionKey().expiresAfterAccess().duration(),
                cacheConfiguration.pkCacheActionKey().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            actionBuilder.recordStats();
        }

        actionKeyPkMap = actionBuilder.build();
        primaryKeyCaches.put("actionKeyPkMap", actionKeyPkMap);

        // Build the block data cache
        Caffeine<String, Integer> blockBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCacheBlockData().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting block data from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing block data from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCacheBlockData().expiresAfterAccess() != null &&
            cacheConfiguration.pkCacheBlockData().expiresAfterAccess().duration() != null
        ) {
            blockBuilder.expireAfterAccess(
                cacheConfiguration.pkCacheBlockData().expiresAfterAccess().duration(),
                cacheConfiguration.pkCacheBlockData().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            blockBuilder.recordStats();
        }

        blockDataPkMap = blockBuilder.build();
        primaryKeyCaches.put("blockDataPkMap", blockDataPkMap);

        // Build the entity type cache
        Caffeine<String, Integer> entityBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCacheEntityType().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting entity type from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing entity type from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCacheEntityType().expiresAfterAccess() != null &&
            cacheConfiguration.pkCacheEntityType().expiresAfterAccess().duration() != null
        ) {
            entityBuilder.expireAfterAccess(
                cacheConfiguration.pkCacheEntityType().expiresAfterAccess().duration(),
                cacheConfiguration.pkCacheEntityType().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            entityBuilder.recordStats();
        }

        entityTypePkMap = entityBuilder.build();
        primaryKeyCaches.put("entityTypePkMap", entityTypePkMap);

        // Build the item data cache
        Caffeine<String, Integer> itemBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCacheItemData().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting item data from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing item data from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCacheItemData().expiresAfterAccess() != null &&
            cacheConfiguration.pkCacheItemData().expiresAfterAccess().duration() != null
        ) {
            itemBuilder.expireAfterAccess(
                cacheConfiguration.pkCacheItemData().expiresAfterAccess().duration(),
                cacheConfiguration.pkCacheItemData().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            itemBuilder.recordStats();
        }

        itemDataPkMap = itemBuilder.build();
        primaryKeyCaches.put("itemDataPkMap", itemDataPkMap);

        // Build the named cause cache
        Caffeine<String, Long> namedCauseBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCacheNamedCause().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting named cause from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing named cause from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCacheNamedCause().expiresAfterAccess() != null &&
            cacheConfiguration.pkCacheNamedCause().expiresAfterAccess().duration() != null
        ) {
            namedCauseBuilder.expireAfterAccess(
                cacheConfiguration.pkCacheNamedCause().expiresAfterAccess().duration(),
                cacheConfiguration.pkCacheNamedCause().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            namedCauseBuilder.recordStats();
        }

        namedCausePkMap = namedCauseBuilder.build();
        primaryKeyCaches.put("namedCausePkMap", namedCausePkMap);

        // Build the player cache
        Caffeine<UUID, Long> playerBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCachePlayer().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting player from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing player from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCachePlayer().expiresAfterAccess() != null &&
            cacheConfiguration.pkCachePlayer().expiresAfterAccess().duration() != null
        ) {
            playerBuilder.expireAfterAccess(
                cacheConfiguration.pkCachePlayer().expiresAfterAccess().duration(),
                cacheConfiguration.pkCachePlayer().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            playerBuilder.recordStats();
        }

        playerUuidPkMap = playerBuilder.build();
        primaryKeyCaches.put("playerUuidPkMap", playerUuidPkMap);

        // Create the world cache
        Caffeine<UUID, Byte> worldBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.pkCacheWorld().maxSize())
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting player from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing player from PK cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (
            cacheConfiguration.pkCacheWorld().expiresAfterAccess() != null &&
            cacheConfiguration.pkCacheWorld().expiresAfterAccess().duration() != null
        ) {
            worldBuilder.expireAfterAccess(
                cacheConfiguration.pkCacheWorld().expiresAfterAccess().duration(),
                cacheConfiguration.pkCacheWorld().expiresAfterAccess().timeUnit()
            );
        }

        if (cacheConfiguration.recordStats()) {
            worldBuilder.recordStats();
        }

        worldUuidPkMap = worldBuilder.build();
        primaryKeyCaches.put("worldUuidPkMap", worldUuidPkMap);
    }
}
