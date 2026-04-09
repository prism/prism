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

package org.prism_mc.prism.paper.services.recording.wal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.prism_mc.prism.api.storage.wal.WalRecord;
import org.prism_mc.prism.loader.services.logging.LoggingService;

/**
 * Reads uncommitted WAL records from the WAL directory.
 */
public class WalReader {

    private static final String WAL_FILE = "wal.jsonl";
    private static final String CHECKPOINT_FILE = "wal.checkpoint";
    private static final String CLEAN_MARKER_FILE = "wal.clean";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check whether the previous shutdown was clean.
     *
     * @param walDir The WAL directory
     * @return True if the clean shutdown marker exists
     */
    public boolean wasCleanShutdown(Path walDir) {
        return Files.exists(walDir.resolve(CLEAN_MARKER_FILE));
    }

    /**
     * Check whether a WAL file exists in the directory.
     *
     * @param walDir The WAL directory
     * @return True if a WAL file exists
     */
    public boolean walFileExists(Path walDir) {
        return Files.exists(walDir.resolve(WAL_FILE));
    }

    /**
     * Read uncommitted records from the WAL directory.
     *
     * @param walDir The WAL directory
     * @param loggingService The logging service
     * @return The list of uncommitted records, or empty if none
     */
    public List<WalRecord> readUncommitted(Path walDir, LoggingService loggingService) {
        Path walFile = walDir.resolve(WAL_FILE);
        if (!Files.exists(walFile)) {
            return Collections.emptyList();
        }

        long committed = readCheckpoint(walDir, loggingService);

        List<WalRecord> allRecords = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(walFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }

                try {
                    allRecords.add(objectMapper.readValue(line, WalRecord.class));
                } catch (IOException e) {
                    loggingService.warn("Skipping corrupt WAL entry: {0}", e.getMessage());
                }
            }
        } catch (IOException e) {
            loggingService.handleException(e);
            return Collections.emptyList();
        }

        if (committed >= allRecords.size()) {
            return Collections.emptyList();
        }

        return allRecords.subList((int) committed, allRecords.size());
    }

    /**
     * Read the checkpoint value from the checkpoint file.
     *
     * @param walDir The WAL directory
     * @param loggingService The logging service
     * @return The committed count, or 0 if no checkpoint exists
     */
    private long readCheckpoint(Path walDir, LoggingService loggingService) {
        Path checkpointFile = walDir.resolve(CHECKPOINT_FILE);
        if (!Files.exists(checkpointFile)) {
            return 0;
        }

        try {
            String content = Files.readString(checkpointFile, StandardCharsets.UTF_8).trim();
            return Long.parseLong(content);
        } catch (IOException | NumberFormatException e) {
            loggingService.warn("Failed to read WAL checkpoint, assuming 0: {0}", e.getMessage());
            return 0;
        }
    }
}
