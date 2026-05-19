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

import java.util.Collection;
import java.util.List;
import org.prism_mc.prism.api.activities.Activity;

/**
 * A pull-based source of activities for modification queues.
 *
 * <p>Backed by either a SQL implementation that pre-fetches primary keys and
 * loads records in batches, or an in-memory implementation that drains an
 * existing collection. Callers fetch chunks via {@link #next(int)} until an
 * empty list is returned, then invoke {@link #close()}.</p>
 */
public interface ActivityStream extends AutoCloseable {
    /**
     * Fetch the next batch of activities, up to the given limit.
     *
     * @param limit Maximum number of activities to return
     * @return The next batch; an empty list signals end of stream
     * @throws Exception If the underlying storage layer fails
     */
    List<Activity> next(int limit) throws Exception;

    /**
     * Release any held resources. Safe to call multiple times.
     */
    @Override
    void close();

    /**
     * Reset iteration state so {@link #next(int)} starts from the beginning. Used to replay
     * a stream across a preview → apply sequence without re-running the underlying query.
     * Must not be called after {@link #close()}.
     */
    void reopen();

    /**
     * Total number of activities the stream will yield, if known.
     *
     * @return The total count
     */
    int total();

    /**
     * Wrap an in-memory collection as a stream that drains in chunks.
     *
     * @param activities The activities
     * @return A stream over the activities
     */
    static ActivityStream of(Collection<Activity> activities) {
        return new InMemoryActivityStream(activities);
    }
}
