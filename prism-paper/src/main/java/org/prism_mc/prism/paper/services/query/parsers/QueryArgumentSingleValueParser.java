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

package org.prism_mc.prism.paper.services.query.parsers;

import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.Optional;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.services.messages.MessageService;

public abstract class QueryArgumentSingleValueParser<T> extends QueryArgumentParser<T> {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param parameter The parameter
     * @param clazz The class
     */
    public QueryArgumentSingleValueParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        String parameter,
        Class<T> clazz
    ) {
        super(messageService, defaultsConfiguration, parameter, clazz);
    }

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param phase The phase
     * @param parameter The parameter
     * @param clazz The class
     */
    public QueryArgumentSingleValueParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        Phase phase,
        String parameter,
        Class<T> clazz
    ) {
        super(messageService, defaultsConfiguration, phase, parameter, clazz);
    }

    @Override
    public boolean isPresent(Arguments arguments) {
        return arguments.getArgument(parameter, clazz).isPresent();
    }

    /**
     * Generic method to parse a single parameter.
     *
     * @param arguments The arguments to parse from.
     * @param builder The query builder to log default usage.
     * @return An Optional containing the parsed value, or empty if not found.
     */
    protected abstract Optional<T> parseSingleParameter(
        Arguments arguments,
        PaperActivityQuery.PaperActivityQueryBuilder<?, ?> builder
    );

    /**
     * Generic method to parse a single parameter.
     *
     * @param arguments The arguments to parse from.
     * @param builder The query builder to log default usage.
     * @param valueParser A function to parse the default string value into the desired type T.
     * @return An Optional containing the parsed value, or empty if not found.
     */
    protected Optional<T> parseSingleParameter(
        Arguments arguments,
        PaperActivityQuery.PaperActivityQueryBuilder<?, ?> builder,
        ValueParser<T> valueParser
    ) {
        Optional<T> parsedArgument = arguments.getArgument(parameter, clazz);
        if (parsedArgument.isPresent()) {
            return parsedArgument;
        }

        if (canUseDefaultValue(parameter, arguments)) {
            String defaultValueString = defaultsConfiguration.parameters().get(parameter);
            T parsedDefault = valueParser.parse(defaultValueString, parameter);

            if (builder != null) {
                builder.defaultUsed(String.format("%s:%s", parameter, parsedDefault));
            }

            return Optional.ofNullable(parsedDefault);
        }

        return Optional.empty();
    }
}
