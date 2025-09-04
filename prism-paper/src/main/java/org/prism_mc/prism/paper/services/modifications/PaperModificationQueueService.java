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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.services.modifications.ModificationQueue;
import org.prism_mc.prism.api.services.modifications.ModificationQueueMode;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.ModificationRuleset;
import org.prism_mc.prism.api.services.modifications.Previewable;
import org.prism_mc.prism.core.injection.factories.RestoreFactory;
import org.prism_mc.prism.core.injection.factories.RollbackFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.modifications.state.BlockStateChange;

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
     * Cache the current queue, if any.
     */
    private ModificationQueue currentQueue = null;

    /**
     * The restore factory.
     */
    private final RestoreFactory restoreFactory;

    /**
     * The rollback factory.
     */
    private final RollbackFactory rollbackFactory;

    /**
     * A cache of recently used queues.
     */
    private final Cache<Object, ModificationQueueResult> queueResults;

    /**
     * Constructor.
     *
     * @param cacheService The cache service
     * @param configurationService The configuration service
     * @param loggingService The logging service
     * @param messageService The message service
     * @param restoreFactory The restore factory
     * @param rollbackFactory The rollback factory.
     */
    @Inject
    public PaperModificationQueueService(
        CacheService cacheService,
        ConfigurationService configurationService,
        LoggingService loggingService,
        MessageService messageService,
        RestoreFactory restoreFactory,
        RollbackFactory rollbackFactory
    ) {
        this.configurationService = configurationService;
        this.messageService = messageService;
        this.restoreFactory = restoreFactory;
        this.rollbackFactory = rollbackFactory;

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
        arguments.getFlagValue("removedrops", Boolean.class).ifPresent(builder::removeDrops);

        return builder;
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
                    BlockData liveBlockData = loc.getWorld().getBlockData(loc);
                    player.sendBlockChange(loc, liveBlockData);
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
        List<Activity> modifications
    ) {
        if (clazz.equals(PaperRollback.class)) {
            return newRollbackQueue(modificationRuleset, owner, query, modifications);
        } else if (clazz.equals(PaperRestore.class)) {
            return newRestoreQueue(modificationRuleset, owner, query, modifications);
        }

        throw new IllegalArgumentException("Invalid modification queue.");
    }

    @Override
    public ModificationQueue newRollbackQueue(
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        List<Activity> modifications
    ) {
        if (!queueAvailable()) {
            throw new IllegalStateException("No queue available until current queue finished.");
        }

        // Cancel any existing queues/results
        clearEverythingForOwner(owner);

        this.currentQueue = rollbackFactory.create(modificationRuleset, owner, query, modifications, this::onEnd);

        return this.currentQueue;
    }

    @Override
    public ModificationQueue newRestoreQueue(
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        List<Activity> modifications
    ) {
        if (!queueAvailable()) {
            throw new IllegalStateException("No queue available until current queue finished.");
        }

        // Cancel any existing queues/results
        clearEverythingForOwner(owner);

        this.currentQueue = restoreFactory.create(modificationRuleset, owner, query, modifications, this::onEnd);

        return this.currentQueue;
    }

    /**
     * On queue end, handle some cleanup.
     *
     * @param result Modification queue result
     */
    protected void onEnd(ModificationQueueResult result) {
        queueResults.put(currentQueue.owner(), result);

        if (result.mode().equals(ModificationQueueMode.COMPLETING)) {
            // Message the user with results
            if (currentQueue.owner() instanceof CommandSender sender) {
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
            cancelQueueForOwner(currentQueue.owner());
        } else if (result.mode().equals(ModificationQueueMode.PLANNING)) {
            // Message the user with results
            if (currentQueue.owner() instanceof CommandSender sender) {
                messageService.modificationsAppliedSuccess(sender, result.planned());
            }
        }
    }

    @Override
    public Optional<ModificationQueueResult> queueResultForOwner(Object owner) {
        return Optional.ofNullable(queueResults.getIfPresent(owner));
    }
}
