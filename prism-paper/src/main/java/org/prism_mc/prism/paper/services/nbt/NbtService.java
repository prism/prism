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

package org.prism_mc.prism.paper.services.nbt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.utils.StringUtils;

@Singleton
public class NbtService {

    /**
     * Cache of default entity nbt data.
     */
    private final Cache<String, ReadableNBT> entityNbtDefaults;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * List all nbt keys we want stripped from entities.
     */
    private final String[] entityRejectKeys = {
        "DeathTime",
        "Fire",
        "Health",
        "HurtByTimestamp",
        "HurtTime",
        "Motion",
        "OnGround",
        "WorldUUIDLeast",
        "WorldUUIDMost",
    };

    /**
     * Constructor.
     *
     * @param cacheService The cache service
     * @param configurationService Configuration service
     * @param loggingService Logging service
     */
    @Inject
    public NbtService(
        CacheService cacheService,
        ConfigurationService configurationService,
        LoggingService loggingService
    ) {
        CacheConfiguration cacheConfiguration = configurationService.prismConfig().cache();

        this.loggingService = loggingService;

        var cacheBuilder = Caffeine.newBuilder()
            .maximumSize(cacheConfiguration.nbtEntityDefaults().maxSize())
            .expireAfterAccess(
                cacheConfiguration.nbtEntityDefaults().expiresAfterAccess().duration(),
                cacheConfiguration.nbtEntityDefaults().expiresAfterAccess().timeUnit()
            )
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting entity nbt default from cache: Key: %s, Value: %s, Removal Cause: %s";
                loggingService.debug(String.format(msg, key, value, cause));
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing entity nbt default from cache: Key: %s, Value: %s, Removal Cause: %s";
                loggingService.debug(String.format(msg, key, value, cause));
            });

        if (configurationService.prismConfig().cache().recordStats()) {
            cacheBuilder.recordStats();
        }

        entityNbtDefaults = cacheBuilder.build();
        cacheService.caches().put("entityNbtDefaults", entityNbtDefaults);
    }

    /**
     * Trims the entity nbt by removing the defaults.
     *
     * @param entity The entity
     * @param consumer The consumer
     */
    public void processEntityNbt(Entity entity, Consumer<ReadableNBT> consumer) {
        String key = entity.getType().getKey().getKey();

        var cachedDefaultNbt = entityNbtDefaults.getIfPresent(key);
        if (cachedDefaultNbt != null) {
            trimEntityNbt(entity, cachedDefaultNbt, consumer);
        } else {
            EntitySnapshot entitySnapshot = Bukkit.getEntityFactory()
                .createEntitySnapshot(String.format("{id:\"%s\"}", key));

            Entity dummyEntity = entitySnapshot.createEntity(Bukkit.getWorlds().getFirst());
            NBT.get(dummyEntity, defaultNbt -> {
                // Create a nbt container not attached to the entity so we can reuse it
                ReadWriteNBT cacheNbt = NBT.createNBTObject();
                cacheNbt.mergeCompound(defaultNbt);

                // Cache the default nbt for this entity
                entityNbtDefaults.put(key, cacheNbt);

                loggingService.debug(
                    "Caching default entity nbt for {0}. Byte length: {1}",
                    key,
                    StringUtils.getUtf8Mb4Length(defaultNbt.toString())
                );

                trimEntityNbt(entity, defaultNbt, consumer);
            });
        }
    }

    /**
     * Trim the entity nbt by removing the defaults.
     *
     * @param entity The entity
     * @param defaultNbt The default nbt values
     * @param consumer The consumer
     */
    protected void trimEntityNbt(Entity entity, ReadableNBT defaultNbt, Consumer<ReadableNBT> consumer) {
        String key = entity.getType().getKey().getKey();

        NBT.get(entity, nbt -> {
            var originalByteLength = StringUtils.getUtf8Mb4Length(nbt.toString());

            // First, filter by extracting differences with the default
            var filtered = nbt.extractDifference(defaultNbt);

            // Next, reject stuff *we* don't want to track
            for (String reject : entityRejectKeys) {
                filtered.removeKey(reject);
            }

            consumer.accept(filtered);

            var filteredByteLength = StringUtils.getUtf8Mb4Length(filtered.toString());
            loggingService.debug(
                "Filtered entity nbt. Original Byte length: {1} Filtered: {2}",
                key,
                originalByteLength,
                filteredByteLength
            );
        });
    }
}
