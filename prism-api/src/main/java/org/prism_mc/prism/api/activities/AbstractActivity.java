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

package org.prism_mc.prism.api.activities;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.prism_mc.prism.api.actions.Action;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.api.util.Pair;

/**
 * An abstract activity represents either an individual activity
 * that's being recorded or read back from storage, or a grouped
 * activity record being read from storage for display.
 */
@SuperBuilder
@Getter
public abstract class AbstractActivity {

    /**
     * The action.
     */
    protected Action action;

    /**
     * The cause.
     */
    protected Cause cause;

    /**
     * The coordinate, if any.
     */
    protected Coordinate coordinate;

    /**
     * The timestamp.
     */
    @Builder.Default
    protected long timestamp = System.currentTimeMillis();

    /**
     * The world.
     */
    protected Pair<UUID, String> world;

    /**
     * Construct an activity.
     *
     * @param action The action
     * @param coordinate The coordinate
     * @param cause The cause
     * @param timestamp The timestamp (or average)
     */
    public AbstractActivity(
        Action action,
        Pair<UUID, String> world,
        Coordinate coordinate,
        Cause cause,
        Long timestamp
    ) {
        this.action = action;
        this.cause = cause;
        this.coordinate = coordinate;
        this.timestamp = timestamp;
        this.world = world;
    }

    /**
     * Get the world UUID.
     *
     * @return The world UUID
     */
    public UUID worldUuid() {
        return world.key();
    }

    public abstract static class AbstractActivityBuilder<
        C extends AbstractActivity, B extends AbstractActivityBuilder<C, B>
    > {}
}
