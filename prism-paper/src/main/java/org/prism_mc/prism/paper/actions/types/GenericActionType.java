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

package org.prism_mc.prism.paper.actions.types;

import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.ActionData;
import org.prism_mc.prism.api.actions.types.ActionResultType;
import org.prism_mc.prism.api.actions.types.ActionType;
import org.prism_mc.prism.paper.actions.GenericPaperAction;

public class GenericActionType extends ActionType {

    /**
     * Construct a new generic action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public GenericActionType(String key, ActionResultType resultType, boolean reversible) {
        super(key, resultType, reversible, true);
    }

    /**
     * Construct a new generic action type.
     *
     * @param key The key
     * @param resultType The result type
     * @param reversible If action is reversible
     */
    public GenericActionType(String key, ActionResultType resultType, boolean reversible, boolean usesDescriptor) {
        super(key, resultType, reversible, usesDescriptor);
    }

    @Override
    public Action createAction(ActionData actionData) throws Exception {
        if (actionData.metadata() != null) {
            return new GenericPaperAction(this, actionData.descriptor(), actionData.metadata());
        } else {
            return new GenericPaperAction(this, actionData.descriptor());
        }
    }
}
