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

package org.prism_mc.prism.bukkit.services.query.parsers;

import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public abstract class QueryArgumentValueSetParser<T, R> extends QueryArgumentParser<T> {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param parameter The parameter
     * @param clazz The class
     */
    public QueryArgumentValueSetParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        String parameter,
        Class<T> clazz
    ) {
        super(messageService, defaultsConfiguration, parameter, clazz);
    }

    @Override
    public boolean isPresent(Arguments arguments) {
        return arguments.getListArgument(parameter, clazz).isPresent();
    }

    /**
     * Parse a multi-value string parameter.
     *
     * @param arguments The arguments
     * @param builder The builder
     * @return The parsed values
     */
    protected abstract Set<R> parseMultipleParameters(
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder
    );

    /**
     * Generic method to parse a list parameter.
     *
     * @param <R> The type of elements in the returned Set.
     * @param arguments The arguments
     * @param builder The query builder
     * @param listArgumentMapper A function to map an element from the argument list to the desired R type.
     * @param defaultValueParser A function to parse the default string value into a Set of R.
     * @return A Set containing the parsed values, never null.
     */
    protected <R> Set<R> parseSetParameter(
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder,
        Function<T, R> listArgumentMapper,
        Function<String, Set<R>> defaultValueParser
    ) {
        Optional<List<T>> parsedListArgument = arguments.getListArgument(parameter, clazz);

        if (parsedListArgument.isPresent()) {
            return parsedListArgument.get().stream().map(listArgumentMapper).collect(Collectors.toSet());
        }

        if (canUseDefaultValue(parameter, arguments)) {
            String defaultValueString = defaultsConfiguration.parameters().get(parameter);
            Set<R> parsedDefault = defaultValueParser.apply(defaultValueString);
            builder.defaultUsed(String.format("%s:%s", parameter, defaultValueString));
            return parsedDefault;
        }

        return new HashSet<>();
    }
}
