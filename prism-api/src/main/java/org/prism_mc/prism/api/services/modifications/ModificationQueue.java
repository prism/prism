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

import org.prism_mc.prism.api.activities.ActivityQuery;

public interface ModificationQueue {
    /**
     * Apply the modifications.
     */
    void apply();

    /**
     * Destroy this queue. Cancels task runners, etc.
     */
    void destroy();

    /**
     * Get the owner.
     *
     * @return The owner
     */
    Object owner();

    /**
     * Get the query used to create this queue.
     *
     * @return The activity query
     */
    ActivityQuery query();

    /**
     * Get the current mode of the queue.
     *
     * @return The mode
     */
    ModificationQueueMode mode();

    /**
     * The size of the current queue.
     *
     * @return Queue size
     */
    int queueSize();

    /**
     * Total number of activities the queue expects to process for the current
     * run, captured at apply/preview time. Returns 0 if no run is in flight or
     * the queue does not track progress.
     *
     * @return Total expected
     */
    default int total() {
        return 0;
    }

    /**
     * Number of activities the queue has processed so far for the current run.
     * Always {@code <= total()}.
     *
     * @return Processed count
     */
    default int processed() {
        return 0;
    }
}
