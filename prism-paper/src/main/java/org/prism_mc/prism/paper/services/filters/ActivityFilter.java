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

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
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
    private final List<String> actions;

    /**
     * The behavior of this filter.
     */
    private final FilterBehavior behavior;

    /**
     * Causes.
     */
    private final List<String> namedCauses;

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
    private final List<GameMode> gameModes;

    /**
     * The item tags.
     */
    private final CustomTag<Material> itemTags;

    /**
     * All permissions.
     */
    private final List<String> permissions;

    /**
     * All world names.
     */
    private final List<String> worldNames;

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
        @NotNull List<String> actions,
        @NotNull List<String> namedCauses,
        @NotNull CustomTag<Material> affectedBlockTags,
        @NotNull CustomTag<Material> causeBlockTags,
        @NotNull CustomTag<EntityType> affectedEntityTypeTags,
        @NotNull CustomTag<EntityType> causeEntityTypeTags,
        @NotNull List<GameMode> gameModes,
        @NotNull CustomTag<Material> itemTags,
        @NotNull List<String> permissions,
        @NotNull List<String> worldNames
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
        List<ConditionResult> results = new ArrayList<>();

        if (debug) {
            loggingService.debug("Filter ({0}) Check for Activity: {1}", name, activity);
            loggingService.debug("Behavior: {0}", behavior);
        }

        var actionResult = actionsMatch(activity);
        results.add(actionResult);
        if (debug && !actions.isEmpty()) {
            loggingService.debug("Action result: {0}", actionResult);
        }

        var namedCauseResult = namedCausesMatch(activity);
        results.add(namedCauseResult);
        if (debug && !namedCauses.isEmpty()) {
            loggingService.debug("Named Cause result: {0}", namedCauseResult);
        }

        var affectedBlockResult = affectedBlocksMatched(activity);
        results.add(affectedBlockResult);
        if (debug && !affectedBlockTags.isEmpty()) {
            loggingService.debug("Affected Blocks result: {0}", affectedBlockResult);
        }

        var causeBlockResult = causeBlocksMatched(activity);
        results.add(causeBlockResult);
        if (debug && !causeBlockTags.isEmpty()) {
            loggingService.debug("Cause Blocks result: {0}", causeBlockResult);
        }

        var affectedEntityTypeResult = affectedEntityTypesMatched(activity);
        results.add(affectedEntityTypeResult);
        if (debug && !affectedEntityTypeTags.isEmpty()) {
            loggingService.debug("Affected Entity Type result: {0}", affectedEntityTypeResult);
        }

        var causeEntityTypeResult = causeEntityTypesMatched(activity);
        results.add(causeEntityTypeResult);
        if (debug && !causeEntityTypeTags.isEmpty()) {
            loggingService.debug("Cause Entity Type result: {0}", causeEntityTypeResult);
        }

        var gameModeResult = gameModesMatched(activity);
        results.add(gameModeResult);
        if (debug && !gameModes.isEmpty()) {
            loggingService.debug("Game mode result: {0}", gameModeResult);
        }

        var itemsResult = itemsMatched(activity);
        results.add(itemsResult);
        if (debug && !itemTags.isEmpty()) {
            loggingService.debug("Items result: {0}", itemsResult);
        }

        var permissionResult = permissionsMatch(activity);
        results.add(permissionResult);
        if (debug && !permissions.isEmpty()) {
            loggingService.debug("Permission result: {0}", permissionResult);
        }

        var worldsResult = worldsMatch(activity);
        results.add(worldsResult);
        if (debug && !worldNames.isEmpty()) {
            loggingService.debug("Worlds result: {0}", worldsResult);
        }

        var finalDecision = getFinalDecision(results, loggingService, debug);
        if (debug) {
            loggingService.debug("Final decision: {0}", finalDecision);
        }

        return finalDecision;
    }

    /**
     * Make a final decision.
     *
     * @param results All filter condition results
     * @return The decision
     */
    private boolean getFinalDecision(List<ConditionResult> results, LoggingService loggingService, boolean debug) {
        int nonApplicable = 0;
        int matched = 0;
        int notMatched = 0;

        // Count each result type
        for (ConditionResult result : results) {
            if (result.equals(ConditionResult.NOT_APPLICABLE)) {
                nonApplicable++;
            } else if (result.equals(ConditionResult.NOT_MATCHED)) {
                notMatched++;
            } else if (result.equals(ConditionResult.MATCHED)) {
                matched++;
            }
        }

        // Check if all were non-applicable
        var allNotApplicable = nonApplicable == results.size();

        if (debug) {
            loggingService.debug(
                "All: {0}; Not Applicable: {1}; Matched: {2}; Not Matched: {3}",
                results.size(),
                nonApplicable,
                matched,
                notMatched
            );
        }

        // No filters applied, allow
        if (allNotApplicable) {
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
        return behavior.equals(FilterBehavior.ALLOW);
    }

    /**
     * Check if filter mode is "ignore".
     *
     * @return True if mode is "ignore"
     */
    private boolean ignoring() {
        return behavior.equals(FilterBehavior.IGNORE);
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
            var player = Bukkit.getServer().getPlayer(paperPlayerContainer.name());
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
