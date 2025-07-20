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
import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.query.ParameterContext;
import org.prism_mc.prism.bukkit.services.query.parsers.multiple.StringSetQueryArgumentParser;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public class ActionParameterParser extends StringSetQueryArgumentParser {

    /**
     * The action registry.
     */
    private final BukkitActionTypeRegistry actionRegistry;

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     * @param actionRegistry The action registry
     */
    public ActionParameterParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        BukkitActionTypeRegistry actionRegistry
    ) {
        super(messageService, defaultsConfiguration, "a");
        this.actionRegistry = actionRegistry;
    }

    @Override
    public boolean parse(
        CommandSender sender,
        ParameterContext parameterContext,
        Arguments arguments,
        BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?> builder
    ) {
        var values = parseMultipleParameters(arguments, builder);

        if (!values.isEmpty()) {
            for (String actionKey : values) {
                if (actionKey.contains("-")) {
                    var optionalIActionType = actionRegistry.actionType(actionKey.toLowerCase(Locale.ENGLISH));
                    if (optionalIActionType.isPresent()) {
                        builder.actionTypeKey(actionKey);
                    }
                } else {
                    builder.actionTypes(actionRegistry.actionTypesInFamily(actionKey.toLowerCase(Locale.ENGLISH)));
                }
            }
        }

        return true;
    }
}
