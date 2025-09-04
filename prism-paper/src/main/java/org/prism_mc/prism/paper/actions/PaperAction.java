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

package org.prism_mc.prism.paper.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.metadata.Metadata;
import org.prism_mc.prism.api.actions.types.ActionType;

public abstract class PaperAction implements Action {

    /**
     * The object mapper.
     */
    public static final ObjectMapper ObjectMapper = new ObjectMapper();

    /**
     * The descriptor.
     */
    @Setter
    protected String descriptor = "";

    /**
     * The type.
     */
    protected final ActionType type;

    /**
     * The metadata.
     */
    protected Metadata metadata;

    /**
     * Construct a new action.
     *
     * @param type The action type
     */
    public PaperAction(ActionType type) {
        this(type, null, null);
    }

    /**
     * Construct a new action.
     *
     * @param type The action type
     * @param descriptor The descriptor
     */
    public PaperAction(ActionType type, String descriptor) {
        this(type, descriptor, null);
    }

    /**
     * Construct a new action.
     *
     * @param type The action type
     * @param descriptor The descriptor
     * @param metadata The metadata
     */
    public PaperAction(ActionType type, String descriptor, Metadata metadata) {
        this.type = type;
        this.descriptor = descriptor;
        this.metadata = metadata;
    }

    @Override
    public String descriptor() {
        return descriptor;
    }

    @Override
    public Component descriptorComponent() {
        return descriptor == null ? Component.empty() : Component.text(descriptor);
    }

    @Override
    public Metadata metadata() {
        return metadata;
    }

    @Override
    public String serializeMetadata() throws Exception {
        return ObjectMapper.writeValueAsString(metadata);
    }

    @Override
    public ActionType type() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("Action{type=%s,descriptor=%s}", type, descriptor);
    }
}
