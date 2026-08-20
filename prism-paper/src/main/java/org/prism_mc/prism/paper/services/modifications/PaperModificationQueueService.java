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

package org.prism_mc.prism.paper.services.modifications;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.tr7zw.nbtapi.NBT;
import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ActivityStream;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.Previewable;
import org.prism_mc.prism.api.services.modifications.UndoEntry;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.core.injection.factories.RestoreFactory;
import org.prism_mc.prism.core.injection.factories.RollbackFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.DefaultsConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;
import org.prism_mc.prism.paper.utils.BlockUtils;
import org.prism_mc.prism.paper.utils.TagLib;

@Singleton
public class PaperModificationQueueService implements ModificationQueueService {

    /**
     * Batch size for the undo snapshot replay. Sized to amortize region-hop
     * cost without holding a giant slice live on any single tick.
     */
    private static final int UNDO_REPLAY_BATCH_SIZE = 5000;

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The message service.
     */
    private final MessageService messageService;

    /**
     * Cache the current queue, if any. Volatile because it is read from caller threads
     * (via {@link #queueAvailable()}, {@link #currentQueue()}, {@link #currentQueueForOwner(Object)})
     * and written from the global region thread when queues are created/cleared.
     */
    private volatile ModificationQueue currentQueue = null;

    /**
     * Set while an undo replay is in flight. Undo bypasses the normal queue
     * machinery (no Activity flow, no executor) but still needs to lock out
     * other modifications.
     */
    private final AtomicBoolean undoInProgress = new AtomicBoolean(false);

    /**
     * The restore factory.
     */
    private final RestoreFactory restoreFactory;

    /**
     * The rollback factory.
     */
    private final RollbackFactory rollbackFactory;

    /**
     * The scheduler — used to hop between async storage queries and the global region thread.
     */
    private final PrismScheduler prismScheduler;

    /**
     * The storage adapter — used to fetch activities for {@link #apply}.
     */
    private final StorageAdapter storageAdapter;

    /**
     * A cache of recently used queues.
     */
    private final Cache<Object, ModificationQueueResult> queueResults;

    /**
     * One-shot completion callbacks, keyed by queue owner. Removed after firing.
     */
    private final Map<Object, Consumer<ModificationQueueResult>> completionCallbacks = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param cacheService The cache service
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @param messageService The message service
     * @param restoreFactory The restore factory
     * @param rollbackFactory The rollback factory
     * @param prismScheduler The scheduler
     * @param storageAdapter The storage adapter
     */
    @Inject
    public PaperModificationQueueService(
        CacheService cacheService,
        ConfigurationService configurationService,
        LoggingService loggingService,
        MessageService messageService,
        RestoreFactory restoreFactory,
        RollbackFactory rollbackFactory,
        PrismScheduler prismScheduler,
        StorageAdapter storageAdapter
    ) {
        this.configurationService = configurationService;
        this.loggingService = loggingService;
        this.messageService = messageService;
        this.restoreFactory = restoreFactory;
        this.rollbackFactory = rollbackFactory;
        this.prismScheduler = prismScheduler;
        this.storageAdapter = storageAdapter;

        var cacheBuilder = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(4)
            .evictionListener((key, value, cause) -> {
                String msg = "Evicting queue result cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            })
            .removalListener((key, value, cause) -> {
                String msg = "Removing queue result cache: Key: {0}, Value: {1}, Removal Cause: {2}";
                loggingService.debug(msg, key, value, cause);
            });

        if (configurationService.prismConfig().cache().recordStats()) {
            cacheBuilder.recordStats();
        }

        queueResults = cacheBuilder.build();
        cacheService.caches().put("queueResults", queueResults);
    }

    /**
     * Apply flags to the modification ruleset.
     *
     * @param arguments The arguments
     * @param command The command whose default flags apply, or null to ignore configured defaults
     * @return The builder
     */
    public ModificationRuleset.ModificationRulesetBuilder applyFlagsToModificationRuleset(
        Arguments arguments,
        DefaultsConfiguration.CommandType command
    ) {
        var builder = configurationService.prismConfig().modifications().toRulesetBuilder();

        Map<String, String> defaultFlags = command == null || arguments.hasFlag("nodefaults")
            ? Map.of()
            : configurationService.prismConfig().defaults().flags(command);

        builder.overwrite(arguments.hasFlag("overwrite") || Boolean.parseBoolean(defaultFlags.get("overwrite")));

        resolveBooleanFlag(arguments, defaultFlags, "drainlava").ifPresent(builder::drainLava);
        resolveBooleanFlag(arguments, defaultFlags, "physics").ifPresent(builder::applyPhysics);
        resolveBooleanFlag(arguments, defaultFlags, "removedrops").ifPresent(builder::removeDrops);

        return builder;
    }

    /**
     * Resolve a boolean modification flag, preferring the player-supplied value and falling back
     * to the configured default when present.
     *
     * @param arguments The arguments
     * @param defaultFlags The resolved default flags
     * @param flag The flag name
     * @return The effective value, or empty when neither supplied nor configured
     */
    private Optional<Boolean> resolveBooleanFlag(Arguments arguments, Map<String, String> defaultFlags, String flag) {
        Optional<Boolean> supplied = arguments.getFlagValue(flag, Boolean.class);
        if (supplied.isPresent()) {
            return supplied;
        }

        String defaultValue = defaultFlags.get(flag);

        return defaultValue != null ? Optional.of(Boolean.parseBoolean(defaultValue)) : Optional.empty();
    }

    @Override
    public ModificationRuleset defaultModificationRuleset() {
        return configurationService.prismConfig().modifications().toRulesetBuilder().build();
    }

    @Override
    public boolean queueAvailable() {
        return currentQueue == null && !undoInProgress.get();
    }

    @Override
    public boolean cancelQueueForOwner(Object owner) {
        if (currentQueue != null && currentQueue.owner().equals(owner)) {
            this.currentQueue.destroy();
            this.currentQueue = null;

            return true;
        }

        return false;
    }

    @Override
    public void clearEverythingForOwner(Object owner) {
        clearEverythingForOwner(owner, null);
    }

    /**
     * Cancel the current queue and invalidate the cached result for {@code owner}.
     *
     * @param owner The owner
     * @param onComplete Optional callback fired once cleanup (including any reveal) finishes
     */
    public void clearEverythingForOwner(Object owner, Runnable onComplete) {
        // Capture the in-flight preview's query before destroy() nulls currentQueue.
        // Without this, cancelling a preview before it finishes leaves phantom block
        // changes on the player's client — finalizeResult never runs, so queueResults
        // has no entry to drive the reveal below.
        ActivityQuery previewQuery = null;
        ModificationQueue active = currentQueue;
        if (
            active != null &&
            active.owner().equals(owner) &&
            active instanceof Previewable &&
            active.mode().equals(ModificationQueueMode.PLANNING)
        ) {
            previewQuery = active.query();
        }

        cancelQueueForOwner(owner);

        ModificationQueueResult result = queueResults.getIfPresent(owner);
        if (result != null) {
            if (
                previewQuery == null &&
                result.queue() instanceof Previewable &&
                result.mode().equals(ModificationQueueMode.PLANNING)
            ) {
                previewQuery = result.queue().query();
            }

            queueResults.invalidate(owner);
        }

        if (previewQuery != null && owner instanceof Player player) {
            revealLiveBlocks(player, previewQuery, onComplete);
        } else if (onComplete != null) {
            onComplete.run();
        }
    }

    /**
     * Cleanup for an owner who has disconnected.
     *
     * @param owner The owner
     */
    public void disconnectedOwner(Object owner) {
        cancelQueueForOwner(owner);
        queueResults.invalidate(owner);
    }

    /**
     * Reveal live block state at every location the preview touched.
     *
     * @param player The previewing player
     * @param query The query the preview was built from
     * @param onComplete Optional callback fired on the player's region thread when the reveal finishes
     */
    protected void revealLiveBlocks(Player player, ActivityQuery query, Runnable onComplete) {
        prismScheduler.runAsync(() -> {
            ActivityStream stream;
            try {
                stream = storageAdapter.streamActivities(query);
            } catch (Exception e) {
                loggingService.handleException(e);
                if (onComplete != null) {
                    prismScheduler.runForEntity(player, onComplete);
                }
                return;
            }

            int total = stream.total();
            int[] processed = { 0 };
            int[] lastReportedPercent = { 0 };
            revealNextBatch(player, stream, total, processed, lastReportedPercent, onComplete);
        });
    }

    /**
     * Pull the next batch of previewed activities and reveal their live block
     * state to the player. Recurses until the stream is drained, then closes
     * it and fires {@code onComplete}. Each batch hops to the player's region
     * thread because {@code block.getBlockData()} and {@code sendBlockChange}
     * must run there on Folia.
     *
     * @param player The previewing player
     * @param stream The stream opened from the result's query
     * @param total Total activity count, captured once for progress denominator
     * @param processed Single-cell mutable counter, summed across batches
     * @param lastReportedPercent Single-cell mutable percent-floor used to throttle progress messages
     * @param onComplete Optional callback fired when the stream is drained or errors
     */
    private void revealNextBatch(
        Player player,
        ActivityStream stream,
        int total,
        int[] processed,
        int[] lastReportedPercent,
        Runnable onComplete
    ) {
        int batchSize = Math.max(1, configurationService.prismConfig().modifications().cancelPreviewBatchSize());

        prismScheduler.runAsync(() -> {
            List<Activity> batch;
            try {
                batch = stream.next(batchSize);
            } catch (Exception e) {
                loggingService.handleException(e);
                stream.close();

                if (onComplete != null) {
                    prismScheduler.runForEntity(player, onComplete);
                }

                return;
            }

            if (batch.isEmpty()) {
                stream.close();

                if (onComplete != null) {
                    prismScheduler.runForEntity(player, onComplete);
                }

                return;
            }

            prismScheduler.runForEntity(player, () -> {
                for (Activity activity : batch) {
                    if (activity.coordinate() == null || activity.worldUuid() == null) {
                        continue;
                    }

                    World world = Bukkit.getWorld(activity.worldUuid());
                    if (world == null) {
                        continue;
                    }

                    Block block = world.getBlockAt(
                        activity.coordinate().intX(),
                        activity.coordinate().intY(),
                        activity.coordinate().intZ()
                    );

                    player.sendBlockChange(block.getLocation(), block.getBlockData());
                }

                processed[0] += batch.size();
                maybeReportProgress(player, processed[0], total, lastReportedPercent);

                revealNextBatch(player, stream, total, processed, lastReportedPercent, onComplete);
            });
        });
    }

    /**
     * Send a {@code modifications-progress} message to the receiver when a new
     * progress-report milestone is reached. Mirrors the throttle used by the
     * normal modification pipeline so cancel and undo runs behave consistently
     * with rollback/restore.
     *
     * @param receiver The owner to message
     * @param processed Items processed so far
     * @param total Total items being processed
     * @param lastReportedPercent Single-cell mutable percent-floor; updated when a milestone fires
     */
    private void maybeReportProgress(CommandSender receiver, int processed, int total, int[] lastReportedPercent) {
        var modConfig = configurationService.prismConfig().modifications();
        if (total < modConfig.progressReportThreshold()) {
            return;
        }

        int percent = (int) (((long) processed * 100L) / total);
        if (percent >= lastReportedPercent[0] + modConfig.progressReportStepPercent() && percent < 100) {
            lastReportedPercent[0] = percent;
            messageService.modificationsProgress(receiver, percent, processed, total);
        }
    }

    @Nullable
    @Override
    public ModificationQueue currentQueue() {
        return currentQueue;
    }

    @Override
    public Optional<ModificationQueue> currentQueueForOwner(Object owner) {
        if (currentQueue != null && currentQueue.owner().equals(owner)) {
            return Optional.of(currentQueue);
        }

        return Optional.empty();
    }

    @Override
    public ModificationQueue newQueue(
        Class<? extends ModificationQueue> clazz,
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream
    ) {
        if (clazz.equals(PaperRollback.class)) {
            return newRollbackQueue(modificationRuleset, owner, query, activityStream);
        } else if (clazz.equals(PaperRestore.class)) {
            return newRestoreQueue(modificationRuleset, owner, query, activityStream);
        }

        throw new IllegalArgumentException("Invalid modification queue.");
    }

    @Override
    public ModificationQueue newRollbackQueue(
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream
    ) {
        if (!queueAvailable()) {
            throw new IllegalStateException("No queue available until current queue finished.");
        }

        // Cancel any existing queues/results
        clearEverythingForOwner(owner);

        this.currentQueue = rollbackFactory.create(modificationRuleset, owner, query, activityStream, this::onEnd);

        return this.currentQueue;
    }

    @Override
    public ModificationQueue newRestoreQueue(
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream
    ) {
        if (!queueAvailable()) {
            throw new IllegalStateException("No queue available until current queue finished.");
        }

        // Cancel any existing queues/results
        clearEverythingForOwner(owner);

        this.currentQueue = restoreFactory.create(modificationRuleset, owner, query, activityStream, this::onEnd);

        return this.currentQueue;
    }

    /**
     * Run a rollback, restore, or preview for activities matching the given query.
     *
     * <p>Queries activities asynchronously, then creates and applies the queue on the
     * global region thread. The returned future completes when the queue ends — on the
     * thread that ran the queue's executor, which is not the main thread.</p>
     *
     * <p>If no activities match the query the future completes immediately with an empty
     * result whose {@code queue()} is null.</p>
     *
     * @param type The modification type
     * @param owner The owner — typically a Player or CommandSender
     * @param query The activity query
     * @param ruleset The modification ruleset
     * @return A future completing with the queue result
     */
    public CompletableFuture<ModificationQueueResult> apply(
        ModificationType type,
        Object owner,
        ActivityQuery query,
        ModificationRuleset ruleset
    ) {
        if (!queueAvailable()) {
            return CompletableFuture.failedFuture(new IllegalStateException("A modification queue is already running"));
        }

        CompletableFuture<ModificationQueueResult> future = new CompletableFuture<>();

        prismScheduler.runAsync(() -> {
            ActivityStream activityStream;
            try {
                activityStream = storageAdapter.streamActivities(query);
            } catch (Exception e) {
                future.completeExceptionally(e);
                return;
            }

            if (activityStream.total() == 0) {
                activityStream.close();
                future.complete(
                    ModificationQueueResult.builder()
                        .mode(
                            type == ModificationType.PREVIEW
                                ? ModificationQueueMode.PLANNING
                                : ModificationQueueMode.COMPLETING
                        )
                        .results(List.of())
                        .build()
                );
                return;
            }

            prismScheduler.runGlobal(() -> runQueue(type, owner, query, ruleset, activityStream, future));
        });

        return future;
    }

    /**
     * Create the queue and kick off the work. Runs on the global region thread.
     *
     * <p>Registers the completion callback only after the queue is successfully created,
     * so a queue-busy throw never clobbers a sibling caller's callback. If the apply or
     * preview throws after the callback is registered, the registered callback is
     * removed and the orphan queue is cancelled.</p>
     */
    private void runQueue(
        ModificationType type,
        Object owner,
        ActivityQuery query,
        ModificationRuleset ruleset,
        ActivityStream activityStream,
        CompletableFuture<ModificationQueueResult> future
    ) {
        ModificationQueue queue;
        try {
            queue = type == ModificationType.RESTORE
                ? newRestoreQueue(ruleset, owner, query, activityStream)
                : newRollbackQueue(ruleset, owner, query, activityStream);
        } catch (Exception e) {
            activityStream.close();
            future.completeExceptionally(e);
            return;
        }

        completionCallbacks.put(owner, future::complete);

        try {
            if (type == ModificationType.PREVIEW) {
                if (queue instanceof Previewable previewable) {
                    previewable.preview();
                } else {
                    throw new IllegalStateException("Queue type does not support preview");
                }
            } else {
                queue.apply();
            }
        } catch (Exception e) {
            completionCallbacks.remove(owner);
            cancelQueueForOwner(owner);
            future.completeExceptionally(e);
        }
    }

    /**
     * Register a one-shot callback to fire when the queue owned by {@code owner} ends.
     *
     * <p>For {@link ModificationQueueMode#COMPLETING} queues this fires after the queue
     * is destroyed, so {@link #queueAvailable()} returns true inside the callback. For
     * {@link ModificationQueueMode#PLANNING} (preview) queues the queue stays active.</p>
     *
     * @param owner The owner the convenience caller used when creating the queue
     * @param callback The callback to invoke with the result
     */
    public void onCompletion(Object owner, Consumer<ModificationQueueResult> callback) {
        completionCallbacks.put(owner, callback);
    }

    /**
     * Remove a previously registered completion callback without firing it.
     *
     * @param owner The owner
     */
    public void removeCompletionCallback(Object owner) {
        completionCallbacks.remove(owner);
    }

    /**
     * On queue end, handle some cleanup.
     *
     * @param result Modification queue result
     */
    protected void onEnd(ModificationQueueResult result) {
        // Prefer the queue carried on the result — it's the queue that just ended,
        // even if currentQueue has since been cleared by a concurrent cancel.
        ModificationQueue endedQueue = result.queue() != null ? result.queue() : currentQueue;
        Object owner = endedQueue.owner();
        queueResults.put(owner, result);

        if (result.mode().equals(ModificationQueueMode.COMPLETING)) {
            // Message the user with results
            if (owner instanceof CommandSender sender) {
                messageService.modificationsAppliedSuccess(sender);
                messageService.modificationsApplied(sender, result.applied());
                messageService.modificationsPartial(sender, result);
                messageService.modificationsSkipped(sender, result);

                if (result.drainedLava() > 0) {
                    messageService.modificationsDrainedLava(sender, result.drainedLava());
                }

                if (result.movedEntities() > 0) {
                    messageService.modificationsMovedEntities(sender, result.movedEntities());
                }

                if (result.removedBlocks() > 0) {
                    messageService.modificationsRemovedBlocks(sender, result.removedBlocks());
                }

                if (result.removedDrops() > 0) {
                    messageService.modificationsRemovedDrops(sender, result.removedDrops());
                }
            }

            // Clear and destroy the queue if completing
            cancelQueueForOwner(owner);
        } else if (result.mode().equals(ModificationQueueMode.PLANNING)) {
            // Message the user with results
            if (owner instanceof CommandSender sender) {
                messageService.modificationsAppliedSuccess(sender, result.planned());
            }
        }

        // Fire the completion callback last so observers see a freed queue.
        Consumer<ModificationQueueResult> callback = completionCallbacks.remove(owner);
        if (callback != null) {
            callback.accept(result);
        }
    }

    @Override
    public Optional<ModificationQueueResult> queueResultForOwner(Object owner) {
        return Optional.ofNullable(queueResults.getIfPresent(owner));
    }

    /**
     * Replay the captured pre-modification world state for a previously
     * completed rollback/restore.
     *
     * @param sender The command sender requesting the undo
     * @param queueResult The cached result whose entries we're replaying
     * @param undoOfRollback True if undoing a rollback, false if undoing a restore
     */
    public void applyUndo(CommandSender sender, ModificationQueueResult queueResult, boolean undoOfRollback) {
        if (!undoInProgress.compareAndSet(false, true)) {
            messageService.errorQueueNotFree(sender);
            return;
        }

        // Snapshot the entries — the cached result list could be mutated by
        // cancelPreview or eviction while we iterate async.
        List<UndoEntry> entries = List.copyOf(queueResult.undoEntries());
        if (entries.isEmpty()) {
            undoInProgress.set(false);
            messageService.modificationsUndoNoResult(sender);
            return;
        }

        int[] applied = { 0 };
        int[] skipped = { 0 };
        int[] lastReportedPercent = { 0 };
        List<Long> appliedKeys = new ArrayList<>();

        replayUndoBatch(sender, entries, 0, applied, skipped, appliedKeys, lastReportedPercent, undoOfRollback);
    }

    /**
     * Process one batch of undo entries on a region-safe thread, then recurse
     * (async-hopping in between) until the snapshot list is drained.
     */
    private void replayUndoBatch(
        CommandSender sender,
        List<UndoEntry> entries,
        int cursor,
        int[] applied,
        int[] skipped,
        List<Long> appliedKeys,
        int[] lastReportedPercent,
        boolean undoOfRollback
    ) {
        if (cursor >= entries.size()) {
            finishUndo(sender, applied[0], skipped[0], appliedKeys, undoOfRollback);
            return;
        }

        int end = Math.min(cursor + UNDO_REPLAY_BATCH_SIZE, entries.size());
        List<UndoEntry> batch = entries.subList(cursor, end);

        Runnable applyBatch = () -> {
            for (UndoEntry entry : batch) {
                if (!(entry instanceof BlockUndoEntry block)) {
                    continue;
                }

                try {
                    World world = Bukkit.getWorld(block.worldUuid());
                    if (world == null) {
                        skipped[0]++;
                        continue;
                    }

                    Block live = world.getBlockAt(
                        block.coordinate().intX(),
                        block.coordinate().intY(),
                        block.coordinate().intZ()
                    );

                    // Live-state guard: only revert if the block is still what the
                    // original modification wrote. If something else is there now,
                    // someone (or something) has built over it since — leave it.
                    if (!live.getBlockData().matches(block.newBlockData())) {
                        skipped[0]++;
                        continue;
                    }

                    BlockData writtenData = block.oldBlockData();
                    BlockData replacedData = block.newBlockData();

                    // The snapshot is taken from the live world, so it can catch a block mid-mechanic
                    // that no plain write can reproduce. Replaying one would strand a ghost block, so
                    // leave the cell as the modification left it.
                    if (TagLib.TRANSIENT_BLOCKS.isTagged(writtenData.getMaterial())) {
                        skipped[0]++;
                        continue;
                    }

                    boolean physics = writtenData.getMaterial() != org.bukkit.Material.AIR;

                    BlockUtils.reconcileBedPartner(live, writtenData, replacedData, physics);
                    BlockUtils.reconcileBisectedPartner(live, writtenData, replacedData, physics);

                    live.setBlockData(writtenData, physics);

                    // Same guard as the forward write in PaperBlockAction#setBlock: a snapshot
                    // can carry tile NBT for a block whose block entity setBlockData cannot
                    // recreate (minecraft:moving_piston), and NBT-API throws on such a state.
                    if (block.oldTileNbt() != null && live.getState() instanceof TileState liveState) {
                        NBT.modify(liveState, nbt -> {
                            nbt.mergeCompound(block.oldTileNbt());
                        });
                    }

                    // Double-chest halves are separate snapshots. Re-placing one must reconnect its
                    // partner: a half removed after its partner during the original op is captured as
                    // SINGLE, so verbatim replay yields LEFT/RIGHT beside SINGLE — reconcile (which is
                    // order-independent) repairs it once both halves land. Deliberately no partner
                    // *downgrade* on removal: when both halves are being undone, eagerly downgrading the
                    // survivor to SINGLE makes its own snapshot's live-guard miss and leaves it behind.
                    if (writtenData instanceof Chest chest) {
                        BlockUtils.reconcileChestConnection(live, chest, physics);
                    }

                    applied[0]++;
                    appliedKeys.add(block.activityPk());
                } catch (Exception e) {
                    skipped[0]++;
                    loggingService.handleThrowable(String.format("An undo error occurred. %s", block), e);
                }
            }

            maybeReportProgress(sender, end, entries.size(), lastReportedPercent);

            // Schedule the next batch via async hop, then back to the region
            // thread for the writes. Keeps any single tick bounded.
            prismScheduler.runAsync(() ->
                prismScheduler.runGlobal(() ->
                    replayUndoBatch(
                        sender,
                        entries,
                        end,
                        applied,
                        skipped,
                        appliedKeys,
                        lastReportedPercent,
                        undoOfRollback
                    )
                )
            );
        };

        if (sender instanceof Player player) {
            prismScheduler.runForEntity(player, applyBatch);
        } else {
            prismScheduler.runGlobal(applyBatch);
        }
    }

    /**
     * Final step of undo: flip the activity reversed flag in storage, message
     * the user with applied/skipped counts, and clear the busy flag.
     */
    private void finishUndo(
        CommandSender sender,
        int applied,
        int skipped,
        List<Long> appliedKeys,
        boolean undoOfRollback
    ) {
        // undoOfRollback=true means the original op marked reversed=true; we flip back to false.
        // undoOfRollback=false (restore was original) means we flip back to true.
        boolean newReversedState = !undoOfRollback;

        prismScheduler.runAsync(() -> {
            try {
                storageAdapter.markReversed(appliedKeys, newReversedState);
            } catch (Exception e) {
                loggingService.handleException(e);
            } finally {
                undoInProgress.set(false);
            }

            Runnable notify = () -> {
                messageService.modificationsAppliedSuccess(sender);
                messageService.modificationsApplied(sender, applied);
                if (skipped > 0) {
                    // Synthetic result so the message template's {result.skipped}
                    // placeholder renders; undo doesn't run through the queue
                    // so there's no real ModificationQueueResult to reuse.
                    ModificationQueueResult conflictView = ModificationQueueResult.builder()
                        .mode(ModificationQueueMode.COMPLETING)
                        .results(List.of())
                        .skipped(skipped)
                        .build();
                    messageService.modificationsSkipped(sender, conflictView);
                }
            };
            if (sender instanceof Player player) {
                prismScheduler.runForEntity(player, notify);
            } else {
                prismScheduler.runGlobal(notify);
            }
        });
    }
}
