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

package org.prism_mc.prism.loader.services.configuration.purge;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class CommandScheduleConfiguration {

    @Comment(
        """
        The command to be executed.
        Commands will be executed by the console and will effectively have OP permissions.
        The default command will purge all records more than six weeks old."""
    )
    private String command = "prism purge start before:6w --nodefaults";

    @Comment(
        """
        Configure the execution time(s) of this command using cron syntax.
        Here is a good crontab generator: https://www.freeformatter.com/cron-expression-generator-quartz.html
        Times are always based on the timezone of the machine the server is running on.
        The below example will run every day at midnight."""
    )
    private String cron = "0 0 0 ? * * *";

    @Comment("Toggle command schedule. When disabled, it will never run.")
    private boolean enabled = true;

    public String command() {
        return command;
    }

    public String cron() {
        return cron;
    }

    public boolean enabled() {
        return this.enabled;
    }
}
