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

package org.prism_mc.prism.bukkit.services.query.parsers.multiple;

import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.query.parsers.QueryArgumentValueSetParser;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public abstract class MaterialSetQueryArgumentParser extends QueryArgumentValueSetParser<Material, String> {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param parameter The parameter
     */
    public MaterialSetQueryArgumentParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        String parameter
    ) {
        super(messageService, defaultsConfiguration, parameter, Material.class);
    }

    @Override
    protected Set<String> parseMultipleParameters(
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder
    ) {
        return parseSetParameter(
            arguments,
            builder,
            material -> material.name().toLowerCase(Locale.ENGLISH),
            defaultValue -> Arrays.stream(defaultValue.split(",")).collect(Collectors.toSet())
        );
    }
}
