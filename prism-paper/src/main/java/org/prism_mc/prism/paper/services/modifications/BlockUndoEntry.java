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

package org.prism_mc.prism.paper.services.modifications;

import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.block.data.BlockData;
import org.prism_mc.prism.api.services.modifications.UndoEntry;
import org.prism_mc.prism.api.util.Coordinate;

/**
 * Per-block undo snapshot. Carries enough to restore the world to its
 * pre-modification state and to detect concurrent edits since the original
 * modification ran.
 *
 * @param activityPk  Primary key of the source activity, for reversing the
 *                    {@code reversed} flag in storage on undo
 * @param worldUuid   World the block lives in
 * @param coordinate  Block position
 * @param oldBlockData State at this position immediately before the original
 *                    modification overwrote it; the target undo will write back
 * @param newBlockData State the original modification wrote; checked against
 *                    the live block at undo time to skip locations that have
 *                    been changed since
 * @param oldTileNbt  Tile entity NBT captured at the same moment as
 *                    {@code oldBlockData}, or null for non-tile blocks
 */
public record BlockUndoEntry(
    long activityPk,
    UUID worldUuid,
    Coordinate coordinate,
    BlockData oldBlockData,
    BlockData newBlockData,
    @Nullable ReadWriteNBT oldTileNbt
)
    implements UndoEntry {}
