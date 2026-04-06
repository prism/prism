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

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.api.services.modifications.ModificationHandler;

public interface Action extends ModificationHandler {
    /**
     * Get the descriptor.
     *
     * <p>A descriptor is a mix between an identifier and formatted/specific content.</p>
     *
     * <p>For example, we log the entity type of "BOAT" and custom nbt serialized data
     * but custom data is too unique to group, yet pieces are important to users.
     * A descriptor parses the key data for a *descriptive*, *group-able* name.</p>
     *
     * <p>Like "mangrove boat". The descriptor is stored in the DB so it a) works
     * in grouping queries and b) is available to external sources like a web ui.</p>
     *
     * @return The descriptor
     */
    String descriptor();

    /**
     * An in-game descriptor. May differ from the database version
     * in that it includes translatable components or other information.
     *
     * @return The descriptor component
     */
    Component descriptorComponent();

    /**
     * Get the metadata record attached to this action.
     *
     * @return The metadata
     */
    @Nullable
    Metadata metadata();

    /**
     * Serialize the metadata.
     *
     * @return The serialized metadata
     * @throws Exception Serialization exception
     */
    String serializeMetadata() throws Exception;

    /**
     * Get the action type.
     *
     * @return The action type
     */
    ActionType type();
}
