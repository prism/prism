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

package org.prism_mc.prism.bukkit.services.query.parsers.parameters;

import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.query.ParameterContext;
import org.prism_mc.prism.bukkit.services.query.parsers.single.StringQueryArgumentParser;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public class BeforeParameterParser extends StringQueryArgumentParser {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     */
    public BeforeParameterParser(MessageService messageService, DefaultsConfiguration defaultsConfiguration) {
        super(messageService, defaultsConfiguration, "before");
    }

    @Override
    public boolean parse(
        CommandSender sender,
        ParameterContext parameterContext,
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder
    ) {
        var optionalParameter = parseSingleParameter(arguments, builder);

        if (optionalParameter.isPresent()) {
            Long parsedTimestamp = parseTimestamp(optionalParameter.get());
            if (parsedTimestamp != null) {
                builder.before(parsedTimestamp);
            } else {
                messageService.errorParamInvalidTime(sender);

                return false;
            }
        }

        return true;
    }
}
