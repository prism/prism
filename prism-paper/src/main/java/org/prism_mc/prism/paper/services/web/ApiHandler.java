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
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public abstract class ApiHandler implements HttpHandler {

    /**
     * The object mapper.
     */
    protected final ObjectMapper objectMapper;

    /**
     * The API key.
     */
    private final String apiKey;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * Browser origins permitted to call the API cross-origin. Empty means CORS is disabled and the
     * API is reachable same-origin only.
     */
    private final List<String> allowedOrigins;

    /**
     * Constructor.
     *
     * @param objectMapper The object mapper
     * @param apiKey The API key
     * @param loggingService The logging service
     * @param allowedOrigins The browser origins permitted to call the API cross-origin
     */
    protected ApiHandler(
        ObjectMapper objectMapper,
        String apiKey,
        LoggingService loggingService,
        List<String> allowedOrigins
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.loggingService = loggingService;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        applyCorsHeaders(exchange);

        // Answer CORS preflight requests before authentication: they carry no Authorization header
        // by design, and only need the CORS headers set above plus an empty success response.
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");

        if (!authenticate(exchange)) {
            sendError(exchange, 401, "Unauthorized");
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            handleRequest(exchange);
        } catch (Exception e) {
            loggingService.handleThrowable("Error handling web request: " + exchange.getRequestURI().getPath(), e);
            sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Apply CORS response headers when the request's Origin is permitted by the configuration. Does
     * nothing when no origins are configured (the default), leaving the API same-origin only.
     *
     * @param exchange The HTTP exchange
     */
    private void applyCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || allowedOrigins.isEmpty()) {
            return;
        }

        boolean wildcard = allowedOrigins.contains("*");
        if (!wildcard && !allowedOrigins.contains(origin)) {
            return;
        }

        var responseHeaders = exchange.getResponseHeaders();
        if (wildcard) {
            responseHeaders.set("Access-Control-Allow-Origin", "*");
        } else {
            // Echo the specific origin and vary on it so shared caches don't serve one origin's
            // response to another.
            responseHeaders.set("Access-Control-Allow-Origin", origin);
            responseHeaders.set("Vary", "Origin");
        }
        responseHeaders.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        responseHeaders.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        responseHeaders.set("Access-Control-Max-Age", "3600");
    }

    /**
     * Handle an authenticated GET request.
     *
     * @param exchange The HTTP exchange
     * @throws Exception If the request cannot be handled
     */
    protected abstract void handleRequest(HttpExchange exchange) throws Exception;

    /**
     * Authenticate a request using its bearer token.
     *
     * @param exchange The HTTP exchange
     * @return True if the token matches the configured API key
     */
    private boolean authenticate(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }

        // Constant-time comparison to avoid leaking the key via response timing.
        return MessageDigest.isEqual(
            apiKey.getBytes(StandardCharsets.UTF_8),
            authHeader.substring(7).getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Write an object to the response body as JSON.
     *
     * @param exchange The HTTP exchange
     * @param statusCode The HTTP status code
     * @param body The object to serialize
     * @throws IOException If the response cannot be written
     */
    protected void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] response = objectMapper.writeValueAsBytes(body);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Write an error message to the response body as JSON.
     *
     * @param exchange The HTTP exchange
     * @param statusCode The HTTP status code
     * @param message The error message
     * @throws IOException If the response cannot be written
     */
    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }

    /**
     * Parse the request's query string into decoded key/value pairs.
     *
     * @param exchange The HTTP exchange
     * @return The decoded query parameters
     */
    protected Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(
                    java.net.URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                );
            }
        }

        return params;
    }
}
