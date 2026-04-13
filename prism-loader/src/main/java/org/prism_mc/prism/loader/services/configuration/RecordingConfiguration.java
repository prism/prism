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

package org.prism_mc.prism.loader.services.configuration;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
@Getter
public class RecordingConfiguration {

    @Comment("The delay (in ticks) between queued data being commit to the DB.")
    private long delay = 10;

    @Comment(
        """
        Max seconds to spend draining the queue on shutdown. Set to 0 to disable.
        Any services with stall detection will assume the server is frozen and may
        attempt to kill the process. The longer the queue drain timeout is, the more
        likely that may be. That may cause more harm than good."""
    )
    private int drainTimeoutSeconds = 10;

    @Comment(
        """
        Maximum number of activities that can be queued in memory. Acts as a safety cap
        to prevent out-of-memory errors when the database can't keep up. Activities that
        exceed this limit are dropped. Set to 0 for unlimited (not recommended)."""
    )
    private int queueMaxCapacity = 100000;

    @Comment(
        """
        Number of parallel recording tasks that can drain the queue concurrently.
        Higher values drain the queue faster but use more database connections.
        Only increase this if your database server can handle additional concurrent
        connections. Most shared database hosts should leave this at 1. Max: 4."""
    )
    private int parallelism = 1;

    @Comment(
        """
        When enabled, certain high-frequency actions (e.g. hopper-insert, hopper-remove)
        are aggregated into a single record per location/item type. This dramatically
        reduces database writes for automated systems like hopper farms."""
    )
    private boolean aggregateActivities = true;

    @Comment(
        """
        How long (in ticks) aggregatable activities are held before being flushed to the
        database. Longer intervals aggregate more events into fewer records but delay when
        they appear in lookups. Default is 520 ticks (26 seconds), just above the time for
        a hopper to move a full stack of 64 items."""
    )
    private long aggregationInterval = 520;

    @Comment(
        """
        Write-ahead log mode for activity queue persistence. If the database is unavailable
        during shutdown, uncommitted activities are saved to a local WAL file and replayed
        on next startup. WAL data is discarded after crashes since the world state may have
        reverted to the last auto-save.
        Options:
          disabled  - No WAL (default)
          on-demand - Only writes to WAL on database failure or shutdown with a remaining queue.
                      Zero overhead during normal operation.
          always    - Continuously writes all activities to WAL as they enter the queue.
                      Provides protection against mid-operation database failures but adds
                      constant disk I/O."""
    )
    private String walMode = "disabled";

    @Comment(
        """
        How often (in milliseconds) the WAL buffer is flushed to disk. Only applies when
        walMode is "always". Lower values reduce potential data loss but increase disk I/O.
        Default is 1000ms (1 second)."""
    )
    private int walFlushIntervalMs = 1000;

    /**
     * Get the parallelism value, clamped to [1, 4].
     *
     * @return The parallelism value
     */
    public int parallelism() {
        return Math.max(1, Math.min(4, parallelism));
    }
}
