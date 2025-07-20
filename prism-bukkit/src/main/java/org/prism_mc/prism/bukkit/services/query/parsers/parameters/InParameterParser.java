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
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.integrations.worldedit.WorldEditIntegration;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.services.query.ParameterContext;
import org.prism_mc.prism.bukkit.services.query.parsers.single.StringQueryArgumentParser;
import org.prism_mc.prism.bukkit.utils.LocationUtils;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;

public class InParameterParser extends StringQueryArgumentParser {

    /**
     * The world edit integration.
     */
    private final WorldEditIntegration worldEditIntegration;

    /**
     * Constructor.
     *
     * @param messageService The message service
     * @param defaultsConfiguration The defaults configuration
     */
    public InParameterParser(
        MessageService messageService,
        DefaultsConfiguration defaultsConfiguration,
        WorldEditIntegration worldEditIntegration
    ) {
        super(messageService, defaultsConfiguration, "in");
        this.worldEditIntegration = worldEditIntegration;
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
            if (parameterContext.referenceLocation == null) {
                messageService.errorParamConsoleIn(sender);

                return false;
            }

            if (optionalParameter.get().equalsIgnoreCase("worldedit")) {
                if (worldEditIntegration != null) {
                    var regionBounds = worldEditIntegration.getRegionBounds((Player) sender);

                    if (regionBounds != null) {
                        builder.boundingCoordinates(regionBounds.key(), regionBounds.value());
                    } else {
                        messageService.errorWorldEditMissingSelection(sender);
                    }
                } else {
                    messageService.errorWorldEditMissing(sender);
                }
            } else {
                builder.worldUuid(parameterContext.world.getUID());

                if (optionalParameter.get().equalsIgnoreCase("chunk")) {
                    Chunk chunk = parameterContext.referenceLocation.getChunk();
                    Coordinate chunkMin = LocationUtils.getChunkMinCoordinate(chunk);
                    Coordinate chunkMax = LocationUtils.getChunkMaxCoordinate(chunk);

                    builder.boundingCoordinates(chunkMin, chunkMax);
                }
            }
        }

        return true;
    }
}
