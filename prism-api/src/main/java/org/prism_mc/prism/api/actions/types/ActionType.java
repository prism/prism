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
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ActionData;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.services.modifications.ModificationHandler;

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
     * Whether action type uses a descriptor.
     */
    protected final boolean usesDescriptor;

    /**
     * Whether activities of this type should be aggregated.
     */
    protected final boolean aggregatable;

    /**
     * Custom modification handler for rollback/restore logic.
     */
    @Nullable
    @EqualsAndHashCode.Exclude
    protected ModificationHandler modificationHandler;

    /**
     * Set a custom modification handler for rollback/restore logic.
     *
     * @param modificationHandler The handler, or null to use default logic
     */
    public void setModificationHandler(@Nullable ModificationHandler modificationHandler) {
        this.modificationHandler = modificationHandler;
    }

    /**
     * Construct a new action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public ActionType(String key, ActionResultType resultType, boolean reversible) {
        this(key, resultType, reversible, true, null, false);
    }

    /**
     * Construct a new action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     * @param usesDescriptor Whether the action uses a descriptor
     */
    public ActionType(String key, ActionResultType resultType, boolean reversible, boolean usesDescriptor) {
        this(key, resultType, reversible, usesDescriptor, null, false);
    }

    /**
     * Construct a new action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     * @param usesDescriptor Whether the action uses a descriptor
     * @param metadata The metadata
     */
    public ActionType(
        String key,
        ActionResultType resultType,
        boolean reversible,
        boolean usesDescriptor,
        Metadata metadata
    ) {
        this(key, resultType, reversible, usesDescriptor, metadata, false);
    }

    /**
     * Construct a new action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     * @param usesDescriptor Whether the action uses a descriptor
     * @param metadata The metadata
     * @param aggregatable Whether activities should be aggregated
     */
    public ActionType(
        String key,
        ActionResultType resultType,
        boolean reversible,
        boolean usesDescriptor,
        Metadata metadata,
        boolean aggregatable
    ) {
        this.key = key;
        this.metadata = metadata;
        this.resultType = resultType;
        this.reversible = reversible;
        this.usesDescriptor = usesDescriptor;
        this.aggregatable = aggregatable;
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
        return String.format(
            "ActionType{key=%s,resultType=%s,reversible=%s,usesDescriptor=%s}",
            key,
            resultType,
            reversible,
            usesDescriptor
        );
    }
}
