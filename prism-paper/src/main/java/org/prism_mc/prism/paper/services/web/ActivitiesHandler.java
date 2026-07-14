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

package org.prism_mc.prism.paper.services.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.actions.BlockAction;
import org.prism_mc.prism.api.actions.CustomData;
import org.prism_mc.prism.api.actions.EntityAction;
import org.prism_mc.prism.api.actions.ItemAction;
import org.prism_mc.prism.api.actions.MaterialAction;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.activities.Cause;
import org.prism_mc.prism.api.activities.GroupedActivity;
import org.prism_mc.prism.api.containers.BlockContainer;
import org.prism_mc.prism.api.containers.Container;
import org.prism_mc.prism.api.containers.EntityContainer;
import org.prism_mc.prism.api.containers.IdentityContainer;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.api.containers.TranslatableContainer;
import org.prism_mc.prism.api.services.pagination.PartialListPaginationResult;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.utils.QueryParsingUtil;

public class ActivitiesHandler extends ApiHandler {

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The maximum number of results a query may return.
     */
    private final int maxResults;

    /**
     * Constructor.
     *
     * @param objectMapper The object mapper
     * @param apiKey The API key
     * @param loggingService The logging service
     * @param allowedOrigins The browser origins permitted to call the API cross-origin
     * @param storageAdapter The storage adapter
     * @param maxResults The maximum number of results a query may return
     */
    protected ActivitiesHandler(
        ObjectMapper objectMapper,
        String apiKey,
        LoggingService loggingService,
        List<String> allowedOrigins,
        StorageAdapter storageAdapter,
        int maxResults
    ) {
        super(objectMapper, apiKey, loggingService, allowedOrigins);
        this.storageAdapter = storageAdapter;
        this.maxResults = maxResults;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        Map<String, String> params = parseQueryParams(exchange);

        ActivityQuery.ActivityQueryBuilder<?, ?> builder = ActivityQuery.builder().lookup(true).grouped(true);

        if (params.containsKey("action")) {
            builder.actionTypeKeys(Arrays.asList(params.get("action").split(",")));
        }
        if (params.containsKey("excludeAction")) {
            builder.actionTypeKeysExcluded(Arrays.asList(params.get("excludeAction").split(",")));
        }

        // Cause players
        if (params.containsKey("causePlayer")) {
            builder.causePlayerNames(Arrays.asList(params.get("causePlayer").split(",")));
        }
        if (params.containsKey("excludeCausePlayer")) {
            builder.causePlayerNamesExcluded(Arrays.asList(params.get("excludeCausePlayer").split(",")));
        }

        // Affected players
        if (params.containsKey("affectedPlayer")) {
            builder.affectedPlayerNames(Arrays.asList(params.get("affectedPlayer").split(",")));
        }
        if (params.containsKey("excludeAffectedPlayer")) {
            builder.affectedPlayerNamesExcluded(Arrays.asList(params.get("excludeAffectedPlayer").split(",")));
        }

        if (params.containsKey("since")) {
            Long since = QueryParsingUtil.parseRelativeTimestamp(params.get("since"));
            if (since != null) {
                builder.after(since);
            }
        }

        if (params.containsKey("before")) {
            Long before = QueryParsingUtil.parseRelativeTimestamp(params.get("before"));
            if (before != null) {
                builder.before(before);
            }
        }

        // Worlds are filtered by their storage id because world names are not unique.
        if (params.containsKey("world")) {
            try {
                builder.worldId(Integer.parseInt(params.get("world")));
            } catch (NumberFormatException ignored) {
                // Ignore a malformed world id
            }
        }
        if (params.containsKey("excludeWorld")) {
            try {
                builder.worldIdExcluded(Integer.parseInt(params.get("excludeWorld")));
            } catch (NumberFormatException ignored) {
                // Ignore a malformed world id
            }
        }

        // Affected blocks (by name)
        if (params.containsKey("block")) {
            builder.affectedBlocks(Arrays.asList(params.get("block").split(",")));
        }
        if (params.containsKey("excludeBlock")) {
            builder.affectedBlocksExcluded(Arrays.asList(params.get("excludeBlock").split(",")));
        }

        // Affected blocks (by tag, resolved to names)
        if (params.containsKey("blockTag")) {
            List<String> blockNames = QueryParsingUtil.resolveBlockTags(params.get("blockTag"));
            if (!blockNames.isEmpty()) {
                builder.affectedBlocks(blockNames);
            }
        }
        if (params.containsKey("excludeBlockTag")) {
            List<String> blockNames = QueryParsingUtil.resolveBlockTags(params.get("excludeBlockTag"));
            if (!blockNames.isEmpty()) {
                builder.affectedBlocksExcluded(blockNames);
            }
        }

        // Cause blocks (by name)
        if (params.containsKey("causeBlock")) {
            builder.causeBlocks(Arrays.asList(params.get("causeBlock").split(",")));
        }
        if (params.containsKey("excludeCauseBlock")) {
            builder.causeBlocksExcluded(Arrays.asList(params.get("excludeCauseBlock").split(",")));
        }

        // Affected entities (by name)
        if (params.containsKey("entity")) {
            builder.affectedEntityTypes(Arrays.asList(params.get("entity").split(",")));
        }
        if (params.containsKey("excludeEntity")) {
            builder.affectedEntityTypesExcluded(Arrays.asList(params.get("excludeEntity").split(",")));
        }

        // Affected entities (by tag, resolved to names)
        if (params.containsKey("entityTag")) {
            List<String> entityNames = QueryParsingUtil.resolveEntityTypeTags(params.get("entityTag"));
            if (!entityNames.isEmpty()) {
                builder.affectedEntityTypes(entityNames);
            }
        }
        if (params.containsKey("excludeEntityTag")) {
            List<String> entityNames = QueryParsingUtil.resolveEntityTypeTags(params.get("excludeEntityTag"));
            if (!entityNames.isEmpty()) {
                builder.affectedEntityTypesExcluded(entityNames);
            }
        }

        // Cause entities (by name)
        if (params.containsKey("causeEntity")) {
            builder.causeEntityTypes(Arrays.asList(params.get("causeEntity").split(",")));
        }
        if (params.containsKey("excludeCauseEntity")) {
            builder.causeEntityTypesExcluded(Arrays.asList(params.get("excludeCauseEntity").split(",")));
        }

        // Affected items (by name)
        if (params.containsKey("item")) {
            builder.affectedMaterials(Arrays.asList(params.get("item").split(",")));
        }
        if (params.containsKey("excludeItem")) {
            builder.affectedMaterialsExcluded(Arrays.asList(params.get("excludeItem").split(",")));
        }

        // Affected items (by tag, resolved to names)
        if (params.containsKey("itemTag")) {
            List<String> materialNames = QueryParsingUtil.resolveItemTags(params.get("itemTag"));
            if (!materialNames.isEmpty()) {
                builder.affectedMaterials(materialNames);
            }
        }
        if (params.containsKey("excludeItemTag")) {
            List<String> materialNames = QueryParsingUtil.resolveItemTags(params.get("excludeItemTag"));
            if (!materialNames.isEmpty()) {
                builder.affectedMaterialsExcluded(materialNames);
            }
        }

        if (params.containsKey("at")) {
            Coordinate coord = QueryParsingUtil.parseCoordinate(params.get("at"));
            if (coord != null) {
                builder.coordinate(coord);
            }
        }

        if (params.containsKey("minBound") && params.containsKey("maxBound")) {
            Coordinate min = QueryParsingUtil.parseCoordinate(params.get("minBound"));
            Coordinate max = QueryParsingUtil.parseCoordinate(params.get("maxBound"));
            if (min != null && max != null) {
                builder.boundingCoordinates(min, max);
            }
        }

        if (params.containsKey("reversed")) {
            builder.reversed(Boolean.parseBoolean(params.get("reversed")));
        }

        if (params.containsKey("grouped")) {
            builder.grouped(Boolean.parseBoolean(params.get("grouped")));
        }

        if (params.containsKey("sort")) {
            if ("asc".equalsIgnoreCase(params.get("sort"))) {
                builder.sort(ActivityQuery.Sort.ASCENDING);
            } else {
                builder.sort(ActivityQuery.Sort.DESCENDING);
            }
        }

        int limit = 100;
        if (params.containsKey("limit")) {
            try {
                limit = Math.max(1, Math.min(Integer.parseInt(params.get("limit")), maxResults));
            } catch (NumberFormatException ignored) {
                // Use default
            }
        }
        builder.limit(limit);

        if (params.containsKey("offset")) {
            try {
                builder.offset(Math.max(0, Integer.parseInt(params.get("offset"))));
            } catch (NumberFormatException ignored) {
                // Use default
            }
        }

        PartialListPaginationResult<AbstractActivity> result = storageAdapter.queryActivitiesPaginated(builder.build());

        List<Map<String, Object>> activities = new ArrayList<>();
        for (AbstractActivity abstractActivity : result.results()) {
            activities.add(serializeActivity(abstractActivity));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("activities", activities);
        response.put("totalResults", result.totalResults());
        response.put("hasNextPage", result.hasNextPage());
        response.put("count", activities.size());

        sendJson(exchange, 200, response);
    }

    /**
     * Serialize an activity into a JSON-friendly map.
     *
     * @param activity The activity
     * @return The serialized activity
     */
    private Map<String, Object> serializeActivity(AbstractActivity activity) {
        Map<String, Object> map = new HashMap<>();
        Action action = activity.action();
        map.put("actionType", action.type().key());
        map.put("descriptor", action.descriptor());
        map.put("timestamp", activity.timestamp());
        map.put("reversed", activity.reversed());

        if (activity.cause() != null) {
            Map<String, Object> cause = serializeCause(activity.cause());
            if (cause != null) {
                map.put("cause", cause);
            }
        }

        if (activity.world() != null) {
            map.put("world", activity.world().value());
        }

        if (activity.coordinate() != null) {
            Map<String, Object> coord = new HashMap<>();
            coord.put("x", activity.coordinate().intX());
            coord.put("y", activity.coordinate().intY());
            coord.put("z", activity.coordinate().intZ());
            map.put("coordinate", coord);
        }

        if (activity instanceof Activity act) {
            map.put("id", act.primaryKey());
        }

        if (activity instanceof GroupedActivity grouped) {
            map.put("count", grouped.count());
        }

        if (action instanceof BlockAction blockAction) {
            map.put("block", serializeBlock(blockAction.blockContainer()));
            BlockContainer replaced = blockAction.replacedBlockContainer();
            if (replaced != null) {
                map.put("replacedBlock", serializeBlock(replaced));
            }
        }

        if (action instanceof EntityAction entityAction) {
            Map<String, Object> entity = new HashMap<>();
            entity.put("type", entityAction.entityContainer().serializeEntityType());
            map.put("entity", entity);
        }

        if (action instanceof ItemAction itemAction) {
            Map<String, Object> item = new HashMap<>();
            item.put("material", itemAction.serializeMaterial());
            item.put("quantity", itemAction.quantity());
            String itemData = itemAction.serializeItemData();
            if (itemData != null && !itemData.isEmpty()) {
                item.put("data", itemData);
            }
            map.put("item", item);
        } else if (action instanceof MaterialAction materialAction) {
            Map<String, Object> item = new HashMap<>();
            item.put("material", materialAction.serializeMaterial());
            map.put("item", item);
        }

        if (action instanceof CustomData customData && customData.hasCustomData()) {
            String custom = customData.serializeCustomData();
            if (custom != null && !custom.isEmpty()) {
                map.put("customData", custom);
            }
        }

        return map;
    }

    /**
     * Serialize a block container into a JSON-friendly map.
     *
     * @param container The block container
     * @return The serialized block
     */
    private Map<String, Object> serializeBlock(BlockContainer container) {
        Map<String, Object> block = new HashMap<>();
        block.put("namespace", container.blockNamespace());
        block.put("name", container.blockName());
        String blockData = container.serializeBlockData();
        if (blockData != null && !blockData.isEmpty()) {
            block.put("data", blockData);
        }
        return block;
    }

    /**
     * Serialize an activity's cause into a JSON-friendly map.
     *
     * @param cause The cause
     * @return The serialized cause, or null if the cause has no container
     */
    private Map<String, Object> serializeCause(Cause cause) {
        Container container = cause.container();
        if (container == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        if (container instanceof PlayerContainer player) {
            map.put("type", "player");
            map.put("value", player.name());
            if (player.uuid() != null) {
                map.put("uuid", player.uuid().toString());
            }
        } else if (container instanceof IdentityContainer identity) {
            map.put("type", "identity");
            map.put("value", identity.name());
            if (identity.uuid() != null) {
                map.put("uuid", identity.uuid().toString());
            }
        } else if (container instanceof BlockContainer block) {
            map.put("type", "block");
            map.put("value", block.blockName());
            map.put("namespace", block.blockNamespace());
        } else if (container instanceof EntityContainer entity) {
            map.put("type", "entity");
            map.put("value", entity.serializeEntityType());
        } else if (container instanceof TranslatableContainer translatable) {
            map.put("type", "translatable");
            map.put("value", translatable.translationKey());
        } else if (container instanceof StringContainer string) {
            map.put("type", "string");
            map.put("value", string.value());
        } else {
            map.put("type", "unknown");
            map.put("value", container.toString());
        }
        return map;
    }
}
