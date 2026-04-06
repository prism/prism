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

package org.prism_mc.prism.api.actions;

import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionType;

public interface ActionFactory {
    /**
     * Create a generic action.
     *
     * @param actionType The action type
     * @return The action
     */
    Action createGenericAction(ActionType actionType);

    /**
     * Create a generic action with a descriptor.
     *
     * @param actionType The action type
     * @param descriptor The descriptor
     * @return The action
     */
    Action createGenericAction(ActionType actionType, String descriptor);

    /**
     * Create a generic action with a descriptor and metadata.
     *
     * @param actionType The action type
     * @param descriptor The descriptor
     * @param metadata The metadata
     * @return The action
     */
    Action createGenericAction(ActionType actionType, String descriptor, @Nullable Metadata metadata);
}
