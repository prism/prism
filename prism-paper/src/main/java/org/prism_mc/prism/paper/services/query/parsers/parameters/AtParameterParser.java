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

package org.prism_mc.prism.paper.services.query.parsers.parameters;

import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import org.bukkit.command.CommandSender;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.query.ParameterContext;
import org.prism_mc.prism.paper.services.query.parsers.single.StringQueryArgumentParser;

public class AtParameterParser extends StringQueryArgumentParser {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     */
    public AtParameterParser(MessageService messageService, DefaultsConfiguration defaultsConfiguration) {
        super(messageService, defaultsConfiguration, Phase.PRE, "at");
    }

    @Override
    public boolean parse(
        CommandSender sender,
        ParameterContext parameterContext,
        Arguments arguments,
        PaperActivityQuery.PaperActivityQueryBuilder<?, ?> builder
    ) {
        var optionalParameter = parseSingleParameter(arguments, builder);

        if (optionalParameter.isPresent()) {
            if (parameterContext.world == null) {
                messageService.errorParamAtNoWorld(sender);

                return false;
            }

            String[] segments = optionalParameter.get().split(",");
            if (segments.length == 3) {
                try {
                    int x = Integer.parseInt(segments[0]);
                    int y = Integer.parseInt(segments[1]);
                    int z = Integer.parseInt(segments[2]);

                    var atCoordinate = new Coordinate(x, y, z);
                    parameterContext.setReferenceLocation(atCoordinate);
                    builder.referenceCoordinate(atCoordinate);
                } catch (NumberFormatException e) {
                    messageService.errorParamAtInvalidLocation(sender);

                    return false;
                }
            } else {
                messageService.errorParamAtInvalidLocation(sender);

                return false;
            }
        }

        return true;
    }
}
