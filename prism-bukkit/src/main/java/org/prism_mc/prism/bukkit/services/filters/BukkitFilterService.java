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

package org.prism_mc.prism.bukkit.services.filters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.filters.FilterBehavior;
import org.prism_mc.prism.api.services.filters.FilterService;
import org.prism_mc.prism.bukkit.utils.CustomTag;
import org.prism_mc.prism.bukkit.utils.ListUtils;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConditionsConfiguration;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;

@Singleton
public class BukkitFilterService implements FilterService {

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * Cache all "IGNORE" filters.
     */
    private final List<ActivityFilter> ignoreFilters = new ArrayList<>();

    /**
     * Cache all "ALLOW" filters.
     */
    private final List<ActivityFilter> allowFilters = new ArrayList<>();

    /**
     * Construct a new filter service.
     *
     * @param loggingService The logging service
     * @param configurationService The configuration service
     */
    @Inject
    public BukkitFilterService(LoggingService loggingService, ConfigurationService configurationService) {
        this.loggingService = loggingService;
        this.configurationService = configurationService;

        loadFilters();
    }

    /**
     * Load all filters from the config.
     */
    public void loadFilters() {
        ignoreFilters.clear();
        allowFilters.clear();

        // Convert all configured filters into Filter objects
        for (FilterConfiguration config : configurationService.prismConfig().filters()) {
            var name = config.name() == null || config.name().isEmpty() ? "Unnamed" : config.name();
            loadFilter(name, config.behavior(), config.conditions());
        }
    }

    /**
     * Load filter.
     */
    protected void loadFilter(String filterName, FilterBehavior behavior, FilterConditionsConfiguration config) {
        // Behavior
        if (behavior == null) {
            loggingService.warn(
                "Filter error: No behavior defined in filter {0}. Behavior must be either IGNORE or ALLOW.",
                filterName
            );

            return;
        }

        boolean conditionExists = false;

        // Worlds
        // Note: Worlds may not be loaded here and users type world names so we'll
        // just rely on the name for comparison. No need for UUIDs otherwise we'd need
        // to monitor world load/unload events.
        // Unfortunately that also means we can't error when an invalid world is configured.
        List<String> worldNames = config.worlds();

        if (
            !ListUtils.isNullOrEmpty(worldNames) ||
            !ListUtils.isNullOrEmpty(config.permissions()) ||
            !ListUtils.isNullOrEmpty(config.actions())
        ) {
            conditionExists = true;
        }

        var entityTypeTags = new CustomTag<>(EntityType.class);

        // Entity Types
        if (!ListUtils.isNullOrEmpty(config.entityTypes())) {
            for (String entityTypeKey : config.entityTypes()) {
                try {
                    EntityType entityType = EntityType.valueOf(entityTypeKey.toUpperCase(Locale.ENGLISH));
                    entityTypeTags.append(entityType);
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Filter error in {0}: No entity type matching {1}", filterName, entityTypeKey);
                }
            }

            conditionExists = true;
        }

        // Entity type tags
        if (!ListUtils.isNullOrEmpty(config.entityTypesTags())) {
            for (String entityTypeTag : config.entityTypesTags()) {
                var namespacedKey = NamespacedKey.fromString(entityTypeTag);
                if (namespacedKey != null) {
                    var tag = Bukkit.getTag("entity_types", namespacedKey, EntityType.class);
                    if (tag != null) {
                        conditionExists = true;
                        entityTypeTags.append(tag);

                        continue;
                    }
                }

                loggingService.warn("Filter error in {0}: Invalid entity type tag {1}", filterName, entityTypeTag);
            }
        }

        CustomTag<Material> materialTags = new CustomTag<>(Material.class);

        // Materials
        if (!ListUtils.isNullOrEmpty(config.materials())) {
            for (String materialKey : config.materials()) {
                try {
                    Material material = Material.valueOf(materialKey.toUpperCase(Locale.ENGLISH));
                    materialTags.append(material);
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Filter error in {0}: No material matching {1}", filterName, materialKey);
                }
            }

            conditionExists = true;
        }

        // Block material tags
        if (!ListUtils.isNullOrEmpty(config.blockTags())) {
            for (String blockTag : config.blockTags()) {
                var namespacedKey = NamespacedKey.fromString(blockTag);
                if (namespacedKey != null) {
                    var tag = Bukkit.getTag("blocks", namespacedKey, Material.class);
                    if (tag != null) {
                        conditionExists = true;
                        materialTags.append(tag);

                        continue;
                    }
                }

                loggingService.warn("Filter error in {0}: Invalid block tag {1}", filterName, blockTag);
            }
        }

        // Item material tags
        if (!ListUtils.isNullOrEmpty(config.itemTags())) {
            for (String itemTag : config.itemTags()) {
                var namespacedKey = NamespacedKey.fromString(itemTag);
                if (namespacedKey != null) {
                    var tag = Bukkit.getTag("items", namespacedKey, Material.class);

                    if (tag != null) {
                        conditionExists = true;
                        materialTags.append(tag);

                        continue;
                    }
                }

                loggingService.warn("Filter error in {0}: Invalid item tag {1}", filterName, itemTag);
            }
        }

        // Game modes
        List<GameMode> gameModes = new ArrayList<>();
        for (var gameModeString : config.player().gameModes()) {
            try {
                gameModes.add(GameMode.valueOf(gameModeString.toUpperCase(Locale.ENGLISH)));

                conditionExists = true;
            } catch (IllegalArgumentException e) {
                loggingService.warn("Filter error in {0}: Invalid game mode {1}", filterName, gameModeString);
            }
        }

        if (conditionExists) {
            var filter = new ActivityFilter(
                filterName,
                behavior,
                ListUtils.isNullOrEmpty(config.actions()) ? new ArrayList<>() : config.actions(),
                ListUtils.isNullOrEmpty(config.causes()) ? new ArrayList<>() : config.causes(),
                entityTypeTags,
                gameModes,
                materialTags,
                ListUtils.isNullOrEmpty(config.permissions()) ? new ArrayList<>() : config.permissions(),
                ListUtils.isNullOrEmpty(worldNames) ? new ArrayList<>() : worldNames
            );

            if (behavior.equals(FilterBehavior.ALLOW)) {
                allowFilters.add(filter);
            } else {
                ignoreFilters.add(filter);
            }

            loggingService.info("Loaded filters. Allow: {0}, Ignore: {1}", allowFilters.size(), ignoreFilters.size());
        } else {
            loggingService.warn("Filter error in {0}: Not enough conditions", filterName);
        }
    }

    /**
     * Pass an activity through filters. If any disallow it, reject.
     *
     * @param activity The activity
     * @return True if filters rejected the activity
     */
    public boolean shouldRecord(Activity activity) {
        // If ANY "IGNORE" filter rejects this activity, disallow recording and stop looking
        for (ActivityFilter filter : ignoreFilters) {
            if (!filter.shouldRecord(activity, loggingService, configurationService.prismConfig().debugFilters())) {
                return false;
            }
        }

        // If ANY "ALLOW" filter rejects this activity, we have to keep looking to ensure no others do
        for (ActivityFilter filter : allowFilters) {
            if (filter.shouldRecord(activity, loggingService, configurationService.prismConfig().debugFilters())) {
                return true;
            }
        }

        // If "ALLOW" filters exist, we have to deny this by default, otherwise we can allow.
        return allowFilters.isEmpty();
    }
}
