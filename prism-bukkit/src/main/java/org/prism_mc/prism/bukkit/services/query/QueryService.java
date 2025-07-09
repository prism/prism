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

package org.prism_mc.prism.bukkit.services.query;

import com.google.inject.Inject;
import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.bukkit.actions.types.BukkitActionTypeRegistry;
import org.prism_mc.prism.bukkit.api.activities.BukkitActivityQuery;
import org.prism_mc.prism.bukkit.integrations.worldedit.WorldEditIntegration;
import org.prism_mc.prism.bukkit.services.messages.MessageService;
import org.prism_mc.prism.bukkit.utils.LocationUtils;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class QueryService {

    /**
     * The action registry.
     */
    private final BukkitActionTypeRegistry actionRegistry;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * The world edit integration.
     */
    private final WorldEditIntegration worldEditIntegration;

    /**
     * The query service.
     *
     * @param actionRegistry The action registry
     */
    @Inject
    public QueryService(
        BukkitActionTypeRegistry actionRegistry,
        ConfigurationService configurationService,
        MessageService messageService,
        @Nullable WorldEditIntegration worldEditIntegration
    ) {
        this.actionRegistry = actionRegistry;
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.worldEditIntegration = worldEditIntegration;
    }

    /**
     * Start a query builder from command-derived parameters.
     *
     * @param sender The command sender
     * @param arguments The arguments
     * @return The activity query builder
     */
    public Optional<BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?>> queryFromArguments(
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
    public Optional<BukkitActivityQuery.BukkitActivityQueryBuilder<?, ?>> queryFromArguments(
        CommandSender sender,
        Arguments arguments,
        Location referenceLocation
    ) {
        var builder = BukkitActivityQuery.builder();
        World world = referenceLocation != null ? referenceLocation.getWorld() : null;

        // No-group flag
        if (arguments.hasFlag("nogroup")) {
            builder.grouped(false);
        }

        // If an ID is provided, no other parameters matter
        if (arguments.getListArgument("id", Integer.class).isPresent()) {
            builder.activityIds(arguments.getListArgument("id", Integer.class).get());

            return Optional.of(builder);
        }

        // Read "world" parameter from arguments or defaults
        String worldName = null;
        if (arguments.getArgument("world", String.class).isPresent()) {
            worldName = arguments.getArgument("world", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("world")
        ) {
            worldName = configurationService.prismConfig().defaults().parameters().get("world");

            builder.defaultUsed("world:" + worldName);
        }

        // Attempt to resolve world name into server world
        if (worldName != null) {
            world = Bukkit.getServer().getWorld(worldName);

            if (world != null) {
                builder.worldUuid(world.getUID());
            } else {
                messageService.errorParamInvalidWorld(sender);

                return Optional.empty();
            }
        }

        // Read "at" parameter from arguments or defaults
        String at = null;
        if (arguments.getArgument("at", String.class).isPresent()) {
            at = arguments.getArgument("at", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("at")
        ) {
            at = configurationService.prismConfig().defaults().parameters().get("at");

            builder.defaultUsed("at:" + at);
        }

        // Attempt to resolve at to a location
        if (at != null) {
            if (world == null) {
                messageService.errorParamAtNoWorld(sender);

                return Optional.empty();
            }

            String[] segments = at.split(",");
            if (segments.length == 3) {
                int x = Integer.parseInt(segments[0]);
                int y = Integer.parseInt(segments[1]);
                int z = Integer.parseInt(segments[2]);

                builder.referenceCoordinate(new Coordinate(x, y, z));
                builder.worldUuid(world.getUID());
            } else {
                messageService.errorParamAtInvalidLocation(sender);

                return Optional.empty();
            }
        }

        // Read "in" parameter from arguments or defaults
        String in = null;
        if (arguments.getArgument("in", String.class).isPresent()) {
            in = arguments.getArgument("in", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            !(sender instanceof ConsoleCommandSender) &&
            configurationService.prismConfig().defaults().parameters().containsKey("in")
        ) {
            in = configurationService.prismConfig().defaults().parameters().get("in");

            builder.defaultUsed("in:" + in);
        }

        // Attempt to resolve to a region
        if (in != null) {
            if (referenceLocation == null && at == null) {
                messageService.errorParamConsoleIn(sender);

                return Optional.empty();
            }

            if (in.equalsIgnoreCase("worldedit")) {
                if (worldEditIntegration != null) {
                    var regionBounds = worldEditIntegration.getRegionBounds((Player) sender);

                    if (regionBounds != null) {
                        builder.boundingCoordinates(regionBounds.key(), regionBounds.value());
                    } else {
                        messageService.errorWorldEditMissingSelection(sender);

                        return Optional.empty();
                    }
                } else {
                    messageService.errorWorldEditMissing(sender);

                    return Optional.empty();
                }
            } else {
                parseIn(builder, referenceLocation, in);
            }
        }

        // Read "r" parameter from arguments or defaults
        Integer r = null;
        if (arguments.getArgument("r", Integer.class).isPresent()) {
            if (in != null) {
                messageService.errorParamRadiusAndIn(sender);

                return Optional.empty();
            }

            r = arguments.getArgument("r", Integer.class).get();
        } else if (
            in == null &&
            at == null &&
            !(sender instanceof ConsoleCommandSender) &&
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("r")
        ) {
            r = Integer.parseInt(configurationService.prismConfig().defaults().parameters().get("r"));

            builder.defaultUsed("r:" + r);
        }

        // Attempt to resolve to a location
        if (r != null) {
            if (referenceLocation == null && at == null) {
                messageService.errorParamConsoleRadius(sender);

                return Optional.empty();
            } else if (referenceLocation != null && at == null) {
                builder.referenceCoordinate(
                    new Coordinate(referenceLocation.getX(), referenceLocation.getY(), referenceLocation.getZ())
                );

                builder.worldUuid(world.getUID());
                builder.radius(r);
            }
        } else if (at != null) {
            builder.coordinateFromReferenceCoordinate();
        }

        // Read "bounds" parameter from arguments or defaults
        String bounds = null;
        if (arguments.getArgument("bounds", String.class).isPresent()) {
            bounds = arguments.getArgument("bounds", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("bounds")
        ) {
            bounds = configurationService.prismConfig().defaults().parameters().get("bounds");

            builder.defaultUsed("bounds:" + bounds);
        }

        // Attempt to resolve to a boundary
        if (bounds != null) {
            if (world == null) {
                messageService.errorParamConsoleBounds(sender);

                return Optional.empty();
            }

            String[] segments = bounds.split("/");
            if (segments.length != 2) {
                messageService.errorParamBoundsInvalid(sender);

                return Optional.empty();
            }

            String[] minSegments = segments[0].split(",");
            if (minSegments.length != 3) {
                messageService.errorParamBoundsInvalid(sender);

                return Optional.empty();
            }

            String[] maxSegments = segments[1].split(",");
            if (maxSegments.length != 3) {
                messageService.errorParamBoundsInvalid(sender);

                return Optional.empty();
            }

            int minX = Integer.parseInt(minSegments[0]);
            int minY = Integer.parseInt(minSegments[1]);
            int minZ = Integer.parseInt(minSegments[2]);

            int maxX = Integer.parseInt(maxSegments[0]);
            int maxY = Integer.parseInt(maxSegments[1]);
            int maxZ = Integer.parseInt(maxSegments[2]);

            builder.worldUuid(world.getUID());
            Coordinate min = new Coordinate(minX, minY, minZ);
            Coordinate max = new Coordinate(maxX, maxY, maxZ);
            builder.boundingCoordinates(min, max);
        }

        if (at != null && r == null && in == null && bounds == null) {
            builder.coordinateFromReferenceCoordinate();
        }

        // Read "before" parameter from arguments or defaults
        String before = null;
        if (arguments.getArgument("before", String.class).isPresent()) {
            before = arguments.getArgument("before", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("before")
        ) {
            before = configurationService.prismConfig().defaults().parameters().get("before");

            builder.defaultUsed("before:" + before);
        }

        // Attempt to resolve to a timestamp
        if (before != null) {
            parseBefore(builder, before);
        }

        // Read "since" parameter from arguments or defaults
        String since = null;
        if (arguments.getArgument("since", String.class).isPresent()) {
            since = arguments.getArgument("since", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("since")
        ) {
            since = configurationService.prismConfig().defaults().parameters().get("since");

            builder.defaultUsed("since:" + since);
        }

        // Attempt to resolve to a timestamp
        if (since != null) {
            parseSince(builder, since);
        }

        // Read "cause" parameter from arguments or defaults
        String cause = null;
        if (arguments.getArgument("c", String.class).isPresent()) {
            cause = arguments.getArgument("c", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("c")
        ) {
            cause = configurationService.prismConfig().defaults().parameters().get("c");

            builder.defaultUsed("c:" + cause);
        }

        // Attempt to resolve to a timestamp
        if (cause != null) {
            builder.namedCause(cause);
        }

        // Read "a" parameter from arguments or defaults
        List<String> a = new ArrayList<>();
        if (arguments.getListArgument("a", String.class).isPresent()) {
            a = arguments.getListArgument("a", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("a")
        ) {
            String activityString = configurationService.prismConfig().defaults().parameters().get("a");
            a = Arrays.stream(activityString.split(",")).toList();

            builder.defaultUsed("a:" + activityString);
        }

        // Attempt to resolve to an activity
        if (!a.isEmpty()) {
            parseActions(builder, a);
        }

        // Read "b" parameter from arguments or defaults
        final Set<String> blocks = new HashSet<>();
        if (arguments.getListArgument("b", String.class).isPresent()) {
            arguments
                .getListArgument("b", String.class)
                .get()
                .forEach(block -> {
                    blocks.add(block.toLowerCase(Locale.ENGLISH));
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("b")
        ) {
            String blocksString = configurationService.prismConfig().defaults().parameters().get("b");
            Collections.addAll(blocks, blocksString.split(","));

            builder.defaultUsed("b:" + blocksString);
        }

        // Read "btag" parameter from arguments or defaults
        final List<String> blockTags = new ArrayList<>();
        if (arguments.getListArgument("btag", String.class).isPresent()) {
            blockTags.addAll(arguments.getListArgument("btag", String.class).get());
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("btag")
        ) {
            String blockTagString = configurationService.prismConfig().defaults().parameters().get("btag");
            Collections.addAll(blockTags, blockTagString.split(","));

            builder.defaultUsed("btag:" + blockTagString);
        }

        // Attempt to resolve to block tags
        if (!blockTags.isEmpty()) {
            for (String blockTag : blockTags) {
                var namespacedKey = NamespacedKey.fromString(blockTag);
                if (namespacedKey == null) {
                    messageService.errorParamInvalidNamespace(sender);

                    return Optional.empty();
                }

                var tag = Bukkit.getTag("blocks", namespacedKey, Material.class);
                if (tag == null) {
                    messageService.errorParamInvalidBlockTag(sender);

                    return Optional.empty();
                }

                tag
                    .getValues()
                    .forEach(material -> {
                        blocks.add(material.toString().toLowerCase(Locale.ENGLISH));
                    });
            }
        }

        if (!blocks.isEmpty()) {
            builder.affectedBlocks(blocks);
        }

        // Read "bc" parameter from arguments or defaults
        final Set<String> causeBlocks = new HashSet<>();
        if (arguments.getListArgument("bc", String.class).isPresent()) {
            arguments
                .getListArgument("bc", String.class)
                .get()
                .forEach(block -> {
                    causeBlocks.add(block.toLowerCase(Locale.ENGLISH));
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("bc")
        ) {
            String blocksString = configurationService.prismConfig().defaults().parameters().get("bc");
            Collections.addAll(causeBlocks, blocksString.split(","));

            builder.defaultUsed("bc:" + blocksString);
        }

        if (!causeBlocks.isEmpty()) {
            builder.causeBlocks(causeBlocks);
        }

        // Read "i" parameter from arguments or defaults
        final Set<String> materials = new HashSet<>();
        if (arguments.getListArgument("i", Material.class).isPresent()) {
            arguments
                .getListArgument("i", Material.class)
                .get()
                .forEach(material -> {
                    materials.add(material.toString().toLowerCase(Locale.ENGLISH));
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("i")
        ) {
            String materialString = configurationService.prismConfig().defaults().parameters().get("i");
            Collections.addAll(materials, materialString.split(","));

            builder.defaultUsed("i:" + materialString);
        }

        // Read "itag" parameter from arguments or defaults
        final List<String> itemTags = new ArrayList<>();
        if (arguments.getListArgument("itag", String.class).isPresent()) {
            itemTags.addAll(arguments.getListArgument("itag", String.class).get());
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("itag")
        ) {
            String itemTagString = configurationService.prismConfig().defaults().parameters().get("itag");
            Collections.addAll(itemTags, itemTagString.split(","));

            builder.defaultUsed("itag:" + itemTagString);
        }

        // Attempt to resolve to item tags
        if (!itemTags.isEmpty()) {
            for (String itemTag : itemTags) {
                var namespacedKey = NamespacedKey.fromString(itemTag);
                if (namespacedKey == null) {
                    messageService.errorParamInvalidNamespace(sender);

                    return Optional.empty();
                }

                var tag = Bukkit.getTag("items", namespacedKey, Material.class);
                if (tag == null) {
                    messageService.errorParamInvalidItemTag(sender);

                    return Optional.empty();
                }

                tag
                    .getValues()
                    .forEach(material -> {
                        materials.add(material.toString().toLowerCase(Locale.ENGLISH));
                    });
            }
        }

        if (!materials.isEmpty()) {
            builder.affectedMaterials(materials);
        }

        // Read "e" parameter from arguments or defaults
        final Set<String> entityTypes = new HashSet<>();
        if (arguments.getListArgument("e", EntityType.class).isPresent()) {
            arguments
                .getListArgument("e", EntityType.class)
                .get()
                .forEach(entity -> {
                    entityTypes.add(entity.toString().toLowerCase(Locale.ENGLISH));
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("e")
        ) {
            String entityString = configurationService.prismConfig().defaults().parameters().get("e");
            Collections.addAll(entityTypes, entityString.split(","));

            builder.defaultUsed("e:" + entityString);
        }

        // Read "etag" parameter from arguments or defaults
        final List<String> entityTypeTags = new ArrayList<>();
        if (arguments.getListArgument("etag", String.class).isPresent()) {
            entityTypeTags.addAll(arguments.getListArgument("etag", String.class).get());
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("etag")
        ) {
            String entityTypeTagString = configurationService.prismConfig().defaults().parameters().get("etag");
            Collections.addAll(entityTypeTags, entityTypeTagString.split(","));

            builder.defaultUsed("etag:" + entityTypeTagString);
        }

        // Attempt to resolve to entity type tags
        if (!entityTypeTags.isEmpty()) {
            for (String entityTypeTag : entityTypeTags) {
                var namespacedKey = NamespacedKey.fromString(entityTypeTag);
                if (namespacedKey == null) {
                    messageService.errorParamInvalidNamespace(sender);

                    return Optional.empty();
                }

                var tag = Bukkit.getTag("entity_types", namespacedKey, EntityType.class);
                if (tag == null) {
                    messageService.errorParamInvalidEntityTypeTag(sender);

                    return Optional.empty();
                }

                tag
                    .getValues()
                    .forEach(entityType -> {
                        entityTypes.add(entityType.toString().toLowerCase(Locale.ENGLISH));
                    });
            }
        }

        // Add affected entity types
        if (!entityTypes.isEmpty()) {
            builder.affectedEntityTypes(entityTypes);
        }

        // Read "ec" parameter from arguments or defaults
        final Set<String> causeEntityTypes = new HashSet<>();
        if (arguments.getListArgument("ec", EntityType.class).isPresent()) {
            arguments
                .getListArgument("ec", EntityType.class)
                .get()
                .forEach(entity -> {
                    causeEntityTypes.add(entity.toString().toLowerCase(Locale.ENGLISH));
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("ec")
        ) {
            String entityString = configurationService.prismConfig().defaults().parameters().get("ec");
            Collections.addAll(causeEntityTypes, entityString.split(","));

            builder.defaultUsed("ec:" + entityString);
        }

        // Add cause entity types
        if (!causeEntityTypes.isEmpty()) {
            builder.causeEntityTypes(causeEntityTypes);
        }

        // Read "p" parameter from arguments or defaults
        final List<String> p = new ArrayList<>();
        if (arguments.getListArgument("p", OfflinePlayer.class).isPresent()) {
            arguments
                .getListArgument("p", OfflinePlayer.class)
                .get()
                .forEach(player -> {
                    p.add(player.getName());
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("p")
        ) {
            String playerString = configurationService.prismConfig().defaults().parameters().get("p");
            Collections.addAll(p, playerString.split(","));

            builder.defaultUsed("p:" + playerString);
        }

        // Attempt to resolve players
        if (!p.isEmpty()) {
            builder.affectedPlayerNames(p);
            builder.causePlayerNames(p);
        }

        // Read "pa" parameter from arguments or defaults
        final List<String> pa = new ArrayList<>();
        if (arguments.getListArgument("pa", OfflinePlayer.class).isPresent()) {
            arguments
                .getListArgument("pa", OfflinePlayer.class)
                .get()
                .forEach(player -> {
                    pa.add(player.getName());
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("pa")
        ) {
            String playerString = configurationService.prismConfig().defaults().parameters().get("pa");
            Collections.addAll(pa, playerString.split(","));

            builder.defaultUsed("pa:" + playerString);
        }

        // Attempt to resolve players
        if (!pa.isEmpty()) {
            builder.affectedPlayerNames(pa);
        }

        // Read "pc" parameter from arguments or defaults
        final List<String> pc = new ArrayList<>();
        if (arguments.getListArgument("pc", OfflinePlayer.class).isPresent()) {
            arguments
                .getListArgument("pc", OfflinePlayer.class)
                .get()
                .forEach(player -> {
                    pc.add(player.getName());
                });
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("pc")
        ) {
            String playerString = configurationService.prismConfig().defaults().parameters().get("pc");
            Collections.addAll(pa, playerString.split(","));

            builder.defaultUsed("pc:" + playerString);
        }

        // Attempt to resolve players
        if (!pc.isEmpty()) {
            builder.causePlayerNames(pc);
        }

        // reversed: parameter
        if (arguments.getArgument("reversed", Boolean.class).isPresent()) {
            builder.reversed(arguments.getArgument("reversed", Boolean.class).get());
        }

        // Read "reversed" parameter from arguments or defaults
        Boolean reversed = null;
        if (arguments.getArgument("reversed", Boolean.class).isPresent()) {
            reversed = arguments.getArgument("reversed", Boolean.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("reversed")
        ) {
            reversed = configurationService
                .prismConfig()
                .defaults()
                .parameters()
                .get("reversed")
                .equalsIgnoreCase("true");

            builder.defaultUsed("reversed:" + reversed);
        }

        // Attempt to resolve to a boolean
        if (reversed != null) {
            builder.reversed(reversed);
        }

        // Read "q" parameter from arguments or defaults
        String query = null;
        if (arguments.getArgument("q", String.class).isPresent()) {
            query = arguments.getArgument("q", String.class).get();
        } else if (
            !arguments.hasFlag("nodefaults") &&
            configurationService.prismConfig().defaults().parameters().containsKey("q")
        ) {
            query = configurationService.prismConfig().defaults().parameters().get("q");

            builder.defaultUsed("q:" + query);
        }

        if (query != null) {
            builder.query(query);
        }

        return Optional.of(builder);
    }

    /**
     * Parse and apply the "action" parameter to a query builder.
     *
     * @param query The query
     * @param actions An action name, names, family, or families
     */
    protected void parseActions(ActivityQuery.ActivityQueryBuilder query, List<String> actions) {
        for (String actionKey : actions) {
            if (actionKey.contains("-")) {
                var optionalIActionType = actionRegistry.actionType(actionKey.toLowerCase(Locale.ENGLISH));
                if (optionalIActionType.isPresent()) {
                    query.actionTypeKey(actionKey);
                }
            } else {
                query.actionTypes(actionRegistry.actionTypesInFamily(actionKey.toLowerCase(Locale.ENGLISH)));
            }
        }
    }

    /**
     * Parse and apply the "in" parameter to a query builder.
     *
     * @param builder The builder
     * @param referenceLocation The reference location
     * @param in The in param
     */
    protected void parseIn(ActivityQuery.ActivityQueryBuilder builder, Location referenceLocation, String in) {
        builder.worldUuid(referenceLocation.getWorld().getUID());

        if (in.equalsIgnoreCase("chunk")) {
            Chunk chunk = referenceLocation.getChunk();
            Coordinate chunkMin = LocationUtils.getChunkMinCoordinate(chunk);
            Coordinate chunkMax = LocationUtils.getChunkMaxCoordinate(chunk);

            builder.boundingCoordinates(chunkMin, chunkMax);
        }
    }

    /**
     * Parse and apply the "before" parameter.
     *
     * @param builder The builder
     * @param since The duration string
     */
    protected void parseBefore(ActivityQuery.ActivityQueryBuilder builder, String since) {
        Long parsedTimestamp = parseTimestamp(since);
        if (parsedTimestamp != null) {
            builder.before(parsedTimestamp);
        }
    }

    /**
     * Parse and apply the "since" parameter.
     *
     * @param builder The builder
     * @param since The duration string
     */
    protected void parseSince(ActivityQuery.ActivityQueryBuilder builder, String since) {
        Long parsedTimestamp = parseTimestamp(since);
        if (parsedTimestamp != null) {
            builder.after(parsedTimestamp);
        }
    }

    /**
     * Parses a string duration into a unix timestamp.
     *
     * @return The timestamp
     */
    public static Long parseTimestamp(String value) {
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
                    default:
                        return null;
                }
            }
        }

        return cal.getTime().getTime() / 1000;
    }
}
