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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.WebConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.purge.PurgeService;

@Singleton
public class WebService {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The storage adapter.
     */
    private final StorageAdapter storageAdapter;

    /**
     * The recording service.
     */
    private final RecordingService recordingService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The purge service.
     */
    private final PurgeService purgeService;

    /**
     * The plugin version.
     */
    private final String version;

    /**
     * The HTTP server.
     */
    private HttpServer server;

    /**
     * Constructor.
     *
     * @param configurationService The configuration service
     * @param storageAdapter The storage adapter
     * @param recordingService The recording service
     * @param loggingService The logging service
     * @param purgeService The purge service
     * @param version The plugin version
     */
    @Inject
    public WebService(
        ConfigurationService configurationService,
        StorageAdapter storageAdapter,
        RecordingService recordingService,
        LoggingService loggingService,
        PurgeService purgeService,
        @Named("version") String version
    ) {
        this.configurationService = configurationService;
        this.storageAdapter = storageAdapter;
        this.recordingService = recordingService;
        this.loggingService = loggingService;
        this.purgeService = purgeService;
        this.version = version;
    }

    /**
     * The result of an attempt to start the web server.
     */
    public enum StartResult {
        /**
         * The server was started.
         */
        STARTED,

        /**
         * The server was already running.
         */
        ALREADY_RUNNING,

        /**
         * The web server feature is disabled in the configuration.
         */
        DISABLED,

        /**
         * No API key is configured.
         */
        NO_API_KEY,

        /**
         * The server failed to start.
         */
        FAILED,
    }

    /**
     * Check whether the web server is running.
     *
     * @return True if the server is running
     */
    public boolean running() {
        return server != null;
    }

    /**
     * Start the web server if the feature is enabled and configured to auto-start.
     */
    public void startIfAutoStart() {
        WebConfiguration config = configurationService.prismConfig().web();
        if (config.enabled() && config.autoStart()) {
            start();
        }
    }

    /**
     * Start the web server.
     *
     * @return The result of the start attempt
     */
    public StartResult start() {
        WebConfiguration config = configurationService.prismConfig().web();

        if (!config.enabled()) {
            return StartResult.DISABLED;
        }

        if (running()) {
            return StartResult.ALREADY_RUNNING;
        }

        String apiKey = config.apiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            loggingService.error(
                "Web server cannot start because no API key is configured. Set web.api-key in config."
            );
            return StartResult.NO_API_KEY;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String storageType = configurationService.storageConfig().primaryStorageType().name().toLowerCase();
            int queueMaxCapacity = configurationService.prismConfig().recording().queueMaxCapacity();
            String walMode = configurationService.prismConfig().recording().walMode();
            List<String> allowedOrigins = config.allowedOrigins();

            server = HttpServer.create(new InetSocketAddress(config.bindAddress(), config.port()), 0);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            // Prefix all contexts with the configured base path (empty by default). The proxy must
            // forward the full path without stripping the prefix.
            String prefix = config.contextPrefix();

            // API endpoints
            server.createContext(
                prefix + "/api/v1/activities",
                new ActivitiesHandler(
                    objectMapper,
                    apiKey,
                    loggingService,
                    allowedOrigins,
                    storageAdapter,
                    config.maxResults()
                )
            );

            server.createContext(
                prefix + "/api/v1/worlds",
                new WorldsHandler(objectMapper, apiKey, loggingService, allowedOrigins, storageAdapter)
            );

            server.createContext(
                prefix + "/api/v1/reports/recording-queue",
                new RecordingQueueReportHandler(
                    objectMapper,
                    apiKey,
                    loggingService,
                    allowedOrigins,
                    recordingService,
                    queueMaxCapacity
                )
            );

            server.createContext(
                prefix + "/api/v1/status",
                new StatusHandler(
                    objectMapper,
                    apiKey,
                    loggingService,
                    allowedOrigins,
                    version,
                    storageType,
                    queueMaxCapacity,
                    walMode,
                    storageAdapter,
                    recordingService,
                    purgeService,
                    config.defaultActivityRange()
                )
            );

            // Static file serving. Registered at the root so it catches asset requests under the
            // base prefix; the handler strips the prefix and injects the base href into HTML/JS/CSS.
            server.createContext("/", new StaticFileHandler(prefix, config.baseHref()));

            server.start();
            loggingService.info("Web server started on port {0}", String.valueOf(config.port()));
            return StartResult.STARTED;
        } catch (IOException e) {
            loggingService.error("Failed to start web server: {0}", e.getMessage());
            server = null;
            return StartResult.FAILED;
        }
    }

    /**
     * Stop the web server.
     *
     * @return True if a running server was stopped
     */
    public boolean stop() {
        if (server == null) {
            return false;
        }

        server.stop(0);
        server = null;
        loggingService.info("Web server stopped");
        return true;
    }
}
