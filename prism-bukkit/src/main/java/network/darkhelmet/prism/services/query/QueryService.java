/*
 * Prism (Refracted)
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

package network.darkhelmet.prism.services.query;

import com.google.inject.Inject;

import dev.triumphteam.cmd.core.argument.named.Arguments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import network.darkhelmet.prism.actions.types.ActionTypeRegistry;
import network.darkhelmet.prism.api.actions.types.IActionType;
import network.darkhelmet.prism.api.activities.ActivityQuery;
import network.darkhelmet.prism.api.util.Coordinate;
import network.darkhelmet.prism.utils.LocationUtils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class QueryService {
    /**
     * The action registry.
     */
    private final ActionTypeRegistry actionRegistry;

    /**
     * The query service.
     *
     * @param actionRegistry The action registry
     */
    @Inject
    public QueryService(ActionTypeRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }

    /**
     * Start a query builder from command-derived parameters.
     *
     * @param referenceLocation The reference location
     * @param arguments The arguments
     * @return The activity query builder
     */
    public ActivityQuery queryFromArguments(Location referenceLocation, Arguments arguments) {
        List<String> actions = null;
        String in = null;
        String at = null;
        Integer radius = null;
        List<String> materials = null;
        List<String> entityTypes = null;
        List<String> playerNames = null;
        String before = null;
        String since = null;

        if (arguments.get("r", Integer.class).isPresent()) {
            radius = arguments.get("r", Integer.class).get();
        }

        if (arguments.get("in", String.class).isPresent()) {
            in = arguments.get("in", String.class).get();
        }

        if (arguments.get("before", String.class).isPresent()) {
            before = arguments.get("before", String.class).get();
        }

        if (arguments.get("since", String.class).isPresent()) {
            since = arguments.get("since", String.class).get();
        }

        if (arguments.get("at", String.class).isPresent()) {
            since = arguments.get("at", String.class).get();
        }

        if (arguments.getAsList("a", String.class).isPresent()) {
            actions = arguments.getAsList("a", String.class).get();
        }

        if (arguments.getAsList("m", Material.class).isPresent()) {
            List<String> finalMaterials = new ArrayList<>();
            arguments.getAsList("m", Material.class).get().forEach(m -> {
                finalMaterials.add(m.toString());
            });

            materials = finalMaterials;
        }

        if (arguments.getAsList("e", EntityType.class).isPresent()) {
            List<String> finalEntityTypes = new ArrayList<>();
            arguments.getAsList("e", EntityType.class).get().forEach(e -> {
                finalEntityTypes.add(e.toString());
            });

            entityTypes = finalEntityTypes;
        }

        if (arguments.getAsList("p", String.class).isPresent()) {
            playerNames = arguments.getAsList("p", String.class).get();
        }

        return queryFromParameters(
            referenceLocation, actions, in, at, radius, materials, entityTypes, playerNames, before, since);
    }

    /**
     * Start a query builder from parameters.
     *
     * @param referenceLocation The reference location
     * @param actions The actions
     * @param in The "in" parameter
     * @param radius The radius parameter
     * @return The activity query builder
     */
    public ActivityQuery queryFromParameters(
            Location referenceLocation,
            List<String> actions,
            String in,
            String at,
            Integer radius,
            List<String> materials,
            List<String> entityTypes,
            List<String> playerNames,
            String before,
            String since) {
        ActivityQuery builder = new ActivityQuery();

        // At. If set, use this as the reference location.
        if (at != null) {
            String[] segments = at.split(",");
            if (segments.length == 3) {
                int x = Integer.parseInt(segments[0]);
                int y = Integer.parseInt(segments[1]);
                int z = Integer.parseInt(segments[2]);

                referenceLocation = new Location(referenceLocation.getWorld(), x, y, z);
            } else {
                throw new IllegalArgumentException("param-error-at-invalid-loc");
            }
        }

        // Actions
        if (actions != null) {
            parseActions(builder, actions);
        }

        // Entity Type
        if (entityTypes != null) {
            parseEntityTypes(builder, entityTypes);
        }

        // In
        if (in != null) {
            parseIn(builder, referenceLocation, in);
        }

        // Materials
        if (materials != null) {
            parseMaterials(builder, materials);
        }

        // Players
        if (playerNames != null) {
            parsePlayers(builder, playerNames);
        }

        // Radius
        if (radius != null) {
            if (in != null && in.equalsIgnoreCase("chunk")) {
                throw new IllegalArgumentException("param-error-r-and-in-chunk");
            }

            parseRadius(builder, referenceLocation, radius);
        }

        // Before
        if (before != null) {
            parseBefore(builder, before);
        }

        // Since
        if (since != null) {
            parseSince(builder, since);
        }

        return builder;
    }

    /**
     * Parse and apply the "action" parameter to a query builder.
     *
     * @param builder The builder
     * @param actions An action name, names, family, or families
     */
    protected void parseActions(ActivityQuery builder, List<String> actions) {
        for (String actionTerm : actions) {
            if (actionTerm.contains("-")) {
                Optional<IActionType> optionalIActionType = actionRegistry
                    .getActionType(actionTerm.toLowerCase(Locale.ENGLISH));
                optionalIActionType.ifPresent(builder::actionType);
            } else {
                Collection<IActionType> actionTypes = actionRegistry
                    .actionTypesInFamily(actionTerm.toLowerCase(Locale.ENGLISH));
                builder.actionTypes(actionTypes);
            }
        }
    }

    /**
     * Parse and apply the entity types parameter to a query builder.
     *
     * @param builder The builder
     * @param entityTypes The entity types parameter
     */
    protected void parseEntityTypes(ActivityQuery builder, List<String> entityTypes) {
        builder.entityTypes(entityTypes);
    }

    /**
     * Parse and apply the "in" parameter to a query builder.
     *
     * @param builder The builder
     * @param referenceLocation The reference location
     * @param in The in param
     */
    protected void parseIn(
            ActivityQuery builder, Location referenceLocation, String in) {
        if (in.equalsIgnoreCase("chunk")) {
            Chunk chunk = referenceLocation.getChunk();
            Coordinate chunkMin = LocationUtils.getChunkMinCoordinate(chunk);
            Coordinate chunkMax = LocationUtils.getChunkMaxCoordinate(chunk);

            builder.boundingCoordinates(chunkMin, chunkMax).worldUuid(referenceLocation.getWorld().getUID());
        } else if (in.equalsIgnoreCase("world")) {
            builder.worldUuid(referenceLocation.getWorld().getUID());
        }
    }

    /**
     * Parse and apply the "material" parameter to a query builder.
     *
     * @param builder The builder
     * @param materials The materials parameter
     */
    protected void parseMaterials(ActivityQuery builder, List<String> materials) {
        builder.materials(materials);
    }

    /**
     * Parse and apply the "player" parameter to a query builder.
     *
     * @param builder The builder
     * @param playerNames The player names
     */
    protected void parsePlayers(ActivityQuery builder, List<String> playerNames) {
        for (String playerName : playerNames) {
            builder.playerByName(playerName);
        }
    }

    /**
     * Parse and apply the "radius" parameter to a query builder.
     *
     * @param builder The builder
     * @param referenceLocation The reference location
     * @param radius The radius
     */
    protected void parseRadius(
            ActivityQuery builder, Location referenceLocation, Integer radius) {
        Coordinate minCoordinate = LocationUtils.getMinCoordinate(referenceLocation, radius);
        Coordinate maxCoordinate = LocationUtils.getMaxCoordinate(referenceLocation, radius);

        builder.boundingCoordinates(minCoordinate, maxCoordinate)
            .worldUuid(referenceLocation.getWorld().getUID());
    }

    /**
     * Parse and apply the "before" parameter.
     *
     * @param builder The builder
     * @param since The duration string
     */
    protected void parseBefore(ActivityQuery builder, String since) {
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
    protected void parseSince(ActivityQuery builder, String since) {
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
        final Pattern pattern = Pattern.compile("([0-9]+)(s|h|m|d|w)");
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

        return cal.getTime().getTime();
    }
}
