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

import org.bukkit.Material;
import org.bukkit.Tag;
import org.prism_mc.prism.loader.services.configuration.alerts.BlockBreakAlertConfiguration;

public class BlockBreakAlert extends Alert<BlockBreakAlertConfiguration> {

    /**
     * Constructor.
     *
     * @param config The config
     * @param materialTag The material tag
     */
    public BlockBreakAlert(BlockBreakAlertConfiguration config, Tag<Material> materialTag) {
        super(config, materialTag);
    }
}
