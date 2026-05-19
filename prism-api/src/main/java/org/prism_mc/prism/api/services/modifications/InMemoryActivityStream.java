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
 * In-memory {@link ActivityStream} backed by a pre-loaded collection. Used by
 * callers that already have a materialized list (e.g. undo, which replays a
 * prior result's applied activities) and by the storage-list compatibility
 * shim.
 */
final class InMemoryActivityStream implements ActivityStream {

    private final List<Activity> source;
    private int cursor;
    private boolean closed;

    InMemoryActivityStream(Collection<Activity> activities) {
        this.source = List.copyOf(activities);
    }

    @Override
    public synchronized List<Activity> next(int limit) {
        if (closed || cursor >= source.size() || limit <= 0) {
            return List.of();
        }

        int end = Math.min(cursor + limit, source.size());
        List<Activity> chunk = List.copyOf(source.subList(cursor, end));
        cursor = end;
        return chunk;
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    @Override
    public synchronized void reopen() {
        if (closed) {
            throw new IllegalStateException("Cannot reopen a closed ActivityStream");
        }
        cursor = 0;
    }

    @Override
    public int total() {
        return source.size();
    }
}
