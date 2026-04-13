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

package org.prism_mc.prism.api.storage;

import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.storage.wal.WalRecord;

public interface ActivityBatch {
    /**
     * Start a new batch.
     *
     * @throws Exception Storage layer exception
     */
    void startBatch() throws Exception;

    /**
     * Add an activity object to the batch.
     *
     * @param activity The activity
     * @throws Exception Storage layer exception
     */
    void add(Activity activity) throws Exception;

    /**
     * Add a WAL record to the batch for replay.
     *
     * @param record The WAL record
     * @throws Exception Storage layer exception
     */
    void addFromWalRecord(WalRecord record) throws Exception;

    /**
     * Commit the batch.
     *
     * @throws Exception Storage layer exception
     */
    void commitBatch() throws Exception;
}
