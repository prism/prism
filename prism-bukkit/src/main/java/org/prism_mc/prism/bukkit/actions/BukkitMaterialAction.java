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

package org.prism_mc.prism.bukkit.actions;

import static org.prism_mc.prism.bukkit.api.activities.BukkitActivity.enumNameToString;

import java.util.Locale;
import org.bukkit.Material;
import org.prism_mc.prism.api.actions.MaterialAction;
import org.prism_mc.prism.api.actions.types.ActionType;

public abstract class BukkitMaterialAction extends BukkitAction implements MaterialAction {

    /**
     * The material.
     */
    protected Material material;

    /**
     * Construct a new material action.
     *
     * @param type The action type
     * @param material The material
     */
    public BukkitMaterialAction(ActionType type, Material material, String descriptor) {
        super(type, descriptor, null);
        this.material = material;

        if (this.descriptor == null) {
            this.descriptor = enumNameToString(material.name());
        }
    }

    /**
     * Get the material.
     *
     * @return The material
     */
    public Material material() {
        return material;
    }

    @Override
    public String serializeMaterial() {
        return material.toString().toLowerCase(Locale.ENGLISH);
    }
}
