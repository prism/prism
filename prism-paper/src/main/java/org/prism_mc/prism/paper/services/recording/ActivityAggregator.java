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

package org.prism_mc.prism.paper.services.recording;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.inventory.ItemStack;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.paper.actions.PaperItemStackAction;
import org.prism_mc.prism.paper.api.activities.PaperActivity;

public class ActivityAggregator {

    /**
     * Key for aggregation grouping.
     */
    private record AggregationKey(String actionKey, UUID worldUuid, int x, int y, int z, String material) {}

    /**
     * An aggregation entry holding a template activity and accumulated quantity.
     */
    private static class AggregationEntry {

        final Activity templateActivity;
        final int maxStackSize;
        final AtomicInteger quantity;
        final long createdAt;

        AggregationEntry(Activity activity, int initialQuantity, int maxStackSize) {
            this.templateActivity = activity;
            this.maxStackSize = maxStackSize;
            this.quantity = new AtomicInteger(initialQuantity);
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * The aggregation buffer.
     */
    private final ConcurrentHashMap<AggregationKey, AggregationEntry> buffer = new ConcurrentHashMap<>();

    /**
     * Entries that reached max stack size and are ready to flush immediately.
     */
    private final LinkedBlockingQueue<AggregationEntry> fullEntries = new LinkedBlockingQueue<>();

    /**
     * The minimum age (in milliseconds) before an entry is eligible for flushing.
     */
    private final long minAgeMs;

    /**
     * Construct an aggregator.
     *
     * @param minAgeTicks The minimum age in ticks before entries are flushed
     */
    public ActivityAggregator(long minAgeTicks) {
        // Convert ticks to milliseconds (1 tick = 50ms)
        this.minAgeMs = minAgeTicks * 50;
    }

    /**
     * Aggregate an activity into the buffer. When an entry reaches the item's
     * max stack size, it is flushed to the pending queue and a new entry begins.
     *
     * @param activity The activity to aggregate
     */
    public void aggregate(Activity activity) {
        PaperItemStackAction action = (PaperItemStackAction) activity.action();

        AggregationKey key = new AggregationKey(
            action.type().key(),
            activity.worldUuid(),
            activity.coordinate().intX(),
            activity.coordinate().intY(),
            activity.coordinate().intZ(),
            action.material().name()
        );

        buffer.compute(key, (k, existing) -> {
            if (existing == null) {
                return new AggregationEntry(activity, action.quantity(), action.itemStack().getMaxStackSize());
            }

            int newTotal = existing.quantity.get() + action.quantity();
            if (newTotal > existing.maxStackSize) {
                // Entry is full — flush it and start a new one
                fullEntries.offer(existing);
                return new AggregationEntry(activity, action.quantity(), existing.maxStackSize);
            }

            existing.quantity.addAndGet(action.quantity());
            return existing;
        });
    }

    /**
     * Flush full entries and aged entries into the recording queue.
     *
     * @param sink The consumer to receive flushed activities
     */
    public void flush(Consumer<Activity> sink) {
        // Drain entries that hit max stack size
        drainFullEntries(sink);

        // Flush aged entries from the buffer
        long now = System.currentTimeMillis();
        for (AggregationKey key : buffer.keySet()) {
            AggregationEntry entry = buffer.get(key);
            if (entry == null) {
                continue;
            }

            if (now - entry.createdAt < minAgeMs) {
                continue;
            }

            entry = buffer.remove(key);
            if (entry == null) {
                continue;
            }

            sink.accept(buildAggregatedActivity(entry));
        }
    }

    /**
     * Flush all entries regardless of age. Used during shutdown.
     *
     * @param sink The consumer to receive flushed activities
     */
    public void flushAll(Consumer<Activity> sink) {
        drainFullEntries(sink);

        for (AggregationKey key : buffer.keySet()) {
            AggregationEntry entry = buffer.remove(key);
            if (entry == null) {
                continue;
            }

            sink.accept(buildAggregatedActivity(entry));
        }
    }

    /**
     * Drain all full entries to the consumer.
     *
     * @param sink The consumer to receive flushed activities
     */
    private void drainFullEntries(Consumer<Activity> sink) {
        AggregationEntry entry;
        while ((entry = fullEntries.poll()) != null) {
            sink.accept(buildAggregatedActivity(entry));
        }
    }

    /**
     * Build an activity from an aggregation entry.
     *
     * @param entry The aggregation entry
     * @return The aggregated activity
     */
    private Activity buildAggregatedActivity(AggregationEntry entry) {
        Activity template = entry.templateActivity;
        int totalQuantity = entry.quantity.get();

        PaperItemStackAction templateAction = (PaperItemStackAction) template.action();
        ItemStack newStack = templateAction.itemStack().clone();
        newStack.setAmount(totalQuantity);

        var action = new PaperItemStackAction(
            templateAction.type(),
            newStack,
            totalQuantity,
            templateAction.descriptor()
        );

        return PaperActivity.builder()
            .action(action)
            .world(template.world())
            .coordinate(template.coordinate())
            .cause(template.cause())
            .build();
    }
}
