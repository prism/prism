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

package org.prism_mc.prism.api.actions;

import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.containers.BlockContainer;

public interface BlockAction extends CustomData {
    /**
     * The block container.
     *
     * @return Block container
     */
    BlockContainer blockContainer();

    /**
     * The replaced block container.
     *
     * @return Block container
     */
    @Nullable
    BlockContainer replacedBlockContainer();
}
