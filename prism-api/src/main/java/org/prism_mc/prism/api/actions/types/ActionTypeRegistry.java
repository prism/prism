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

import java.util.Collection;
import java.util.Optional;
import org.prism_mc.prism.api.services.modifications.ModificationHandler;

public interface ActionTypeRegistry {
    /**
     * Get all registered action types.
     *
     * @return All registered action types
     */
    Collection<ActionType> actionTypes();

    /**
     * Get all action types that belong to a family.
     *
     * @param family The family
     * @return All matching action types
     */
    Collection<ActionType> actionTypesInFamily(String family);

    /**
     * Get an action type by key.
     *
     * @param key The key
     * @return The action type, if any
     */
    Optional<ActionType> actionType(String key);

    /**
     * Register a new action type.
     *
     * @param actionType The action type
     */
    void registerAction(ActionType actionType);

    /**
     * Register a new generic (non-reversible, descriptor-only) action type.
     *
     * @param key The unique action type key (e.g. "myplugin-event")
     * @return The registered action type
     */
    ActionType registerGenericAction(String key);

    /**
     * Register a new block action type.
     *
     * @param key The unique action type key (e.g. "myplugin-break")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @return The registered action type
     */
    ActionType registerBlockAction(String key, ActionResultType resultType, boolean reversible);

    /**
     * Register a new block action type with a custom modification handler.
     *
     * @param key The unique action type key (e.g. "myplugin-break")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @param handler Custom handler for rollback/restore logic
     * @return The registered action type
     */
    ActionType registerBlockAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    );

    /**
     * Register a new entity action type.
     *
     * @param key The unique action type key (e.g. "myplugin-spawn")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @return The registered action type
     */
    ActionType registerEntityAction(String key, ActionResultType resultType, boolean reversible);

    /**
     * Register a new entity action type with a custom modification handler.
     *
     * @param key The unique action type key (e.g. "myplugin-spawn")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @param handler Custom handler for rollback/restore logic
     * @return The registered action type
     */
    ActionType registerEntityAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    );

    /**
     * Register a new item action type.
     *
     * @param key The unique action type key (e.g. "myplugin-craft")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @return The registered action type
     */
    ActionType registerItemAction(String key, ActionResultType resultType, boolean reversible);

    /**
     * Register a new item action type with a custom modification handler.
     *
     * @param key The unique action type key (e.g. "myplugin-craft")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @param handler Custom handler for rollback/restore logic
     * @return The registered action type
     */
    ActionType registerItemAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    );

    /**
     * Register a new player action type.
     *
     * @param key The unique action type key (e.g. "myplugin-action")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @return The registered action type
     */
    ActionType registerPlayerAction(String key, ActionResultType resultType, boolean reversible);

    /**
     * Register a new player action type with a custom modification handler.
     *
     * @param key The unique action type key (e.g. "myplugin-action")
     * @param resultType The result type (CREATES, REMOVES, REPLACES, or NONE)
     * @param reversible Whether this action supports rollback/restore
     * @param handler Custom handler for rollback/restore logic
     * @return The registered action type
     */
    ActionType registerPlayerAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    );
}
