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
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.query.ParameterContext;
import org.prism_mc.prism.bukkit.services.query.QueryService;
import org.prism_mc.prism.bukkit.services.query.annotations.ConflictsWith;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public abstract class QueryArgumentParser<T> {

    public enum Phase {
        /**
         * This parser must run before everything else.
         */
        PRE,

        /**
         * This parser runs during the normal phase.
         */
        NORMAL,
    }

    /**
     * The phase.
     */
    public final Phase phase;

    /**
     * The configuration of parameter defaults.
     */
    protected final DefaultsConfiguration defaultsConfiguration;

    /**
     * The message service.
     */
    protected final MessageService messageService;

    /**
     * The parameter.
     */
    @Getter
    protected final String parameter;

    /**
     * The parameter result class.
     */
    protected final Class<T> clazz;

    /**
     * Functional interface to parse a string from defaults into the desired type.
     *
     * @param <T> The type
     */
    @FunctionalInterface
    protected interface ValueParser<T> {
        T parse(String defaultValue, String parameterName);
    }

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param parameter The parameter
     * @param clazz The class
     */
    public QueryArgumentParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        String parameter,
        Class<T> clazz
    ) {
        this(messageService, defaultsConfiguration, Phase.NORMAL, parameter, clazz);
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
    public QueryArgumentParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        Phase phase,
        String parameter,
        Class<T> clazz
    ) {
        this.messageService = messageService;
        this.defaultsConfiguration = defaultsConfiguration;
        this.phase = phase;
        this.parameter = parameter;
        this.clazz = clazz;
    }

    /**
     * Check if the argument is present.
     *
     * @param arguments The arguments
     * @return True if present
     */
    public abstract boolean isPresent(Arguments arguments);

    /**
     * Parse the argument parameters into a query object.
     *
     * @param sender The command sender
     * @param parameterContext The query context
     * @param arguments The arguments
     * @param builder The builder
     * @return False if the parser encountered a fatal error
     */
    public abstract boolean parse(
        CommandSender sender,
        ParameterContext parameterContext,
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder
    );

    /**
     * Alert the sender if conflicts are present.
     *
     * @param sender The sender
     * @param arguments The arguments
     * @return True if conflicts were alerted
     */
    protected boolean alertConflicts(CommandSender sender, Arguments arguments) {
        var conflicts = hasArgumentConflicts(arguments);

        if (conflicts.length == 2) {
            messageService.errorParamConflict(sender, conflicts[0], conflicts[1]);

            return true;
        }

        return false;
    }

    /**
     * Check whether the default value should be used for a parameter.
     *
     * @param parameter The parameter
     * @param arguments The arguments
     * @return True if default can be used
     */
    protected boolean canUseDefaultValue(String parameter, Arguments arguments) {
        return (
            hasArgumentConflicts(arguments).length == 0 &&
            !arguments.hasFlag("nodefaults") &&
            defaultsConfiguration.parameters().containsKey(parameter)
        );
    }

    /**
     * Check whether this parameter has any conflicts present in the current arguments object.
     *
     * @param arguments The arguments
     * @return Parameters that conflict
     */
    protected String[] hasArgumentConflicts(Arguments arguments) {
        ConflictsWith annotation = this.getClass().getAnnotation(ConflictsWith.class);
        if (annotation != null) {
            for (var conflictedClass : annotation.value()) {
                for (var parser2 : QueryService.parsers) {
                    if (parser2.getClass() == conflictedClass && parser2.isPresent(arguments)) {
                        return new String[] { this.parameter, parser2.parameter() };
                    }
                }
            }
        }

        return new String[] {};
    }

    /**
     * Parses a string duration into a unix timestamp.
     *
     * @return The timestamp
     */
    protected Long parseTimestamp(String value) {
        final Pattern pattern = Pattern.compile("([0-9]+)([shmdw])");
        final Matcher matcher = pattern.matcher(value);

        final Calendar cal = Calendar.getInstance();
        while (matcher.find()) {
            if (matcher.groupCount() == 2) {
                final int time = Integer.parseInt(matcher.group(1));
                final String duration = matcher.group(2);

                switch (duration) {
                    case "w":
                        cal.add(Calendar.WEEK_OF_YEAR, -1 * time);
                        break;
                    case "d":
                        cal.add(Calendar.DAY_OF_MONTH, -1 * time);
                        break;
                    case "h":
                        cal.add(Calendar.HOUR, -1 * time);
                        break;
                    case "m":
                        cal.add(Calendar.MINUTE, -1 * time);
                        break;
                    case "s":
                        cal.add(Calendar.SECOND, -1 * time);
                        break;
                }

                return cal.getTime().getTime() / 1000;
            }
        }

        return null;
    }
}
