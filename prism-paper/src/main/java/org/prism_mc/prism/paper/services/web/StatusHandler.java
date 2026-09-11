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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.storage.StorageConnectionStatus;
import org.prism_mc.prism.api.storage.World;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.purge.PurgeService;

public class StatusHandler extends ApiHandler {

    /**
     * The plugin version.
     */
    private final String version;

    /**
     * The primary storage type.
     */
    private final String storageType;

    /**
     * The recording queue capacity.
     */
    private final int queueCapacity;

    /**
     * The write-ahead log mode.
     */
    private final String walMode;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The recording service.
     */
    private final RecordingService recordingService;

    /**
     * The purge service.
     */
    private final PurgeService purgeService;

    /**
     * The default relative time range the web UI should pre-apply as a "since" filter. May be empty.
     */
    private final String defaultActivityRange;

    /**
     * Constructor.
     *
     * @param objectMapper The object mapper
     * @param apiKey The API key
     * @param loggingService The logging service
     * @param allowedOrigins The browser origins permitted to call the API cross-origin
     * @param version The plugin version
     * @param storageType The primary storage type
     * @param queueCapacity The recording queue capacity
     * @param walMode The write-ahead log mode
     * @param storageAdapter The storage adapter
     * @param recordingService The recording service
     * @param purgeService The purge service
     * @param defaultActivityRange The default relative time range the web UI should pre-apply
     */
    protected StatusHandler(
        ObjectMapper objectMapper,
        String apiKey,
        LoggingService loggingService,
        List<String> allowedOrigins,
        String version,
        String storageType,
        int queueCapacity,
        String walMode,
        StorageAdapter storageAdapter,
        RecordingService recordingService,
        PurgeService purgeService,
        String defaultActivityRange
    ) {
        super(objectMapper, apiKey, loggingService, allowedOrigins);
        this.version = version;
        this.storageType = storageType;
        this.queueCapacity = queueCapacity;
        this.walMode = walMode;
        this.storageAdapter = storageAdapter;
        this.recordingService = recordingService;
        this.purgeService = purgeService;
        this.defaultActivityRange = defaultActivityRange;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("version", version);
        response.put("serverBrand", Bukkit.getName());
        response.put("serverVersion", Bukkit.getMinecraftVersion());
        response.put("storageType", storageType);
        response.put("storageReady", storageAdapter.ready());
        response.put("queueSize", recordingService.queue().size());
        response.put("queueCapacity", queueCapacity);
        response.put("walMode", walMode);
        response.put("purgeActive", !purgeService.queueFree());

        response.put("defaultActivityRange", defaultActivityRange == null ? "" : defaultActivityRange);
        List<World> worlds = storageAdapter.worlds();
        response.put("defaultWorldId", worlds.isEmpty() ? null : worlds.getFirst().id());

        StorageConnectionStatus connectionStatus = storageAdapter.connectionStatus();
        Map<String, Object> connection = new HashMap<>();
        connection.put("connected", connectionStatus.connected());
        connection.put("active", connectionStatus.activeConnections());
        connection.put("idle", connectionStatus.idleConnections());
        connection.put("total", connectionStatus.totalConnections());
        connection.put("max", connectionStatus.maxConnections());
        connection.put("awaiting", connectionStatus.threadsAwaitingConnection());
        response.put("connection", connection);

        sendJson(exchange, 200, response);
    }
}
