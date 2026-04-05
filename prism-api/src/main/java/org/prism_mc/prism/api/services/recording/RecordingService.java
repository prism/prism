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

package org.prism_mc.prism.api.services.recording;

import java.util.concurrent.LinkedBlockingQueue;
import org.prism_mc.prism.api.activities.Activity;

public interface RecordingService {
    /**
     * Add an activity to the recording queue.
     *
     * @param activity Activity
     * @return True if added to queue
     */
    boolean addToQueue(final Activity activity);

    /**
     * Cancels and removes the current recording task.
     */
    void clearTask();

    /**
     * Reset the dropped activity counter and return the count since the last reset.
     *
     * @return The number of activities dropped since the last reset
     */
    int resetDroppedCount();

    /**
     * Get the queue.
     *
     * @return the queue
     */
    LinkedBlockingQueue<Activity> queue();

    /**
     * Schedule the next recording task.
     *
     * @param task The task
     */
    void queueNextRecording(Runnable task);

    /**
     * Stop the recording service.
     */
    void stop();
}
