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

package org.prism_mc.prism.loader.services.configuration;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.prism_mc.prism.loader.services.configuration.alerts.AlertsConfiguration;
import org.prism_mc.prism.loader.services.configuration.cache.CacheConfiguration;
import org.prism_mc.prism.loader.services.configuration.filters.FilterConfiguration;
import org.prism_mc.prism.loader.services.configuration.purge.PurgeConfiguration;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class PrismConfiguration {

    @Comment(
        """
        Actions are in-game events/changes that Prism can record data for.
        Some are purely informational, some can be reversed/restored.
        Disabling any here will completely prevent prism from recording them.
        Actions that are disabled by default may need some consideration/filters
        before enabling them. Blanket-enabling everything is a recipe for
        exponential database growth."""
    )
    private ActionsConfiguration actions = new ActionsConfiguration();

    @Comment("Activities represent an action, location, cause, timestamp, etc.")
    private ActivitiesConfiguration activities = new ActivitiesConfiguration();

    private AlertsConfiguration alerts = new AlertsConfiguration();

    @Comment(
        """
        Configure how Prism caches data. Probably best to leave
        these settings alone unless you have specific reasons to change them."""
    )
    private CacheConfiguration cache = new CacheConfiguration();

    private CommandsConfiguration commands = new CommandsConfiguration();

    @Comment("Enable plugin debug mode. Produces extra logging to help diagnose issues.")
    private boolean debug = false;

    @Comment("Enable filter debug mode. Logs details about every event and filter matching.")
    private boolean debugFilters = false;

    private DefaultsConfiguration defaults = new DefaultsConfiguration();

    @Comment(
        """
        Filters allow fine-grained control over what prism records.
        Please see https://docs.prism-mc.org/configs/filters/"""
    )
    private List<FilterConfiguration> filters = new ArrayList<>();

    @Comment("Configure rules for modifications (rollbacks/restores).")
    private ModificationConfiguration modifications = new ModificationConfiguration();

    private PurgeConfiguration purges = new PurgeConfiguration();

    private RecordingConfiguration recording = new RecordingConfiguration();
}
