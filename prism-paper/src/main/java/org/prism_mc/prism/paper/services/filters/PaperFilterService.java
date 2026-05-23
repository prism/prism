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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.filters.FilterBehavior;
import org.prism_mc.prism.api.services.filters.FilterMode;
import org.prism_mc.prism.api.services.filters.FilterService;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConditionsConfiguration;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.utils.CustomTag;
import org.prism_mc.prism.paper.utils.ListUtils;
import org.prism_mc.prism.paper.utils.MaterialTagResolver;

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
     * All filters, in the order they were defined.
     */
    private final List<ActivityFilter> filters = new ArrayList<>();

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
        filters.clear();

        // Convert all configured filters into Filter objects
        for (FilterConfiguration config : configurationService.prismConfig().filters()) {
            var name = config.name() == null || config.name().isEmpty() ? "Unnamed" : config.name();

            if (!config.enabled()) {
                loggingService.info("Skipping disabled filter {0}", name);

                continue;
            }

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
            !ListUtils.isNullOrEmpty(config.excludedWorlds()) ||
            !ListUtils.isNullOrEmpty(config.permissions()) ||
            !ListUtils.isNullOrEmpty(config.actions()) ||
            !ListUtils.isNullOrEmpty(config.excludedActions()) ||
            !ListUtils.isNullOrEmpty(config.namedCauses()) ||
            !ListUtils.isNullOrEmpty(config.excludedNamedCauses())
        ) {
            conditionExists = true;
        }

        // Cause container types
        Set<CauseContainerType> causeTypes = parseCauseTypes(filterName, config.causeTypes());
        Set<CauseContainerType> causeTypesExcluded = parseCauseTypes(filterName, config.excludedCauseTypes());
        if (!causeTypes.isEmpty() || !causeTypesExcluded.isEmpty()) {
            conditionExists = true;
        }

        var affectedBlock = config.affectedBlock();
        var affectedBlockTags = loadMaterialTags(
            filterName,
            "blocks",
            affectedBlock == null ? null : affectedBlock.materials,
            affectedBlock == null ? null : affectedBlock.tags
        );
        var affectedBlockExcludedTags = loadMaterialTags(
            filterName,
            "blocks",
            affectedBlock == null ? null : affectedBlock.excludedMaterials,
            affectedBlock == null ? null : affectedBlock.excludedTags
        );
        if (!affectedBlockTags.isEmpty() || !affectedBlockExcludedTags.isEmpty()) {
            conditionExists = true;
        }

        var causeBlock = config.causeBlock();
        var causeBlockTags = loadMaterialTags(
            filterName,
            "blocks",
            causeBlock == null ? null : causeBlock.materials,
            causeBlock == null ? null : causeBlock.tags
        );
        var causeBlockExcludedTags = loadMaterialTags(
            filterName,
            "blocks",
            causeBlock == null ? null : causeBlock.excludedMaterials,
            causeBlock == null ? null : causeBlock.excludedTags
        );
        if (!causeBlockTags.isEmpty() || !causeBlockExcludedTags.isEmpty()) {
            conditionExists = true;
        }

        var affectedEntityType = config.affectedEntityType();
        var affectedEntityTypeTags = loadEntityTypeTags(
            filterName,
            affectedEntityType == null ? null : affectedEntityType.entityTypes,
            affectedEntityType == null ? null : affectedEntityType.tags
        );
        var affectedEntityTypeExcludedTags = loadEntityTypeTags(
            filterName,
            affectedEntityType == null ? null : affectedEntityType.excludedEntityTypes,
            affectedEntityType == null ? null : affectedEntityType.excludedTags
        );
        if (!affectedEntityTypeTags.isEmpty() || !affectedEntityTypeExcludedTags.isEmpty()) {
            conditionExists = true;
        }

        var causeEntityType = config.causeEntityType();
        var causeEntityTypeTags = loadEntityTypeTags(
            filterName,
            causeEntityType == null ? null : causeEntityType.entityTypes,
            causeEntityType == null ? null : causeEntityType.tags
        );
        var causeEntityTypeExcludedTags = loadEntityTypeTags(
            filterName,
            causeEntityType == null ? null : causeEntityType.excludedEntityTypes,
            causeEntityType == null ? null : causeEntityType.excludedTags
        );
        if (!causeEntityTypeTags.isEmpty() || !causeEntityTypeExcludedTags.isEmpty()) {
            conditionExists = true;
        }

        var item = config.item();
        var itemTags = loadMaterialTags(
            filterName,
            "items",
            item == null ? null : item.materials,
            item == null ? null : item.tags
        );
        var itemExcludedTags = loadMaterialTags(
            filterName,
            "items",
            item == null ? null : item.excludedMaterials,
            item == null ? null : item.excludedTags
        );
        if (!itemTags.isEmpty() || !itemExcludedTags.isEmpty()) {
            conditionExists = true;
        }

        // Game modes
        Set<GameMode> gameModes = EnumSet.noneOf(GameMode.class);
        if (config.causePlayer() != null && !ListUtils.isNullOrEmpty(config.causePlayer().gameModes())) {
            for (var gameModeString : config.causePlayer().gameModes()) {
                try {
                    gameModes.add(GameMode.valueOf(gameModeString.toUpperCase(Locale.ENGLISH)));

                    conditionExists = true;
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Filter error in {0}: Invalid game mode {1}", filterName, gameModeString);
                }
            }
        }

        // Player names
        var causePlayer = config.causePlayer();
        Set<String> causePlayerNames = lowercaseSet(causePlayer == null ? null : causePlayer.names());
        Set<String> causePlayerNamesExcluded = lowercaseSet(causePlayer == null ? null : causePlayer.excludedNames());
        if (!causePlayerNames.isEmpty() || !causePlayerNamesExcluded.isEmpty()) {
            conditionExists = true;
        }

        var affectedPlayer = config.affectedPlayer();
        Set<String> affectedPlayerNames = lowercaseSet(affectedPlayer == null ? null : affectedPlayer.names());
        Set<String> affectedPlayerNamesExcluded = lowercaseSet(
            affectedPlayer == null ? null : affectedPlayer.excludedNames()
        );
        if (!affectedPlayerNames.isEmpty() || !affectedPlayerNamesExcluded.isEmpty()) {
            conditionExists = true;
        }

        // Y bounds
        Integer above = config.above();
        Integer below = config.below();
        if (above != null || below != null) {
            conditionExists = true;
        }

        // Bounding box
        Coordinate boundsMin = null;
        Coordinate boundsMax = null;
        if (config.bounds() != null) {
            if (config.bounds().min != null && config.bounds().max != null) {
                var min = config.bounds().min;
                var max = config.bounds().max;
                boundsMin = new Coordinate(Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z));
                boundsMax = new Coordinate(Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z));
                conditionExists = true;
            } else {
                loggingService.warn("Filter error in {0}: bounds requires both min and max coordinates", filterName);
            }
        }

        // Radius
        Coordinate radiusCenter = null;
        Integer radiusDistance = null;
        if (config.radius() != null) {
            if (config.radius().center != null && config.radius().distance != null) {
                var center = config.radius().center;
                radiusCenter = new Coordinate(center.x, center.y, center.z);
                radiusDistance = config.radius().distance;
                conditionExists = true;
            } else {
                loggingService.warn("Filter error in {0}: radius requires both center and distance", filterName);
            }
        }

        if (conditionExists) {
            var filter = ActivityFilter.builder()
                .name(filterName)
                .behavior(behavior)
                .actions(
                    ListUtils.isNullOrEmpty(config.actions()) ? Collections.emptySet() : new HashSet<>(config.actions())
                )
                .actionsExcluded(
                    ListUtils.isNullOrEmpty(config.excludedActions())
                        ? Collections.emptySet()
                        : new HashSet<>(config.excludedActions())
                )
                .namedCauses(
                    ListUtils.isNullOrEmpty(config.namedCauses())
                        ? Collections.emptySet()
                        : new HashSet<>(config.namedCauses())
                )
                .namedCausesExcluded(
                    ListUtils.isNullOrEmpty(config.excludedNamedCauses())
                        ? Collections.emptySet()
                        : new HashSet<>(config.excludedNamedCauses())
                )
                .causeTypes(causeTypes)
                .causeTypesExcluded(causeTypesExcluded)
                .affectedBlockTags(affectedBlockTags)
                .affectedBlockExcludedTags(affectedBlockExcludedTags)
                .causeBlockTags(causeBlockTags)
                .causeBlockExcludedTags(causeBlockExcludedTags)
                .affectedEntityTypeTags(affectedEntityTypeTags)
                .affectedEntityTypeExcludedTags(affectedEntityTypeExcludedTags)
                .causeEntityTypeTags(causeEntityTypeTags)
                .causeEntityTypeExcludedTags(causeEntityTypeExcludedTags)
                .gameModes(gameModes)
                .itemTags(itemTags)
                .itemExcludedTags(itemExcludedTags)
                .permissions(
                    ListUtils.isNullOrEmpty(config.permissions())
                        ? Collections.emptySet()
                        : new HashSet<>(config.permissions())
                )
                .worldNames(ListUtils.isNullOrEmpty(worldNames) ? Collections.emptySet() : new HashSet<>(worldNames))
                .worldNamesExcluded(
                    ListUtils.isNullOrEmpty(config.excludedWorlds())
                        ? Collections.emptySet()
                        : new HashSet<>(config.excludedWorlds())
                )
                .causePlayerNames(causePlayerNames)
                .causePlayerNamesExcluded(causePlayerNamesExcluded)
                .affectedPlayerNames(affectedPlayerNames)
                .affectedPlayerNamesExcluded(affectedPlayerNamesExcluded)
                .above(above)
                .below(below)
                .boundsMin(boundsMin)
                .boundsMax(boundsMax)
                .radiusCenter(radiusCenter)
                .radiusDistance(radiusDistance)
                .build();

            filters.add(filter);

            loggingService.info("Loaded filter {0} ({1}). Total filters: {2}", filterName, behavior, filters.size());
        } else {
            loggingService.warn("Filter error in {0}: Not enough conditions", filterName);
        }
    }

    /**
     * Pass an activity through the configured filters to decide whether it should be recorded.
     *
     * @param activity The activity
     * @return True if the activity should be recorded
     */
    public boolean shouldRecord(Activity activity) {
        var debug = configurationService.prismConfig().debugFilters();

        if (configurationService.prismConfig().filterMode() == FilterMode.ORDERED) {
            // First filter whose conditions match decides the outcome.
            for (ActivityFilter filter : filters) {
                if (filter.matches(activity, loggingService, debug)) {
                    return filter.behavior() == FilterBehavior.ALLOW;
                }
            }

            // Nothing matched, record by default.
            return true;
        }

        // GROUPED: any matching IGNORE filter drops the activity.
        for (ActivityFilter filter : filters) {
            if (filter.behavior() == FilterBehavior.IGNORE && filter.matches(activity, loggingService, debug)) {
                return false;
            }
        }

        // If any ALLOW filter exists, only an explicit match allows recording; otherwise deny.
        boolean allowFilterExists = false;
        for (ActivityFilter filter : filters) {
            if (filter.behavior() == FilterBehavior.ALLOW) {
                allowFilterExists = true;

                if (filter.matches(activity, loggingService, debug)) {
                    return true;
                }
            }
        }

        return !allowFilterExists;
    }

    /**
     * Parse a list of cause type names into a set, warning on any invalid value.
     *
     * @param filterName The filter name
     * @param values The cause type names
     * @return The parsed set, empty if the input is null or empty
     */
    private Set<CauseContainerType> parseCauseTypes(String filterName, List<String> values) {
        Set<CauseContainerType> result = EnumSet.noneOf(CauseContainerType.class);

        if (!ListUtils.isNullOrEmpty(values)) {
            for (var value : values) {
                try {
                    result.add(CauseContainerType.valueOf(value.toUpperCase(Locale.ENGLISH)));
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Filter error in {0}: Invalid cause type {1}", filterName, value);
                }
            }
        }

        return result;
    }

    /**
     * Convert a list of strings to a lowercased set, for case-insensitive matching.
     *
     * @param values The values
     * @return A lowercased set, empty if the input is null or empty
     */
    private Set<String> lowercaseSet(List<String> values) {
        if (ListUtils.isNullOrEmpty(values)) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        for (String value : values) {
            result.add(value.toLowerCase(Locale.ENGLISH));
        }

        return result;
    }

    /**
     * Load entity type tags.
     *
     * @param filterName Filter name
     * @param entityTypeKeys The entity type names
     * @param tagKeys The entity type tag keys
     * @return Tags
     */
    private CustomTag<EntityType> loadEntityTypeTags(
        String filterName,
        List<String> entityTypeKeys,
        List<String> tagKeys
    ) {
        var tags = new CustomTag<>(EntityType.class);

        if (!ListUtils.isNullOrEmpty(entityTypeKeys)) {
            for (var entityTypeKey : entityTypeKeys) {
                try {
                    EntityType entityType = EntityType.valueOf(entityTypeKey.toUpperCase(Locale.ENGLISH));
                    tags.append(entityType);
                } catch (IllegalArgumentException e) {
                    loggingService.warn("Filter error in {0}: No entity type matching {1}", filterName, entityTypeKey);
                }
            }
        }

        if (!ListUtils.isNullOrEmpty(tagKeys)) {
            for (String entityTypeTag : tagKeys) {
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

        return tags;
    }

    /**
     * Load material tags.
     *
     * @param filterName Filter name
     * @param tagKey The registry tag key (blocks or items)
     * @param materialKeys The material names
     * @param tagKeys The material tag keys
     * @return Tags
     */
    private CustomTag<Material> loadMaterialTags(
        String filterName,
        String tagKey,
        List<String> materialKeys,
        List<String> tagKeys
    ) {
        var result = MaterialTagResolver.resolve(materialKeys, tagKeys, tagKey);

        for (String material : result.invalidMaterials()) {
            loggingService.warn("Filter error in {0}: No material matching {1}", filterName, material);
        }

        for (String itemTag : result.invalidTags()) {
            loggingService.warn("Filter error in {0}: Invalid tag {1}", filterName, itemTag);
        }

        return result.tags();
    }
}
