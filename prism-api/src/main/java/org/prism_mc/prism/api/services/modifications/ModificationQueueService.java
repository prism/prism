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

package org.prism_mc.prism.api.services.modifications;

import java.util.Optional;
import org.prism_mc.prism.api.activities.ActivityQuery;

public interface ModificationQueueService {
    /**
     * Check if the queue is free. Nicer than trying and getting exceptions.
     *
     * @return True if a new queue can be made.
     */
    boolean queueAvailable();

    /**
     * Cancel an active queue for a given owner.
     *
     * @param owner The owner
     * @return True if the queue was cleared
     */
    boolean cancelQueueForOwner(Object owner);

    /**
     * Clear active queues and/or recent queue results for a given owner.
     *
     * @param owner The owner
     */
    void clearEverythingForOwner(Object owner);

    /**
     * The current modification queue.
     *
     * @return The queue
     */
    ModificationQueue currentQueue();

    /**
     * Get the current queue only if it's owned by a given owner.
     *
     * @param owner The owner
     * @return The queue, if any
     */
    Optional<ModificationQueue> currentQueueForOwner(Object owner);

    /**
     * Create a new modification queue backed by a streaming source.
     *
     * @param clazz The modification queue class
     * @param owner The owner
     * @param query The query used
     * @param activityStream An activity stream
     * @return The rollback queue
     * @throws IllegalStateException If queue can't be created
     */
    ModificationQueue newQueue(
        Class<? extends ModificationQueue> clazz,
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream
    );

    /**
     * Get the default modification ruleset, populated from the server's configuration.
     *
     * <p>Use this when you do not need to customize behavior — pass it to
     * {@link #newRollbackQueue(ModificationRuleset, Object, ActivityQuery, ActivityStream)}
     * or the other queue factories.</p>
     *
     * @return A ruleset using the server's configured defaults
     */
    ModificationRuleset defaultModificationRuleset();

    /**
     * Create a new rollback queue backed by a streaming source.
     *
     * @param owner The owner
     * @param query The query used
     * @param activityStream An activity stream
     * @return The rollback queue
     * @throws IllegalStateException If queue can't be created
     */
    ModificationQueue newRollbackQueue(
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream
    );

    /**
     * Create a new restore queue backed by a streaming source.
     *
     * @param owner The owner
     * @param query The query used
     * @param activityStream An activity stream
     * @return The restore queue
     * @throws IllegalStateException If queue can't be created
     */
    ModificationQueue newRestoreQueue(
        ModificationRuleset modificationRuleset,
        Object owner,
        ActivityQuery query,
        ActivityStream activityStream
    );

    /**
     * Get a queue result for a given owner.
     *
     * @param owner The owner
     * @return The queue result
     */
    Optional<ModificationQueueResult> queueResultForOwner(Object owner);
}
