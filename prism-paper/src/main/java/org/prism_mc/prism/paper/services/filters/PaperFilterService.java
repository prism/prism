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

package org.prism_mc.prism.paper.services.filters;

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
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.filters.EntityTypeFilterConditionConfiguration;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConditionsConfiguration;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConfiguration;
import org.prism_mc.prism.loader.services.configuration.filters.MaterialTagFilterConditionConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.utils.CustomTag;
import org.prism_mc.prism.paper.utils.ListUtils;

@Singleton
public class PaperFilterService implements FilterService {

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
    public PaperFilterService(LoggingService loggingService, ConfigurationService configurationService) {
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

        var affectedBlockTags = loadMaterialTags(filterName, "blocks", config.affectedBlock());
        if (!affectedBlockTags.isEmpty()) {
            conditionExists = true;
        }

        var causeBlockTags = loadMaterialTags(filterName, "blocks", config.causeBlock());
        if (!causeBlockTags.isEmpty()) {
            conditionExists = true;
        }

        var affectedEntityTypeTags = loadEntityTypeTags(filterName, config.affectedEntityType());
        if (!affectedEntityTypeTags.isEmpty()) {
            conditionExists = true;
        }

        var causeEntityTypeTags = loadEntityTypeTags(filterName, config.causeEntityType());
        if (!causeEntityTypeTags.isEmpty()) {
            conditionExists = true;
        }

        var itemTags = loadMaterialTags(filterName, "items", config.item());
        if (!itemTags.isEmpty()) {
            conditionExists = true;
        }

        // Game modes
        List<GameMode> gameModes = new ArrayList<>();
        if (config.causePlayer() != null) {
            for (var gameModeString : config.causePlayer().gameModes()) {
                try {
                    gameModes.add(GameMode.valueOf(gameModeString.toUpperCase(Locale.ENGLISH)));

                    conditionExists = true;
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Filter error in {0}: Invalid game mode {1}", filterName, gameModeString);
                }
            }
        }

        if (conditionExists) {
            var filter = new ActivityFilter(
                filterName,
                behavior,
                ListUtils.isNullOrEmpty(config.actions()) ? new ArrayList<>() : config.actions(),
                ListUtils.isNullOrEmpty(config.namedCauses()) ? new ArrayList<>() : config.namedCauses(),
                affectedBlockTags,
                causeBlockTags,
                affectedEntityTypeTags,
                causeEntityTypeTags,
                gameModes,
                itemTags,
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

    /**
     * Load cause entity type tags.
     *
     * @param filterName Filter name
     * @param config Filter config
     * @return Tags
     */
    private CustomTag<EntityType> loadEntityTypeTags(String filterName, EntityTypeFilterConditionConfiguration config) {
        var tags = new CustomTag<>(EntityType.class);

        if (config != null) {
            if (!ListUtils.isNullOrEmpty(config.entityTypes)) {
                for (var entityTypeKey : config.entityTypes) {
                    try {
                        EntityType entityType = EntityType.valueOf(entityTypeKey.toUpperCase(Locale.ENGLISH));
                        tags.append(entityType);
                    } catch (IllegalArgumentException e) {
                        loggingService.warn(
                            "Filter error in {0}: No entity type matching {1}",
                            filterName,
                            entityTypeKey
                        );
                    }
                }
            }

            if (!ListUtils.isNullOrEmpty(config.tags)) {
                for (String entityTypeTag : config.tags) {
                    var namespacedKey = NamespacedKey.fromString(entityTypeTag);
                    if (namespacedKey != null) {
                        var tag = Bukkit.getTag("entity_types", namespacedKey, EntityType.class);
                        if (tag != null) {
                            tags.append(tag);

                            continue;
                        }
                    }

                    loggingService.warn("Filter error in {0}: Invalid entity type tag {1}", filterName, entityTypeTag);
                }
            }
        }

        return tags;
    }

    /**
     * Load material tags.
     *
     * @param filterName Filter name
     * @param config Filter config
     * @return Tags
     */
    private CustomTag<Material> loadMaterialTags(
        String filterName,
        String tagKey,
        MaterialTagFilterConditionConfiguration config
    ) {
        CustomTag<Material> tags = new CustomTag<>(Material.class);

        if (config != null) {
            if (!ListUtils.isNullOrEmpty(config.materials)) {
                for (String materialKey : config.materials) {
                    try {
                        Material material = Material.valueOf(materialKey.toUpperCase(Locale.ENGLISH));
                        tags.append(material);
                    } catch (IllegalArgumentException e) {
                        loggingService.warn("Filter error in {0}: No material matching {1}", filterName, materialKey);
                    }
                }
            }

            if (!ListUtils.isNullOrEmpty(config.tags)) {
                for (String itemTag : config.tags) {
                    var namespacedKey = NamespacedKey.fromString(itemTag);
                    if (namespacedKey != null) {
                        var tag = Bukkit.getTag(tagKey, namespacedKey, Material.class);

                        if (tag != null) {
                            tags.append(tag);

                            continue;
                        }
                    }

                    loggingService.warn("Filter error in {0}: Invalid tag {1}", filterName, itemTag);
                }
            }
        }

        return tags;
    }
}
