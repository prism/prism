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
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class ActivitiesConfiguration {

    @Comment(
        """
        Whether to ignore recording activities for players in creative.
        For finer control, you may also create a filter."""
    )
    private boolean ignoreCreative = true;

    @Comment(
        """
        Commands that may contain sensitive data when tracking player-command.
        Matches the beginning of a command string. Do not include the forward-slash."""
    )
    private List<String> sensitiveCommands = new ArrayList<>();

    /**
     * Constructor.
     */
    public ActivitiesConfiguration() {
        sensitiveCommands.add("l");
        sensitiveCommands.add("login");
        sensitiveCommands.add("reg");
        sensitiveCommands.add("register");
        sensitiveCommands.add("changepassword");
        sensitiveCommands.add("changepw");
        sensitiveCommands.add("unreg");
        sensitiveCommands.add("unregister");
    }
}
