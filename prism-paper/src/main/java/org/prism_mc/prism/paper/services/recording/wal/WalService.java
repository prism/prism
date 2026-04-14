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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.prism_mc.prism.api.actions.BlockAction;
import org.prism_mc.prism.api.actions.CustomData;
import org.prism_mc.prism.api.actions.EntityAction;
import org.prism_mc.prism.api.actions.ItemAction;
import org.prism_mc.prism.api.actions.PlayerAction;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.containers.BlockContainer;
import org.prism_mc.prism.api.containers.EntityContainer;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.api.storage.ActivityBatch;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.storage.wal.WalRecord;
import org.prism_mc.prism.api.util.TextUtils;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

/**
 * Orchestrates write-ahead log operations for the recording pipeline.
 *
 * <p>Supports two modes:</p>
 * <ul>
 *   <li><b>always</b> — Continuously writes activities to WAL as they enter
 *       the queue. Uses a buffered writer with periodic flushing and batch
 *       tracking for checkpoint management.</li>
 *   <li><b>on-demand</b> — Zero overhead during normal operation. Only writes
 *       to WAL when a database commit fails or when the queue still has
 *       entries at shutdown.</li>
 * </ul>
 */
@Singleton
public class WalService {

    private static final String MODE_ALWAYS = "always";
    private static final String MODE_ON_DEMAND = "on-demand";
    private static final String MODE_DISABLED = "disabled";

    private final Path walDir;
    private final LoggingService loggingService;
    private final ConfigurationService configurationService;
    private final short serializerVersion;

    private WalWriter walWriter;
    private ScheduledExecutorService flushScheduler;
    private ScheduledFuture<?> flushTask;
    private boolean initialized;

    /**
     * Construct the WAL service.
     *
     * @param dataPath The plugin data directory
     * @param loggingService The logging service
     * @param configurationService The configuration service
     * @param serializerVersion The serializer version
     */
    @Inject
    public WalService(
        Path dataPath,
        LoggingService loggingService,
        ConfigurationService configurationService,
        @Named("serializerVersion") short serializerVersion
    ) {
        this.walDir = dataPath.resolve("wal");
        this.loggingService = loggingService;
        this.configurationService = configurationService;
        this.serializerVersion = serializerVersion;
    }

    /**
     * Check if the WAL is enabled in configuration.
     *
     * @return True if WAL mode is not disabled
     */
    public boolean isEnabled() {
        String mode = configurationService.prismConfig().recording().walMode();
        return MODE_ALWAYS.equals(mode) || MODE_ON_DEMAND.equals(mode);
    }

    /**
     * Check if the WAL is in "always" mode.
     *
     * @return True if WAL mode is "always"
     */
    private boolean isAlwaysMode() {
        return MODE_ALWAYS.equals(configurationService.prismConfig().recording().walMode());
    }

    /**
     * Initialize the WAL service. In "always" mode, starts the writer
     * and periodic flush task. In "on-demand" mode, only prepares the
     * WAL directory.
     */
    public void initialize() {
        if (!isEnabled()) {
            return;
        }

        if (isAlwaysMode()) {
            initializeAlwaysMode();
        } else {
            initialized = true;
            loggingService.info("WAL initialized (on-demand mode)");
        }
    }

    /**
     * Initialize the always-on WAL writer and flush scheduler.
     */
    private void initializeAlwaysMode() {
        try {
            walWriter = new WalWriter(walDir, loggingService);
            walWriter.initialize();
            initialized = true;

            int flushInterval = configurationService.prismConfig().recording().walFlushIntervalMs();
            flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "prism-wal-flush");
                t.setDaemon(true);
                return t;
            });
            flushTask = flushScheduler.scheduleAtFixedRate(
                this::flushBuffer,
                flushInterval,
                flushInterval,
                TimeUnit.MILLISECONDS
            );

            loggingService.info("WAL initialized (always mode, flush interval: {0}ms)", flushInterval);
        } catch (IOException e) {
            loggingService.handleException(e);
            loggingService.error("Failed to initialize WAL, continuing without write-ahead log");
            initialized = false;
        }
    }

    /**
     * Serialize an activity to a WAL record and append it to the buffer.
     * Only active in "always" mode; no-op in "on-demand" mode.
     *
     * @param activity The activity to write
     */
    public void append(Activity activity) {
        if (!initialized || !isAlwaysMode()) {
            return;
        }

        try {
            WalRecord record = serialize(activity);
            walWriter.append(record);
        } catch (Exception e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Register a new batch that is about to be committed to the database.
     * Only active in "always" mode.
     *
     * @param entryCount The number of entries in this batch
     * @return The batch ID, or -1 if not applicable
     */
    public long startBatch(int entryCount) {
        if (!initialized || !isAlwaysMode()) {
            return -1;
        }

        return walWriter.startBatch(entryCount);
    }

    /**
     * Mark a batch as committed after a successful database write.
     * Only active in "always" mode.
     *
     * @param batchId The batch ID from {@link #startBatch(int)}
     */
    public void commitBatch(long batchId) {
        if (!initialized || !isAlwaysMode() || batchId < 0) {
            return;
        }

        walWriter.commitBatch(batchId);
    }

    /**
     * Write a batch of activities to the WAL after a database commit failure.
     * Used by "on-demand" mode to capture failed batches that have already
     * been drained from the queue.
     *
     * @param activities The activities that failed to commit
     */
    public void writeFailedBatch(List<Activity> activities) {
        if (!initialized || activities.isEmpty()) {
            return;
        }

        // In always mode, these activities are already in the WAL
        if (isAlwaysMode()) {
            return;
        }

        writeActivitiesToWal(activities);
        loggingService.warn(
            "Database commit failed, {0} activities saved to disk for replay on next start.",
            activities.size()
        );
    }

    /**
     * Write all remaining activities from the queue to the WAL. Used during
     * shutdown when the queue drain did not fully complete.
     *
     * @param queue The recording queue to drain
     */
    public void writeRemainingQueue(LinkedBlockingQueue<Activity> queue) {
        if (!initialized || queue.isEmpty()) {
            return;
        }

        // In always mode, queue items were already written to WAL at entry time
        if (isAlwaysMode()) {
            return;
        }

        List<Activity> remaining = new ArrayList<>();
        queue.drainTo(remaining);

        if (!remaining.isEmpty()) {
            writeActivitiesToWal(remaining);
        }
    }

    /**
     * Serialize and write a list of activities to the WAL file immediately.
     *
     * @param activities The activities to write
     */
    private void writeActivitiesToWal(List<Activity> activities) {
        try {
            WalWriter onDemandWriter = new WalWriter(walDir, loggingService);
            onDemandWriter.initialize();

            for (Activity activity : activities) {
                onDemandWriter.append(serialize(activity));
            }

            onDemandWriter.flush();
            onDemandWriter.close();
        } catch (IOException e) {
            loggingService.handleException(e);
            loggingService.error("Failed to write activities to WAL");
        }
    }

    /**
     * Flush the WAL buffer to disk. Only applicable in "always" mode.
     */
    public void flushBuffer() {
        if (!initialized || !isAlwaysMode()) {
            return;
        }

        walWriter.flush();
    }

    /**
     * Replay any uncommitted WAL entries into the database.
     *
     * <p>In on-demand mode, if a clean shutdown marker is missing, the WAL
     * is discarded instead of replayed. This avoids inserting activities
     * into the database that may not match the world state after a crash,
     * since Minecraft auto-saves periodically and a crash can revert
     * recent world changes.</p>
     *
     * <p>In always mode, WAL entries are replayed regardless of the clean
     * shutdown marker. Since activities are continuously written to disk,
     * the WAL provides a complete record suitable for crash recovery.</p>
     *
     * @param storageAdapter The storage adapter to replay into
     */
    public void replayUncommitted(StorageAdapter storageAdapter) {
        if (!isEnabled()) {
            return;
        }

        WalReader reader = new WalReader();

        if (!reader.walFileExists(walDir)) {
            cleanupFiles();
            return;
        }

        if (!isAlwaysMode() && !reader.wasCleanShutdown(walDir)) {
            List<WalRecord> uncommitted = reader.readUncommitted(walDir, loggingService);
            loggingService.warn(
                "Discarding {0} WAL entries due to unclean shutdown. " +
                "World state may have reverted to last auto-save, " +
                "so these activities cannot be safely replayed.",
                uncommitted.size()
            );
            cleanupFiles();
            return;
        }

        List<WalRecord> uncommitted = reader.readUncommitted(walDir, loggingService);

        if (uncommitted.isEmpty()) {
            cleanupFiles();
            return;
        }

        loggingService.info("Replaying {0} uncommitted WAL entries...", uncommitted.size());

        try {
            ActivityBatch batch = storageAdapter.createActivityBatch();
            batch.startBatch();

            for (WalRecord record : uncommitted) {
                batch.addFromWalRecord(record);
            }

            batch.commitBatch();
            loggingService.info("WAL replay complete, {0} activities recovered", uncommitted.size());
        } catch (Exception e) {
            loggingService.handleException(e);
            loggingService.error("WAL replay failed, some activities may be lost");
        }

        cleanupFiles();
    }

    /**
     * Shutdown the WAL service. In "always" mode, flushes the buffer
     * and closes the writer. In both modes, writes a clean shutdown
     * marker so the next startup can distinguish a clean shutdown
     * from a crash.
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        if (isAlwaysMode()) {
            if (flushTask != null) {
                flushTask.cancel(false);
            }

            if (flushScheduler != null) {
                flushScheduler.shutdown();
            }

            walWriter.close();
        }

        writeCleanMarker();
        initialized = false;
    }

    /**
     * Write the clean shutdown marker file.
     */
    private void writeCleanMarker() {
        WalWriter markerWriter = walWriter != null ? walWriter : new WalWriter(walDir, loggingService);
        markerWriter.writeCleanMarker();
    }

    /**
     * Clean up WAL files after replay or when no data remains.
     */
    private void cleanupFiles() {
        if (walWriter != null) {
            walWriter.cleanup();
        } else {
            WalWriter tempWriter = new WalWriter(walDir, loggingService);
            tempWriter.cleanup();
        }
    }

    /**
     * Serialize an activity into a flat WAL record.
     *
     * @param activity The activity
     * @return The WAL record
     */
    WalRecord serialize(Activity activity) {
        WalRecord.WalRecordBuilder builder = WalRecord.builder()
            .timestamp(activity.timestamp())
            .x(activity.coordinate().intX())
            .y(activity.coordinate().intY())
            .z(activity.coordinate().intZ())
            .worldUuid(activity.world().key().toString())
            .worldName(activity.world().value())
            .actionKey(activity.action().type().key())
            .descriptor(TextUtils.truncateWithEllipsis(activity.action().descriptor(), 255));

        serializeCause(builder, activity);
        serializeAction(builder, activity);

        if (activity.action().metadata() != null) {
            try {
                builder.metadata(activity.action().serializeMetadata());
            } catch (Exception e) {
                loggingService.handleException(e);
            }
        }

        if (activity.action() instanceof CustomData customData && customData.hasCustomData()) {
            builder.serializerVersion(serializerVersion);
            builder.serializedData(customData.serializeCustomData());
        }

        return builder.build();
    }

    /**
     * Serialize the cause of an activity into the builder.
     *
     * @param builder The WAL record builder
     * @param activity The activity
     */
    private void serializeCause(WalRecord.WalRecordBuilder builder, Activity activity) {
        if (activity.cause().container() instanceof PlayerContainer playerContainer) {
            builder
                .causeType("player")
                .causePlayerUuid(playerContainer.uuid().toString())
                .causePlayerName(playerContainer.name());
        } else if (activity.cause().container() instanceof BlockContainer blockContainer) {
            builder
                .causeType("block")
                .causeBlockNamespace(blockContainer.blockNamespace())
                .causeBlockName(blockContainer.blockName())
                .causeBlockData(blockContainer.serializeBlockData())
                .causeBlockTranslationKey(blockContainer.translationKey());
        } else if (activity.cause().container() instanceof EntityContainer entityContainer) {
            builder
                .causeType("entity")
                .causeEntityType(entityContainer.serializeEntityType())
                .causeEntityTranslationKey(entityContainer.translationKey());
        } else if (activity.cause().container() instanceof StringContainer stringContainer) {
            builder.causeType("string").causeString(stringContainer.value());
        }
    }

    /**
     * Serialize action-specific data into the builder.
     *
     * @param builder The WAL record builder
     * @param activity The activity
     */
    private void serializeAction(WalRecord.WalRecordBuilder builder, Activity activity) {
        if (activity.action() instanceof EntityAction entityAction) {
            builder
                .entityType(entityAction.entityContainer().serializeEntityType())
                .entityTranslationKey(entityAction.entityContainer().translationKey());
        }

        if (activity.action() instanceof ItemAction itemAction) {
            builder
                .itemMaterial(itemAction.serializeMaterial())
                .itemData(itemAction.serializeItemData())
                .itemQuantity(itemAction.quantity());
        }

        if (activity.action() instanceof BlockAction blockAction) {
            builder
                .blockNamespace(blockAction.blockContainer().blockNamespace())
                .blockName(blockAction.blockContainer().blockName())
                .blockData(blockAction.blockContainer().serializeBlockData())
                .blockTranslationKey(blockAction.blockContainer().translationKey());

            if (blockAction.replacedBlockContainer() != null) {
                builder
                    .replacedBlockNamespace(blockAction.replacedBlockContainer().blockNamespace())
                    .replacedBlockName(blockAction.replacedBlockContainer().blockName())
                    .replacedBlockData(blockAction.replacedBlockContainer().serializeBlockData())
                    .replacedBlockTranslationKey(blockAction.replacedBlockContainer().translationKey());
            }
        }

        if (activity.action() instanceof PlayerAction playerAction) {
            builder
                .affectedPlayerUuid(playerAction.playerContainer().uuid().toString())
                .affectedPlayerName(playerAction.playerContainer().name());
        }
    }
}
