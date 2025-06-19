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

package org.prism_mc.prism.api.services.purges;

import org.prism_mc.prism.api.activities.ActivityQuery;

public interface PurgeQueue {
    /**
     * Add a query to the purge queue.
     *
     * @param query The query
     */
    void add(ActivityQuery query);

    /**
     * Check if the queue is running a purge cycle.
     *
     * @return True if running
     */
    boolean running();

    /**
     * Start the purge.
     */
    void start();

    /**
     * Stop the purge.
     */
    void stop();
}
