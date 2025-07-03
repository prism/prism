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

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public final class ModificationQueueResult {

    /**
     * The count of activities applied.
     */
    @Builder.Default
    private int applied = 0;

    /**
     * The count of lava blocks drained.
     */
    @Builder.Default
    private int drainedLava = 0;

    /**
     * The modification mode.
     */
    @NonNull
    private ModificationQueueMode mode;

    /**
     * The count of entities moved.
     */
    @Builder.Default
    private int movedEntities = 0;

    /**
     * The count of activities partially applied.
     */
    @Builder.Default
    private int partial = 0;

    /**
     * The count of activities planned.
     */
    @Builder.Default
    private int planned = 0;

    /**
     * The source queue.
     */
    private ModificationQueue queue;

    /**
     * The count of blocks removed.
     */
    @Builder.Default
    private int removedBlocks = 0;

    /**
     * The count of drops removed.
     */
    @Builder.Default
    private int removedDrops = 0;

    /**
     * The modification results.
     */
    @NonNull
    private List<ModificationResult> results;

    /**
     * The count of activities skipped.
     */
    @Builder.Default
    private int skipped = 0;
}
