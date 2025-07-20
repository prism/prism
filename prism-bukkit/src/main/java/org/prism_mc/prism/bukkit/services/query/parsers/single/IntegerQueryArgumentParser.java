/*
 * prism
 *
 * Copyright (c) 2022 M Botsko (viveleroi)
 * Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY and FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.prism_mc.prism.bukkit.services.query.parsers.single;

import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.Optional;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.query.parsers.QueryArgumentSingleValueParser;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public abstract class IntegerQueryArgumentParser extends QueryArgumentSingleValueParser<Integer> {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param parameter The parameter
     */
    public IntegerQueryArgumentParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        String parameter
    ) {
        super(messageService, defaultsConfiguration, parameter, Integer.class);
    }

    @Override
    protected Optional<Integer> parseSingleParameter(
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder
    ) {
        return parseSingleParameter(arguments, builder, (defaultValue, paramName) -> Integer.parseInt(defaultValue));
    }
}
