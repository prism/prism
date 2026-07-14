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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.storage.World;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class WorldsHandler extends ApiHandler {

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * Constructor.
     *
     * @param objectMapper The object mapper
     * @param apiKey The API key
     * @param loggingService The logging service
     * @param allowedOrigins The browser origins permitted to call the API cross-origin
     * @param storageAdapter The storage adapter
     */
    protected WorldsHandler(
        ObjectMapper objectMapper,
        String apiKey,
        LoggingService loggingService,
        List<String> allowedOrigins,
        StorageAdapter storageAdapter
    ) {
        super(objectMapper, apiKey, loggingService, allowedOrigins);
        this.storageAdapter = storageAdapter;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        List<Map<String, Object>> worlds = new ArrayList<>();
        for (World world : storageAdapter.worlds()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", world.id());
            map.put("name", world.name());
            map.put("uuid", world.uuid());
            worlds.add(map);
        }

        sendJson(exchange, 200, worlds);
    }
}
