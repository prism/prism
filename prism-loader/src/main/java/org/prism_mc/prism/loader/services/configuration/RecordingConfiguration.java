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

    /**
     * Get the parallelism value, clamped to [1, 4].
     *
     * @return The parallelism value
     */
    public int parallelism() {
        return Math.max(1, Math.min(4, parallelism));
    }
}
