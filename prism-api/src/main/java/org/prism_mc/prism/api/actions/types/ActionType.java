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

package org.prism_mc.prism.api.actions.types;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ActionData;
import org.prism_mc.prism.api.actions.metadata.Metadata;

@Getter
@EqualsAndHashCode
public abstract class ActionType {

    /**
     * The key.
     */
    protected final String key;

    /**
     * The action result type.
     */
    protected final ActionResultType resultType;

    /**
     * Indicates whether this action type is usually reversible.
     */
    protected final boolean reversible;

    /**
     * The metadata.
     */
    protected final Metadata metadata;

    /**
     * Construct a new action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public ActionType(String key, ActionResultType resultType, boolean reversible) {
        this(key, resultType, reversible, null);
    }

    /**
     * Construct a new action type.
     *
     * @param key The key
     * @param metadata The metadata
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public ActionType(String key, ActionResultType resultType, boolean reversible, Metadata metadata) {
        this.key = key;
        this.metadata = metadata;
        this.resultType = resultType;
        this.reversible = reversible;
    }

    /**
     * Get the family key.
     *
     * @return The family key
     */
    public String familyKey() {
        String[] segments = key.split("-");
        return segments[segments.length - 1];
    }

    /**
     * Get the past tense translation key.
     *
     * @return The key
     */
    public String pastTenseTranslationKey() {
        return "prism.past-tense." + key;
    }

    /**
     * Create the action from this type.
     *
     * @param actionData The action data
     * @return The action
     * @throws Exception Exception
     */
    public abstract Action createAction(ActionData actionData) throws Exception;

    public String toString() {
        return String.format("ActionType{key=%s,resultType=%s,reversible=%s}", key, resultType, reversible);
    }
}
