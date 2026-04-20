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

package org.prism_mc.prism.core.actions.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.api.services.modifications.ModificationHandler;

public abstract class AbstractActionTypeRegistry implements ActionTypeRegistry {

    /**
     * Cache of action types by key.
     */
    private final Map<String, ActionType> actionsTypes = new HashMap<>();

    @Override
    public Collection<ActionType> actionTypes() {
        return actionsTypes.values();
    }

    @Override
    public Collection<ActionType> actionTypesInFamily(String family) {
        return actionsTypes
            .values()
            .stream()
            .filter(a -> a.familyKey().equalsIgnoreCase(family))
            .collect(Collectors.toList());
    }

    @Override
    public void registerAction(ActionType actionType) {
        if (actionsTypes.containsKey(actionType.key())) {
            throw new IllegalArgumentException("Registry already has an action type with that key.");
        }

        actionsTypes.put(actionType.key(), actionType);
    }

    @Override
    public Optional<ActionType> actionType(String key) {
        if (actionsTypes.containsKey(key)) {
            return Optional.of(actionsTypes.get(key));
        }

        return Optional.empty();
    }

    /**
     * Validate an action type key.
     *
     * @param key The key to validate
     */
    private void validateKey(String key) {
        if (key == null || !key.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?")) {
            throw new IllegalArgumentException(
                "Action type key must be non-null, lowercase alphanumeric with optional hyphens, " +
                "and must not start or end with a hyphen. Got: " +
                key
            );
        }
    }

    @Override
    public ActionType registerGenericAction(String key) {
        validateKey(key);

        ActionType actionType = createGenericActionType(key, null);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerGenericAction(String key, String pastTense) {
        validateKey(key);

        ActionType actionType = createGenericActionType(key, pastTense);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerBlockAction(String key, ActionResultType resultType, boolean reversible) {
        return registerBlockAction(key, resultType, reversible, (ModificationHandler) null);
    }

    @Override
    public ActionType registerBlockAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createBlockActionType(key, resultType, reversible, pastTense);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerBlockAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    ) {
        validateKey(key);

        ActionType actionType = createBlockActionType(key, resultType, reversible, null);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerBlockAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createBlockActionType(key, resultType, reversible, pastTense);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerEntityAction(String key, ActionResultType resultType, boolean reversible) {
        return registerEntityAction(key, resultType, reversible, (ModificationHandler) null);
    }

    @Override
    public ActionType registerEntityAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createEntityActionType(key, resultType, reversible, pastTense);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerEntityAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    ) {
        validateKey(key);

        ActionType actionType = createEntityActionType(key, resultType, reversible, null);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerEntityAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createEntityActionType(key, resultType, reversible, pastTense);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerItemAction(String key, ActionResultType resultType, boolean reversible) {
        return registerItemAction(key, resultType, reversible, (ModificationHandler) null);
    }

    @Override
    public ActionType registerItemAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createItemActionType(key, resultType, reversible, pastTense);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerItemAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    ) {
        validateKey(key);

        ActionType actionType = createItemActionType(key, resultType, reversible, null);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerItemAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createItemActionType(key, resultType, reversible, pastTense);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerPlayerAction(String key, ActionResultType resultType, boolean reversible) {
        return registerPlayerAction(key, resultType, reversible, (ModificationHandler) null);
    }

    @Override
    public ActionType registerPlayerAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createPlayerActionType(key, resultType, reversible, pastTense);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerPlayerAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler
    ) {
        validateKey(key);

        ActionType actionType = createPlayerActionType(key, resultType, reversible, null);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    @Override
    public ActionType registerPlayerAction(
        String key,
        ActionResultType resultType,
        boolean reversible,
        ModificationHandler handler,
        String pastTense
    ) {
        validateKey(key);

        ActionType actionType = createPlayerActionType(key, resultType, reversible, pastTense);
        actionType.modificationHandler(handler);
        registerAction(actionType);

        return actionType;
    }

    /**
     * Create a platform-specific generic action type.
     *
     * @param key The key
     * @param defaultPastTense The default past tense translation string, or null
     * @return The action type
     */
    protected abstract ActionType createGenericActionType(String key, @Nullable String defaultPastTense);

    /**
     * Create a platform-specific block action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible Whether this action is reversible
     * @param defaultPastTense The default past tense translation string, or null
     * @return The action type
     */
    protected abstract ActionType createBlockActionType(
        String key,
        ActionResultType resultType,
        boolean reversible,
        @Nullable String defaultPastTense
    );

    /**
     * Create a platform-specific entity action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible Whether this action is reversible
     * @param defaultPastTense The default past tense translation string, or null
     * @return The action type
     */
    protected abstract ActionType createEntityActionType(
        String key,
        ActionResultType resultType,
        boolean reversible,
        @Nullable String defaultPastTense
    );

    /**
     * Create a platform-specific item action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible Whether this action is reversible
     * @param defaultPastTense The default past tense translation string, or null
     * @return The action type
     */
    protected abstract ActionType createItemActionType(
        String key,
        ActionResultType resultType,
        boolean reversible,
        @Nullable String defaultPastTense
    );

    /**
     * Create a platform-specific player action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible Whether this action is reversible
     * @param defaultPastTense The default past tense translation string, or null
     * @return The action type
     */
    protected abstract ActionType createPlayerActionType(
        String key,
        ActionResultType resultType,
        boolean reversible,
        @Nullable String defaultPastTense
    );
}
