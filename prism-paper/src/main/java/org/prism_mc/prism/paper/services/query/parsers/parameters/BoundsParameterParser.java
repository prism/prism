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
import org.prism_mc.prism.paper.services.query.annotations.ConflictsWith;
import org.prism_mc.prism.paper.services.query.parsers.single.StringQueryArgumentParser;

@ConflictsWith(value = { AtParameterParser.class, InParameterParser.class })
public class BoundsParameterParser extends StringQueryArgumentParser {

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     */
    public BoundsParameterParser(MessageService messageService, DefaultsConfiguration defaultsConfiguration) {
        super(messageService, defaultsConfiguration, "bounds");
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
            if (alertConflicts(sender, arguments)) {
                return false;
            }

            if (parameterContext.world == null) {
                messageService.errorParamBoundsNoWorld(sender);

                return false;
            }

            String[] segments = optionalParameter.get().split("/");
            if (segments.length != 2) {
                messageService.errorParamBoundsInvalid(sender);

                return false;
            }

            String[] minSegments = segments[0].split(",");
            if (minSegments.length != 3) {
                messageService.errorParamBoundsInvalid(sender);

                return false;
            }

            String[] maxSegments = segments[1].split(",");
            if (maxSegments.length != 3) {
                messageService.errorParamBoundsInvalid(sender);

                return false;
            }

            try {
                int minX = Integer.parseInt(minSegments[0]);
                int minY = Integer.parseInt(minSegments[1]);
                int minZ = Integer.parseInt(minSegments[2]);

                int maxX = Integer.parseInt(maxSegments[0]);
                int maxY = Integer.parseInt(maxSegments[1]);
                int maxZ = Integer.parseInt(maxSegments[2]);

                Coordinate min = new Coordinate(minX, minY, minZ);
                Coordinate max = new Coordinate(maxX, maxY, maxZ);
                builder.boundingCoordinates(min, max);
                builder.referenceLocation(parameterContext.referenceLocation);
            } catch (NumberFormatException e) {
                messageService.errorParamBoundsInvalid(sender);

                return false;
            }
        }

        return true;
    }
}
