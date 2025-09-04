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

package org.prism_mc.prism.paper.services.alerts;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;

@Getter
public class Alert<T> {

    /**
     * The configuration.
     */
    private final T config;

    /**
     * The material tag (materials, block-tags, item-tags).
     */
    private final Tag<Material> materialTag;

    /**
     * The constructor.
     *
     * @param config The configuration
     * @param materialTag The material tags
     */
    public Alert(T config, Tag<Material> materialTag) {
        this.config = config;
        this.materialTag = materialTag;
    }
}
