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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.prism_mc.prism.api.storage.wal.WalRecord;
import org.prism_mc.prism.loader.services.logging.LoggingService;

/**
 * Manages buffered, append-only writing of WAL records to a JSONL file.
 */
public class WalWriter {

    static final String WAL_FILE = "wal.jsonl";
    static final String CHECKPOINT_FILE = "wal.checkpoint";
    static final String CLEAN_MARKER_FILE = "wal.clean";

    private final Path walDir;
    private final LoggingService loggingService;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<WalRecord> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong nextSequence = new AtomicLong(0);
    private final AtomicLong totalWritten = new AtomicLong(0);

    /**
     * Tracks in-flight batches for out-of-order commit support.
     * Key is the batch ID, value is the entry count and committed status.
     */
    private final TreeMap<Long, BatchInfo> pendingBatches = new TreeMap<>();
    private long nextBatchId;
    private long contiguousCommitted;

    private BufferedWriter writer;

    /**
     * Construct a new WAL writer.
     *
     * @param walDir The WAL directory
     * @param loggingService The logging service
     */
    public WalWriter(Path walDir, LoggingService loggingService) {
        this.walDir = walDir;
        this.loggingService = loggingService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initialize the writer, creating the WAL directory and file if needed.
     *
     * @throws IOException If directory or file creation fails
     */
    public void initialize() throws IOException {
        Files.createDirectories(walDir);

        writer = Files.newBufferedWriter(
            walDir.resolve(WAL_FILE),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    /**
     * Assign a sequence number to the record and add it to the write buffer.
     *
     * @param record The WAL record to append
     * @return The assigned sequence number
     */
    public long append(WalRecord record) {
        long seq = nextSequence.getAndIncrement();
        record.setSequence(seq);
        buffer.add(record);
        return seq;
    }

    /**
     * Flush the buffer to disk.
     */
    public synchronized void flush() {
        if (writer == null || buffer.isEmpty()) {
            return;
        }

        List<WalRecord> toWrite = new ArrayList<>();
        WalRecord record;
        while ((record = buffer.poll()) != null) {
            toWrite.add(record);
        }

        if (toWrite.isEmpty()) {
            return;
        }

        try {
            for (WalRecord rec : toWrite) {
                writer.write(objectMapper.writeValueAsString(rec));
                writer.newLine();
            }
            writer.flush();
            totalWritten.addAndGet(toWrite.size());
        } catch (IOException e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Register a new batch that has been drained from the queue and is
     * about to be committed to the database. Returns a batch ID that
     * must be passed to {@link #commitBatch(long)} after the DB commit.
     *
     * @param entryCount The number of entries in this batch
     * @return The batch ID
     */
    public synchronized long startBatch(int entryCount) {
        long batchId = nextBatchId++;
        pendingBatches.put(batchId, new BatchInfo(entryCount, false));
        return batchId;
    }

    /**
     * Mark a batch as committed and advance the checkpoint through
     * all contiguous committed batches from the start.
     *
     * <p>Supports out-of-order commits: if batch 2 commits before
     * batch 1, the checkpoint won't advance until batch 1 also commits.</p>
     *
     * @param batchId The batch ID from {@link #startBatch(int)}
     */
    public synchronized void commitBatch(long batchId) {
        BatchInfo info = pendingBatches.get(batchId);
        if (info == null) {
            return;
        }

        info.committed = true;

        // Advance through contiguous committed batches from the front
        while (!pendingBatches.isEmpty()) {
            var firstEntry = pendingBatches.firstEntry();
            if (!firstEntry.getValue().committed) {
                break;
            }

            contiguousCommitted += firstEntry.getValue().entryCount;
            pendingBatches.pollFirstEntry();
        }

        writeCheckpointFile(contiguousCommitted);

        if (contiguousCommitted >= totalWritten.get() && pendingBatches.isEmpty()) {
            truncate();
        }
    }

    /**
     * Write the committed count to the checkpoint file.
     *
     * @param committed The committed count
     */
    private void writeCheckpointFile(long committed) {
        try {
            Files.writeString(
                walDir.resolve(CHECKPOINT_FILE),
                Long.toString(committed),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Truncate the WAL file and reset counters when all entries are committed.
     */
    private synchronized void truncate() {
        try {
            if (writer != null) {
                writer.close();
            }

            Files.deleteIfExists(walDir.resolve(WAL_FILE));
            Files.deleteIfExists(walDir.resolve(CHECKPOINT_FILE));

            totalWritten.set(0);
            contiguousCommitted = 0;
            nextBatchId = 0;
            nextSequence.set(0);

            writer = Files.newBufferedWriter(
                walDir.resolve(WAL_FILE),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Force flush and close the writer.
     */
    public synchronized void close() {
        flush();

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                loggingService.handleException(e);
            }
            writer = null;
        }
    }

    /**
     * Write the clean shutdown marker file. Called after a successful
     * shutdown drain to indicate that any remaining WAL entries are due
     * to database failures rather than a crash.
     */
    public void writeCleanMarker() {
        try {
            Files.createDirectories(walDir);
            Files.writeString(
                walDir.resolve(CLEAN_MARKER_FILE),
                "",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Clean up WAL files. Called after successful replay or when no data remains.
     */
    public void cleanup() {
        try {
            Files.deleteIfExists(walDir.resolve(WAL_FILE));
            Files.deleteIfExists(walDir.resolve(CHECKPOINT_FILE));
            Files.deleteIfExists(walDir.resolve(CLEAN_MARKER_FILE));
        } catch (IOException e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Tracks the state of a single batch for out-of-order commit support.
     */
    private static class BatchInfo {

        final int entryCount;
        boolean committed;

        BatchInfo(int entryCount, boolean committed) {
            this.entryCount = entryCount;
            this.committed = committed;
        }
    }
}
