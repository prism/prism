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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;
import org.prism_mc.prism.paper.integrations.worldedit.WorldEditIntegration;
import org.prism_mc.prism.paper.permissions.PrismFlags;
import org.prism_mc.prism.paper.permissions.PrismPermissions;
import org.prism_mc.prism.paper.services.limits.EffectiveLimits;
import org.prism_mc.prism.paper.services.limits.LimitService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.query.parsers.QueryArgumentParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.AboveParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ActionParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.AirtagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.AtParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BeforeParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BelowParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BlockCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BlockParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BlockTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.BoundsParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.CauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.DescriptorParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.EntityTypeCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.EntityTypeParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.EntityTypeTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeActionParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeBlockCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeBlockParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeBlockTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeEntityTypeCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeEntityTypeParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeEntityTypeTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeItemParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeItemTagParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludePlayerAffectedParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludePlayerCauseParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludePlayerParameterParser;
import org.prism_mc.prism.paper.services.query.parsers.parameters.ExcludeWorldParameterParser;
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
     * The configuration service.
     */
    protected final ConfigurationService configurationService;

    /**
     * The limit service.
     */
    private final LimitService limitService;

    /**
     * All registered argument/query parsers.
     */
    public static final List<QueryArgumentParser<?>> parsers = new ArrayList<>();

    /**
     * Parsers whose parameters constrain query location/region. Excluded from commands
     * (like wand and near) that already derive their location from a different source.
     */
    public static final Set<Class<? extends QueryArgumentParser<?>>> LOCATION_PARSERS = Set.of(
        AboveParameterParser.class,
        AtParameterParser.class,
        BelowParameterParser.class,
        BoundsParameterParser.class,
        InParameterParser.class,
        RadiusQueryArgumentParser.class,
        WorldParameterParser.class
    );

    /**
     * The ID parameter parser, which has the highest priority.
     */
    private final IdParameterParser idParameterParser;

    /**
     * The since parser, used to resolve a query's effective lower time bound
     * when enforcing the permission-gated look-back limit.
     */
    private final SinceParameterParser sinceParameterParser;

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
        LimitService limitService,
        Provider<WorldEditIntegration> worldEditIntegration
    ) {
        this.messageService = messageService;
        this.configurationService = configurationService;
        this.limitService = limitService;
        this.idParameterParser = new IdParameterParser(messageService, configurationService.prismConfig().defaults());

        // World parser must be first
        parsers.add(new WorldParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ExcludeWorldParameterParser(messageService, configurationService.prismConfig().defaults()));

        parsers.add(new AboveParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(
            new ActionParameterParser(messageService, configurationService.prismConfig().defaults(), actionRegistry)
        );
        parsers.add(new AtParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BeforeParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new BelowParameterParser(messageService, configurationService.prismConfig().defaults()));
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
        parsers.add(new AirtagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ItemParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ItemTagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new DescriptorParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new PlayerAffectedParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new PlayerCauseParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new PlayerParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(
            new ExcludePlayerAffectedParameterParser(messageService, configurationService.prismConfig().defaults())
        );
        parsers.add(
            new ExcludePlayerCauseParameterParser(messageService, configurationService.prismConfig().defaults())
        );
        parsers.add(new ExcludePlayerParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(
            new ExcludeActionParameterParser(
                messageService,
                configurationService.prismConfig().defaults(),
                actionRegistry
            )
        );
        parsers.add(
            new ExcludeBlockCauseParameterParser(messageService, configurationService.prismConfig().defaults())
        );
        parsers.add(new ExcludeBlockParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ExcludeBlockTagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ExcludeCauseParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(
            new ExcludeEntityTypeCauseParameterParser(messageService, configurationService.prismConfig().defaults())
        );
        parsers.add(
            new ExcludeEntityTypeParameterParser(messageService, configurationService.prismConfig().defaults())
        );
        parsers.add(
            new ExcludeEntityTypeTagParameterParser(messageService, configurationService.prismConfig().defaults())
        );
        parsers.add(new ExcludeItemParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ExcludeItemTagParameterParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new RadiusQueryArgumentParser(messageService, configurationService.prismConfig().defaults()));
        parsers.add(new ReversedParameterParser(messageService, configurationService.prismConfig().defaults()));

        this.sinceParameterParser = new SinceParameterParser(
            messageService,
            configurationService.prismConfig().defaults()
        );

        parsers.add(sinceParameterParser);
    }

    /**
     * Names of every parameter recognized by the registered parsers, including
     * the id parser. Source of truth for permission-tree construction so adding
     * a new parser automatically extends the per-command parameter perms.
     *
     * @return Unmodifiable set of parameter names (raw, including {@code !} suffixes)
     */
    public Set<String> parameterNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add(idParameterParser.parameter());
        for (var parser : parsers) {
            names.add(parser.parameter());
        }

        return Collections.unmodifiableSet(names);
    }

    /**
     * Check if any registered parameter is present in the arguments.
     *
     * @param arguments The arguments
     * @return True if any parameter is present
     */
    public boolean hasAnyParameter(Arguments arguments) {
        if (idParameterParser.isPresent(arguments)) {
            return true;
        }

        for (var parser : parsers) {
            if (parser.isPresent(arguments)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if any command flag is present.
     *
     * @param arguments The arguments
     * @return True if the sender supplied any flag (query or modification)
     */
    public boolean hasAnyFlag(Arguments arguments) {
        return !arguments.getAllFlags().isEmpty();
    }

    /**
     * Start a query builder from command-derived parameters.
     *
     * @param sender The command sender
     * @param commandPath The permission path for this command (e.g. "lookup.lookup").
     *                    Used to gate which parameters and flags the sender may use.
     * @param arguments The arguments
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        String commandPath,
        Arguments arguments
    ) {
        return queryFromArguments(sender, commandPath, arguments, (DefaultsConfiguration.CommandType) null);
    }

    /**
     * Start a query builder from command-derived parameters, applying the defaults
     * configured for the given command (merged over the base defaults).
     *
     * @param sender The command sender
     * @param commandPath The permission path for this command
     * @param arguments The arguments
     * @param command The command whose defaults should apply, or null for base only
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        String commandPath,
        Arguments arguments,
        DefaultsConfiguration.CommandType command
    ) {
        if (sender instanceof Player player) {
            return queryFromArguments(sender, commandPath, arguments, player.getLocation(), Set.of(), command);
        } else {
            return queryFromArguments(sender, commandPath, arguments, null, Set.of(), command);
        }
    }

    /**
     * Start a query builder from command-derived parameters with the option to
     * bypass config-driven parameter defaults entirely. The skip applies only
     * to this call; the per-thread flag is cleared in a finally block so it
     * cannot leak into other queries.
     *
     * @param sender The command sender
     * @param commandPath The permission path for this command
     * @param arguments The arguments
     * @param referenceLocation The reference location
     * @param skipDefaults If true, parsers will not substitute any configured defaults
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        String commandPath,
        Arguments arguments,
        Location referenceLocation,
        boolean skipDefaults
    ) {
        if (!skipDefaults) {
            return queryFromArguments(sender, commandPath, arguments, referenceLocation, Set.of());
        }

        QueryArgumentParser.setSkipDefaults(true);
        try {
            return queryFromArguments(sender, commandPath, arguments, referenceLocation, Set.of());
        } finally {
            QueryArgumentParser.clearSkipDefaults();
        }
    }

    /**
     * Start a query builder from command-derived parameters, refusing any parsers in the
     * excluded set. If the user supplied an excluded parameter, an error is sent and an empty
     * optional is returned.
     *
     * @param sender The command sender
     * @param commandPath The permission path for this command
     * @param arguments The arguments
     * @param referenceLocation The reference location
     * @param excludedParsers Parser classes that are not allowed for this command
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        String commandPath,
        Arguments arguments,
        Location referenceLocation,
        Set<Class<? extends QueryArgumentParser<?>>> excludedParsers
    ) {
        return queryFromArguments(sender, commandPath, arguments, referenceLocation, excludedParsers, null);
    }

    /**
     * Start a query builder from command-derived parameters, refusing any parsers in the
     * excluded set and applying the defaults configured for the given command. If the user
     * supplied an excluded parameter, an error is sent and an empty optional is returned.
     *
     * @param sender The command sender
     * @param commandPath The permission path for this command
     * @param arguments The arguments
     * @param referenceLocation The reference location
     * @param excludedParsers Parser classes that are not allowed for this command
     * @param command The command whose defaults should apply, or null for base only
     * @return The activity query builder
     */
    public Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        String commandPath,
        Arguments arguments,
        Location referenceLocation,
        Set<Class<? extends QueryArgumentParser<?>>> excludedParsers,
        DefaultsConfiguration.CommandType command
    ) {
        var defaults = configurationService.prismConfig().defaults();
        Map<String, String> defaultParameters = command == null ? defaults.parameters() : defaults.parameters(command);

        QueryArgumentParser.setActiveParameters(defaultParameters);
        try {
            Map<String, String> defaultFlags = command == null ? defaults.flags() : defaults.flags(command);

            return buildQuery(sender, commandPath, arguments, referenceLocation, excludedParsers, defaultFlags);
        } finally {
            QueryArgumentParser.clearActiveParameters();
        }
    }

    /**
     * Build the query, applying the supplied resolved default flags where the player did
     * not provide their own. The active default-parameter map is expected to already be
     * set on {@link QueryArgumentParser} for the duration of this call.
     */
    private Optional<PaperActivityQuery.PaperActivityQueryBuilder<?, ?>> buildQuery(
        CommandSender sender,
        String commandPath,
        Arguments arguments,
        Location referenceLocation,
        Set<Class<? extends QueryArgumentParser<?>>> excludedParsers,
        Map<String, String> defaultFlags
    ) {
        // Reject any excluded parameters the user supplied up front
        for (var parser : parsers) {
            if (excludedParsers.contains(parser.getClass()) && parser.isPresent(arguments)) {
                messageService.errorParamUnsupported(sender, parser.parameter());

                return Optional.empty();
            }
        }

        // Enforce per-parameter and per-flag permissions for this command.
        // The id parser sits outside the `parsers` list, so check it explicitly.
        if (
            idParameterParser.isPresent(arguments) &&
            !hasParameterPerm(sender, commandPath, idParameterParser.parameter())
        ) {
            messageService.errorInsufficientPermission(sender);
            return Optional.empty();
        }

        for (var parser : parsers) {
            if (excludedParsers.contains(parser.getClass())) {
                continue;
            }

            if (parser.isPresent(arguments) && !hasParameterPerm(sender, commandPath, parser.parameter())) {
                messageService.errorInsufficientPermission(sender);
                return Optional.empty();
            }
        }

        for (String flagName : PrismFlags.LONG_NAMES) {
            if (arguments.hasFlag(flagName) && !hasFlagPerm(sender, commandPath, flagName)) {
                messageService.errorInsufficientPermission(sender);
                return Optional.empty();
            }
        }

        // Enforce opt-in, permission-gated limits on parameter values. checkLimit
        // is called for every parser — not just the ones the sender typed — so a
        // limit also constrains the value a parser would substitute from a config
        // default. A violation messages the sender and aborts the query.
        EffectiveLimits limits = limitService.effectiveLimits(sender, commandPath);
        if (!limits.isEmpty()) {
            for (var parser : parsers) {
                if (excludedParsers.contains(parser.getClass())) {
                    continue;
                }

                if (!parser.checkLimit(sender, arguments, limits)) {
                    return Optional.empty();
                }
            }
        }

        if (!arguments.getText().isEmpty()) {
            messageService.errorParamInvalid(sender, arguments.getText());

            return Optional.empty();
        }

        var builder = PaperActivityQuery.builder();

        // Default flags only apply when defaults are not suppressed for this query
        boolean useDefaultFlags = !arguments.hasFlag("nodefaults") && !QueryArgumentParser.isSkipDefaults();

        // Count flag
        if (arguments.hasFlag("count")) {
            builder.countOnly(true);
        } else if (useDefaultFlags && Boolean.parseBoolean(defaultFlags.get("count"))) {
            builder.countOnly(true);
            builder.defaultUsed("--count");
        }

        // No-group flag
        if (arguments.hasFlag("nogroup")) {
            builder.grouped(false);
        } else if (useDefaultFlags && Boolean.parseBoolean(defaultFlags.get("nogroup"))) {
            builder.grouped(false);
            builder.defaultUsed("--nogroup");
        }

        // Sort flag
        Optional<String> sortFlag = arguments.getFlagValue("sort", String.class);
        if (sortFlag.isPresent()) {
            String sortValue = sortFlag.get().toLowerCase(Locale.ROOT);
            switch (sortValue) {
                case "asc" -> builder.sort(ActivityQuery.Sort.ASCENDING);
                case "desc" -> builder.sort(ActivityQuery.Sort.DESCENDING);
                default -> {
                    messageService.errorParamInvalid(sender, "sort:" + sortFlag.get());
                    return Optional.empty();
                }
            }
        } else if (useDefaultFlags && defaultFlags.containsKey("sort")) {
            // A misconfigured default sort is ignored rather than failing every query
            String sortValue = defaultFlags.get("sort").toLowerCase(Locale.ROOT);
            switch (sortValue) {
                case "asc" -> {
                    builder.sort(ActivityQuery.Sort.ASCENDING);
                    builder.defaultUsed("--sort asc");
                }
                case "desc" -> {
                    builder.sort(ActivityQuery.Sort.DESCENDING);
                    builder.defaultUsed("--sort desc");
                }
                default -> {
                    // Ignore invalid configured default
                }
            }
        }

        Optional<String> shareFlag = arguments.getFlagValue("share", String.class);
        if (shareFlag.isPresent()) {
            Player target = Bukkit.getPlayerExact(shareFlag.get());
            if (target == null) {
                messageService.errorPlayerNotFound(sender, shareFlag.get());
                return Optional.empty();
            }

            builder.shareWith(target.getName());
        }

        // If an ID is provided, no other parameters matter
        if (idParameterParser.isPresent(arguments)) {
            idParameterParser.parse(sender, null, arguments, builder);

            return Optional.of(builder);
        }

        // Some parameters set/modify the context
        var queryContext = new ParameterContext(referenceLocation);
        for (var parser : parsers) {
            if (excludedParsers.contains(parser.getClass())) {
                continue;
            }

            if (parser.phase.equals(QueryArgumentParser.Phase.PRE)) {
                parser.parse(sender, queryContext, arguments, builder);
            }
        }

        // Let all parsers do their thing
        for (var parser : parsers) {
            if (excludedParsers.contains(parser.getClass())) {
                continue;
            }

            if (
                parser.phase.equals(QueryArgumentParser.Phase.NORMAL) &&
                !parser.parse(sender, queryContext, arguments, builder)
            ) {
                return Optional.empty();
            }
        }

        // Floor the query's lower time bound so a look-back limit is enforced
        // even when the sender omits since:/before: entirely — a value-only check
        // can't catch an unbounded query, so this clamps it after parsing.
        applyLookbackLimit(arguments, limits, builder);

        return Optional.of(builder);
    }

    /**
     * Clamp the query's lower time bound to the sender's permitted look-back
     * window. Does nothing when no look-back limit applies; otherwise raises the
     * {@code after} bound to {@code now - cap} whenever the query would
     * otherwise reach further back (including when it has no lower bound at all).
     *
     * @param arguments The arguments
     * @param limits The effective limits
     * @param builder The query builder
     */
    private void applyLookbackLimit(
        Arguments arguments,
        EffectiveLimits limits,
        PaperActivityQuery.PaperActivityQueryBuilder<?, ?> builder
    ) {
        var cap = limits.maxLookbackSeconds();
        if (cap.isEmpty()) {
            return;
        }

        long floor = (System.currentTimeMillis() / 1000L) - cap.get();
        var effectiveAfter = sinceParameterParser.effectiveLowerBound(arguments);
        if (effectiveAfter.isEmpty() || effectiveAfter.get() < floor) {
            builder.after(floor);
        }
    }

    /**
     * Test whether the sender holds the per-command parameter permission.
     * Returns true unconditionally when commandPath is null so call sites
     * that have no meaningful command context can opt out of gating.
     */
    private boolean hasParameterPerm(CommandSender sender, String commandPath, String parameterName) {
        if (commandPath == null) {
            return true;
        }
        return sender.hasPermission(PrismPermissions.parameterPerm(commandPath, parameterName));
    }

    /**
     * Test whether the sender holds the per-command flag permission.
     */
    private boolean hasFlagPerm(CommandSender sender, String commandPath, String flagName) {
        if (commandPath == null) {
            return true;
        }
        return sender.hasPermission(PrismPermissions.flagPerm(commandPath, flagName));
    }
}
