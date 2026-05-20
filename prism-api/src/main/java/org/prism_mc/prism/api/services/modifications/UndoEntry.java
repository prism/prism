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

/**
 * A snapshot of an in-world state taken before a modification overwrites it,
 * sufficient to replay the original world state on undo.
 *
 * <p>Captured directly from the live world rather than re-derived from logged
 * activities, so undo is robust against gaps in the activity log (purge, missed
 * events, concurrent edits by other plugins).</p>
 *
 * <p>Platform-specific implementations carry coordinates and block data; this
 * marker keeps the public {@link ModificationQueueResult} contract abstract.</p>
 */
public interface UndoEntry {}
