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

package org.prism_mc.prism.paper.services.query;

import com.google.inject.Inject;
import com.google.inject.Provider;
import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.integrations.worldedit.WorldEditIntegration;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.query.parsers.QueryArgumentParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ActionParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.AtParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BeforeParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BlockCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BlockParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BlockTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BoundsParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.CauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.DescriptorParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.EntityTypeCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.EntityTypeParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.EntityTypeTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.IdParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.InParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ItemParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ItemTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.PlayerAffectedParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.PlayerCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.PlayerParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.RadiusQueryArgumentParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ReversedParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.SinceParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.WorldParameterParser;

public class QueryService {

    /**
     * The message service.
     */
    protected final MessageService messageService;

    /**
     * All registered argument/query parsers.
     */
    public static final List<QueryArgumentParser<?>> parsers = new ArrayList<>();

    /**
     * The ID parameter parser, which has the highest priority.
     */
    private final IdParameterParser idParameterParser;

    /**
     * The query service.
     *
     * @param actionRegistry The action registry
     */
    @Inject
    public QueryService(
        PaperActionTypeRegistry actionRegistry,
        ConfigurationService configurationService,
        MessageService messageService,
        Provider<WorldEditIntegration> worldEditIntegration
    ) {
        this.messageService = messageService;
        this.idParameterParser = new IdParameterParser(messageService, configurationService.prismConfig().defaults());

        // World parser must be first
        parsers.add(new WorldParameterParser(messageService, configurationService.prismConfig().defaults()));

        parsers.add(
            new ActionParameterParser(messageService, configurationService.prismConfig().defaults(), actionRegistry)
        );
        parsers.add(new AtParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BeforeParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BlockCauseParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BlockParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BlockTagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BoundsParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new CauseParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new EntityTypeCauseParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new EntityTypeParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new EntityTypeTagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(
            new InParameterParser(messageService, configurationService.prismConfig().defaults(), worldEditIntegration)
        );
        parsers.add(new ItemParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ItemTagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new DescriptorParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new PlayerAffectedParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new PlayerCauseParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new PlayerParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new RadiusQueryArgumentParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ReversedParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new SinceParameterParser(messageService, configurationService.prismConfig().defaults()));
    }

    /**
     * Start a query builder from command-derived parameters.
     *
     * @param sender The command sender
     * @param arguments The arguments
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        Arguments arguments
    ) {
        if (sender instanceof Player player) {
            return queryFromArguments(sender, arguments, player.getLocation());
        } else {
            return queryFromArguments(sender, arguments, null);
        }
    }

    /**
     * Start a query builder from command-derived parameters.
     *
     * @param referenceLocation The reference location
     * @param arguments The arguments
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        Arguments arguments,
        Location referenceLocation
    ) {
        if (!arguments.getText().isEmpty()) {
            messageService.errorParamInvalid(sender, arguments.getText());

            return Optional.empty();
        }

        var builder = PaperActivityQuery.builder();

        // No-group flag
        if (arguments.hasFlag("nogroup")) {
            builder.grouped(false);
        }

        // If an ID is provided, no other parameters matter
        if (idParameterParser.isPresent(arguments)) {
            idParameterParser.parse(sender, null, arguments, builder);

            return Optional.of(builder);
        }

        // Some parameters set/modify the context
        var queryContext = new ParameterContext(referenceLocation);
        for (var parser : parsers) {
            if (parser.phase.equals(QueryArgumentParser.Phase.PRE)) {
                parser.parse(sender, queryContext, arguments, builder);
            }
        }

        // Let all parsers do their thing
        for (var parser : parsers) {
            if (
                parser.phase.equals(QueryArgumentParser.Phase.NORMAL) &&
                !parser.parse(sender, queryContext, arguments, builder)
            ) {
                return Optional.empty();
            }
        }

        return Optional.of(builder);
    }
}
