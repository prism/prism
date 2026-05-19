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
import dev.triumphteam.cmd.core.argument.keyed.Arguments;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ActivityStream;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.Previewable;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.core.injection.factories.RestoreFactory;
import org.prism_mc.prism.core.injection.factories.RollbackFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.state.BlockStateChange;
import org.prism_mc.prism.paper.services.scheduling.PrismScheduler;

@Singleton
public class PaperModificationQueueService implements ModificationQueueService {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

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
        return currentQueue == null;
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
        cancelQueueForOwner(owner);

        ModificationQueueResult result = queueResults.getIfPresent(owner);
        if (result != null) {
            // If queue has a cancellable result, cancel it
            if (result.queue() instanceof Previewable) {
                cancelPreview(owner, result);
            }

            queueResults.invalidate(owner);
        }
    }

    /**
     * Re-send live blocks for ones we faked.
     *
     * @param owner The owner
     * @param queueResult The queue result
     */
    protected void cancelPreview(Object owner, ModificationQueueResult queueResult) {
        if (!queueResult.mode().equals(ModificationQueueMode.PLANNING)) {
            return;
        }

        if (owner instanceof Player player) {
            for (
                final Iterator<ModificationResult> iterator = queueResult.results().listIterator();
                iterator.hasNext();
            ) {
                final ModificationResult result = iterator.next();

                if (result.stateChange() instanceof BlockStateChange blockStateChange) {
                    Location loc = blockStateChange.oldState().getLocation();
                    player.sendBlockChange(loc, blockStateChange.oldState().getBlockData());
                }

                iterator.remove();
            }
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
}
