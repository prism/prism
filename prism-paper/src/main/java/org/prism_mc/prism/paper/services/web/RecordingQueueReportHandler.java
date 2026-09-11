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
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public class RecordingQueueReportHandler extends ApiHandler {

    /**
     * The recording service.
     */
    private final RecordingService recordingService;

    /**
     * The recording queue capacity.
     */
    private final int queueMaxCapacity;

    /**
     * Constructor.
     *
     * @param objectMapper The object mapper
     * @param apiKey The API key
     * @param loggingService The logging service
     * @param allowedOrigins The browser origins permitted to call the API cross-origin
     * @param recordingService The recording service
     * @param queueMaxCapacity The recording queue capacity
     */
    protected RecordingQueueReportHandler(
        ObjectMapper objectMapper,
        String apiKey,
        LoggingService loggingService,
        List<String> allowedOrigins,
        RecordingService recordingService,
        int queueMaxCapacity
    ) {
        super(objectMapper, apiKey, loggingService, allowedOrigins);
        this.recordingService = recordingService;
        this.queueMaxCapacity = queueMaxCapacity;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("queueSize", recordingService.queue().size());
        response.put("queueCapacity", queueMaxCapacity);
        response.put("droppedCount", recordingService.droppedCount());

        // Action type breakdown from queue snapshot
        Map<String, Integer> actionBreakdown = new HashMap<>();
        for (Activity activity : recordingService.queue().toArray(new Activity[0])) {
            String key = activity.action().type().key();
            actionBreakdown.merge(key, 1, Integer::sum);
        }
        response.put("actionBreakdown", actionBreakdown);

        sendJson(exchange, 200, response);
    }
}
