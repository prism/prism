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

package org.prism_mc.prism.loader.services.configuration.alerts;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class BlockBreakAlertsConfiguration {

    @Comment("Enable or disable all alerts of this type.")
    private boolean enabled = true;

    @Comment("Set the maximum light level that triggers the alert.")
    private int maxLightLevel = 100;

    @Comment("Set the minimum light level that triggers the alert.")
    private int minLightLevel = 0;

    /**
     * Alert configurations.
     */
    private List<BlockBreakAlertConfiguration> alerts = new ArrayList<>();

    /**
     * Constructor.
     */
    public BlockBreakAlertsConfiguration() {
        alerts.add(new BlockBreakAlertConfiguration(List.of("ancient_debris"), null, "#aa00aa", 20));

        alerts.add(new BlockBreakAlertConfiguration(null, List.of("minecraft:copper_ores"), "#c1765a", 150));

        alerts.add(new BlockBreakAlertConfiguration(null, List.of("minecraft:diamond_ores"), "#04babd", 30));

        alerts.add(new BlockBreakAlertConfiguration(null, List.of("minecraft:emerald_ores"), "#21bf60", 20));

        alerts.add(new BlockBreakAlertConfiguration(null, List.of("minecraft:gold_ores"), "#ffe17d", 30));

        alerts.add(new BlockBreakAlertConfiguration(null, List.of("minecraft:iron_ores"), "#d6d6d6", 150));

        alerts.add(new BlockBreakAlertConfiguration(null, List.of("minecraft:lapis_ores"), "#0670cc", 20));
    }
}
