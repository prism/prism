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

import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.api.services.filters.FilterBehavior;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.actions.PaperBlockAction;
import org.prism_mc.prism.paper.actions.PaperEntityAction;
import org.prism_mc.prism.paper.actions.PaperMaterialAction;
import org.prism_mc.prism.paper.api.containers.PaperBlockContainer;
import org.prism_mc.prism.paper.api.containers.PaperEntityContainer;
import org.prism_mc.prism.paper.api.containers.PaperPlayerContainer;
import org.prism_mc.prism.paper.utils.CustomTag;

public class ActivityFilter {

    /**
     * The filter name.
     */
    private final String name;

    /**
     * Actions.
     */
    private final Set<String> actions;

    /**
     * The behavior of this filter.
     */
    private final FilterBehavior behavior;

    /**
     * Causes.
     */
    private final Set<String> namedCauses;

    /**
     * The affected block tags.
     */
    private final CustomTag<Material> affectedBlockTags;

    /**
     * The cause block tags.
     */
    private final CustomTag<Material> causeBlockTags;

    /**
     * The affected entity type tag (entity types, entity types tags).
     */
    private final CustomTag<EntityType> affectedEntityTypeTags;

    /**
     * The cause entity type tag (entity types, entity types tags).
     */
    private final CustomTag<EntityType> causeEntityTypeTags;

    /**
     * The player's game mode(s).
     */
    private final Set<GameMode> gameModes;

    /**
     * The item tags.
     */
    private final CustomTag<Material> itemTags;

    /**
     * All permissions.
     */
    private final Set<String> permissions;

    /**
     * All world names.
     */
    private final Set<String> worldNames;

    /**
     * Construct a new activity filter.
     *
     * @param name The filter name
     * @param behavior The behavior
     * @param actions The actions
     * @param namedCauses The namedCauses
     * @param affectedBlockTags The affected block tags
     * @param causeBlockTags The cause block tags
     * @param affectedEntityTypeTags The affected entity type tags
     * @param causeEntityTypeTags The cause entity type tags
     * @param gameModes The game modes
     * @param itemTags The item tags
     * @param permissions The permissions
     * @param worldNames The world names
     */
    public ActivityFilter(
        @NotNull String name,
        @NotNull FilterBehavior behavior,
        @NotNull Set<String> actions,
        @NotNull Set<String> namedCauses,
        @NotNull CustomTag<Material> affectedBlockTags,
        @NotNull CustomTag<Material> causeBlockTags,
        @NotNull CustomTag<EntityType> affectedEntityTypeTags,
        @NotNull CustomTag<EntityType> causeEntityTypeTags,
        @NotNull Set<GameMode> gameModes,
        @NotNull CustomTag<Material> itemTags,
        @NotNull Set<String> permissions,
        @NotNull Set<String> worldNames
    ) {
        this.name = name;
        this.actions = actions;
        this.behavior = behavior;
        this.namedCauses = namedCauses;
        this.affectedBlockTags = affectedBlockTags;
        this.causeBlockTags = causeBlockTags;
        this.affectedEntityTypeTags = affectedEntityTypeTags;
        this.causeEntityTypeTags = causeEntityTypeTags;
        this.itemTags = itemTags;
        this.gameModes = gameModes;
        this.permissions = permissions;
        this.worldNames = worldNames;
    }

    /**
     * Check if this filter allows the activity.
     *
     * @param activity The activity
     * @param loggingService The logging service
     * @param debug Whether filters are in debug mode
     * @return True if the filter allows it
     */
    public boolean shouldRecord(Activity activity, LoggingService loggingService, boolean debug) {
        if (debug) {
            loggingService.debug("Filter ({0}) Check for Activity: {1}", name, activity);
            loggingService.debug("Behavior: {0}", behavior);
        }

        int total = 0;
        int matched = 0;
        int notMatched = 0;

        var actionResult = actionsMatch(activity);
        total++;
        if (actionResult == ConditionResult.MATCHED) {
            matched++;
        } else if (actionResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !actions.isEmpty()) {
            loggingService.debug("Action result: {0}", actionResult);
        }

        var namedCauseResult = namedCausesMatch(activity);
        total++;
        if (namedCauseResult == ConditionResult.MATCHED) {
            matched++;
        } else if (namedCauseResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !namedCauses.isEmpty()) {
            loggingService.debug("Named Cause result: {0}", namedCauseResult);
        }

        var affectedBlockResult = affectedBlocksMatched(activity);
        total++;
        if (affectedBlockResult == ConditionResult.MATCHED) {
            matched++;
        } else if (affectedBlockResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !affectedBlockTags.isEmpty()) {
            loggingService.debug("Affected Blocks result: {0}", affectedBlockResult);
        }

        var causeBlockResult = causeBlocksMatched(activity);
        total++;
        if (causeBlockResult == ConditionResult.MATCHED) {
            matched++;
        } else if (causeBlockResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !causeBlockTags.isEmpty()) {
            loggingService.debug("Cause Blocks result: {0}", causeBlockResult);
        }

        var affectedEntityTypeResult = affectedEntityTypesMatched(activity);
        total++;
        if (affectedEntityTypeResult == ConditionResult.MATCHED) {
            matched++;
        } else if (affectedEntityTypeResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !affectedEntityTypeTags.isEmpty()) {
            loggingService.debug("Affected Entity Type result: {0}", affectedEntityTypeResult);
        }

        var causeEntityTypeResult = causeEntityTypesMatched(activity);
        total++;
        if (causeEntityTypeResult == ConditionResult.MATCHED) {
            matched++;
        } else if (causeEntityTypeResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !causeEntityTypeTags.isEmpty()) {
            loggingService.debug("Cause Entity Type result: {0}", causeEntityTypeResult);
        }

        var gameModeResult = gameModesMatched(activity);
        total++;
        if (gameModeResult == ConditionResult.MATCHED) {
            matched++;
        } else if (gameModeResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !gameModes.isEmpty()) {
            loggingService.debug("Game mode result: {0}", gameModeResult);
        }

        var itemsResult = itemsMatched(activity);
        total++;
        if (itemsResult == ConditionResult.MATCHED) {
            matched++;
        } else if (itemsResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !itemTags.isEmpty()) {
            loggingService.debug("Items result: {0}", itemsResult);
        }

        var permissionResult = permissionsMatch(activity);
        total++;
        if (permissionResult == ConditionResult.MATCHED) {
            matched++;
        } else if (permissionResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !permissions.isEmpty()) {
            loggingService.debug("Permission result: {0}", permissionResult);
        }

        var worldsResult = worldsMatch(activity);
        total++;
        if (worldsResult == ConditionResult.MATCHED) {
            matched++;
        } else if (worldsResult == ConditionResult.NOT_MATCHED) {
            notMatched++;
        }
        if (debug && !worldNames.isEmpty()) {
            loggingService.debug("Worlds result: {0}", worldsResult);
        }

        int nonApplicable = total - matched - notMatched;

        var finalDecision = getFinalDecision(total, nonApplicable, matched, notMatched, loggingService, debug);
        if (debug) {
            loggingService.debug("Final decision: {0}", finalDecision);
        }

        return finalDecision;
    }

    /**
     * Make a final decision.
     *
     * @param total Total number of conditions
     * @param nonApplicable Count of non-applicable conditions
     * @param matched Count of matched conditions
     * @param notMatched Count of not-matched conditions
     * @param loggingService The logging service
     * @param debug Whether debug mode is enabled
     * @return The decision
     */
    private boolean getFinalDecision(
        int total,
        int nonApplicable,
        int matched,
        int notMatched,
        LoggingService loggingService,
        boolean debug
    ) {
        if (debug) {
            loggingService.debug(
                "All: {0}; Not Applicable: {1}; Matched: {2}; Not Matched: {3}",
                total,
                nonApplicable,
                matched,
                notMatched
            );
        }

        // No filters applied, allow
        if (nonApplicable == total) {
            return true;
        }

        // If ignoring and all applicable conditions matched, reject it
        if (ignoring() && matched > 0 && notMatched == 0) {
            if (debug) {
                loggingService.debug("Rejecting because we're ignoring and all applicable rules matched");
            }

            return false;
        }

        // If the filter is ALLOW but something didn't match, reject it
        if (allowing() && notMatched > 0) {
            if (debug) {
                loggingService.debug("Rejecting because we're allowing and one or more rules did not match");
            }

            return false;
        }

        return true;
    }

    /**
     * Check if filter mode is "allow".
     *
     * @return True if mode is "allow"
     */
    private boolean allowing() {
        return behavior == FilterBehavior.ALLOW;
    }

    /**
     * Check if filter mode is "ignore".
     *
     * @return True if mode is "ignore"
     */
    private boolean ignoring() {
        return behavior == FilterBehavior.IGNORE;
    }

    /**
     * Check if any actions match the activity action.
     *
     * <p>If none listed, the filter will match all.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult actionsMatch(Activity activity) {
        if (actions.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (actions.contains(activity.action().type().key())) {
            return ConditionResult.MATCHED;
        }

        return ConditionResult.NOT_MATCHED;
    }

    /**
     * Check if any named causes match the activity action.
     *
     * <p>If none listed, the filter will match all.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult namedCausesMatch(Activity activity) {
        if (namedCauses.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (
            activity.cause().container() instanceof StringContainer stringContainer &&
            namedCauses.contains(stringContainer.value())
        ) {
            return ConditionResult.MATCHED;
        }

        return ConditionResult.NOT_MATCHED;
    }

    /**
     * Check if any affected blocks match the activity action.
     *
     * <p>If none listed, the filter will match all. Ignores non-material actions.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult affectedBlocksMatched(Activity activity) {
        if (affectedBlockTags.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.action() instanceof PaperBlockAction blockAction) {
            if (affectedBlockTags.isTagged(blockAction.blockContainer().blockData().getMaterial())) {
                return ConditionResult.MATCHED;
            }

            return ConditionResult.NOT_MATCHED;
        } else {
            return ConditionResult.NOT_APPLICABLE;
        }
    }

    /**
     * Check if any cause blocks match the activity action.
     *
     * <p>If none listed, the filter will match all. Ignores non-material actions.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult causeBlocksMatched(Activity activity) {
        if (causeBlockTags.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.cause().container() instanceof PaperBlockContainer blockContainer) {
            if (causeBlockTags.isTagged(blockContainer.blockData().getMaterial())) {
                return ConditionResult.MATCHED;
            }

            return ConditionResult.NOT_MATCHED;
        } else {
            return ConditionResult.NOT_APPLICABLE;
        }
    }

    /**
     * Check if any entity types match the activity action.
     *
     * <p>If none listed, the filter will match all. Ignores non-entity actions.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult affectedEntityTypesMatched(Activity activity) {
        if (affectedEntityTypeTags.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.action() instanceof PaperEntityAction entityAction) {
            if (affectedEntityTypeTags.isTagged(entityAction.entityContainer().entityType())) {
                return ConditionResult.MATCHED;
            }

            return ConditionResult.NOT_MATCHED;
        } else {
            return ConditionResult.NOT_APPLICABLE;
        }
    }

    /**
     * Check if any entity types match the activity action.
     *
     * <p>If none listed, the filter will match all. Ignores non-entity actions.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult causeEntityTypesMatched(Activity activity) {
        if (causeEntityTypeTags.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.cause().container() instanceof PaperEntityContainer entityContainer) {
            if (causeEntityTypeTags.isTagged(entityContainer.entityType())) {
                return ConditionResult.MATCHED;
            }

            return ConditionResult.NOT_MATCHED;
        } else {
            return ConditionResult.NOT_APPLICABLE;
        }
    }

    /**
     * Check if any game modes match the activity action.
     *
     * <p>If none listed, the filter will match all. Ignores non-player actions.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult gameModesMatched(Activity activity) {
        if (gameModes.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.cause().container() instanceof PaperPlayerContainer paperPlayerContainer) {
            if (gameModes.contains(paperPlayerContainer.player().getGameMode())) {
                return ConditionResult.MATCHED;
            }

            return ConditionResult.NOT_MATCHED;
        } else {
            return ConditionResult.NOT_APPLICABLE;
        }
    }

    /**
     * Check if any materials match the activity action.
     *
     * <p>If none listed, the filter will match all. Ignores non-material actions.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult itemsMatched(Activity activity) {
        if (itemTags.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.action() instanceof PaperMaterialAction materialAction) {
            if (itemTags.isTagged(materialAction.material())) {
                return ConditionResult.MATCHED;
            }

            return ConditionResult.NOT_MATCHED;
        } else {
            return ConditionResult.NOT_APPLICABLE;
        }
    }

    /**
     * Check if any permissions match a player in the activity.
     *
     * <p>If none listed, the filter will match all.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult permissionsMatch(Activity activity) {
        if (permissions.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (activity.cause().container() instanceof PaperPlayerContainer paperPlayerContainer) {
            var player = paperPlayerContainer.player();
            if (player != null) {
                for (String permission : permissions) {
                    if (player.hasPermission(permission)) {
                        return ConditionResult.MATCHED;
                    }
                }
            }
        }

        return ConditionResult.NOT_MATCHED;
    }

    /**
     * Check if any worlds match the activity.
     *
     * <p>If none listed, the filter will match all.</p>
     *
     * @param activity The activity
     * @return ConditionResult
     */
    private ConditionResult worldsMatch(Activity activity) {
        if (worldNames.isEmpty()) {
            return ConditionResult.NOT_APPLICABLE;
        }

        if (worldNames.contains(activity.world().value())) {
            return ConditionResult.MATCHED;
        }

        return ConditionResult.NOT_MATCHED;
    }
}
