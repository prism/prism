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

package org.prism_mc.prism.api.services.modifications;

import org.prism_mc.prism.api.activities.Activity;

/**
 * Handler for rollback and restore logic.
 *
 * <p>Prism's built-in actions implement this interface for their default
 * rollback/restore behavior. Third-party plugins can also implement it
 * to define custom reversal logic for their action types.</p>
 */
public interface ModificationHandler {
    /**
     * Apply rollback logic for an activity.
     *
     * @param modificationRuleset The modification ruleset
     * @param owner The owner of this modification
     * @param activityContext The activity to roll back
     * @param mode The modification mode (planning/completing)
     * @return The modification result
     */
    ModificationResult applyRollback(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    );

    /**
     * Apply restore logic for an activity.
     *
     * @param modificationRuleset The modification ruleset
     * @param owner The owner of this modification
     * @param activityContext The activity to restore
     * @param mode The modification mode (planning/completing)
     * @return The modification result
     */
    ModificationResult applyRestore(
        ModificationRuleset modificationRuleset,
        Object owner,
        Activity activityContext,
        ModificationQueueMode mode
    );
}
