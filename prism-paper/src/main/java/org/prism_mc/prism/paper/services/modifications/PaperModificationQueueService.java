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
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

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
     * @return The builder
     */
    public ModificationRuleset.ModificationRulesetBuilder applyFlagsToModificationRuleset(Arguments arguments) {
        var builder = configurationService.prismConfig().modifications().toRulesetBuilder();
        builder.overwrite(arguments.hasFlag("overwrite"));

        arguments.getFlagValue("drainlava", Boolean.class).ifPresent(builder::drainLava);
        arguments.getFlagValue("physics", Boolean.class).ifPresent(builder::applyPhysics);
        arguments.getFlagValue("removedrops", Boolean.class).ifPresent(builder::removeDrops);

        return builder;
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
            revealLiveBlocks(player, previewQuery);
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
     */
    protected void revealLiveBlocks(Player player, ActivityQuery query) {
        prismScheduler.runAsync(() -> {
            ActivityStream stream;
            try {
                stream = storageAdapter.streamActivities(query);
            } catch (Exception e) {
                loggingService.handleException(e);
                return;
            }

            revealNextBatch(player, stream);
        });
    }

    /**
     * Pull the next batch of previewed activities and reveal their live block
     * state to the player. Recurses until the stream is drained, then closes
     * it. Each batch hops to the player's region thread because
     * {@code block.getBlockData()} and {@code sendBlockChange} must run there
     * on Folia.
     *
     * @param player The previewing player
     * @param stream The stream opened from the result's query
     */
    private void revealNextBatch(Player player, ActivityStream stream) {
        int batchSize = Math.max(1, configurationService.prismConfig().modifications().cancelPreviewBatchSize());

        prismScheduler.runAsync(() -> {
            List<Activity> batch;
            try {
                batch = stream.next(batchSize);
            } catch (Exception e) {
                loggingService.handleException(e);
                stream.close();
                return;
            }

            if (batch.isEmpty()) {
                stream.close();
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

                revealNextBatch(player, stream);
            });
        });
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

        replayUndoBatch(sender, entries, 0, applied, skipped, undoOfRollback);
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
        boolean undoOfRollback
    ) {
        if (cursor >= entries.size()) {
            finishUndo(sender, applied[0], skipped[0], entries, undoOfRollback);
            return;
        }

        int end = Math.min(cursor + UNDO_REPLAY_BATCH_SIZE, entries.size());
        List<UndoEntry> batch = entries.subList(cursor, end);

        Runnable applyBatch = () -> {
            for (UndoEntry entry : batch) {
                if (!(entry instanceof BlockUndoEntry block)) {
                    continue;
                }

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

                live.setBlockData(block.oldBlockData());
                if (block.oldTileNbt() != null && live.getType() != org.bukkit.Material.AIR) {
                    NBT.modify(live.getState(), nbt -> {
                        nbt.mergeCompound(block.oldTileNbt());
                    });
                }
                applied[0]++;
            }

            // Schedule the next batch via async hop, then back to the region
            // thread for the writes. Keeps any single tick bounded.
            prismScheduler.runAsync(() ->
                prismScheduler.runGlobal(() -> replayUndoBatch(sender, entries, end, applied, skipped, undoOfRollback))
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
        List<UndoEntry> entries,
        boolean undoOfRollback
    ) {
        List<Long> primarykeys = entries
            .stream()
            .filter(BlockUndoEntry.class::isInstance)
            .map(e -> ((BlockUndoEntry) e).activityPk())
            .toList();

        // undoOfRollback=true means the original op marked reversed=true; we flip back to false.
        // undoOfRollback=false (restore was original) means we flip back to true.
        boolean newReversedState = !undoOfRollback;

        prismScheduler.runAsync(() -> {
            try {
                storageAdapter.markReversed(primarykeys, newReversedState);
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
