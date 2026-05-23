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

package org.prism_mc.prism.api.services.airtags;

/**
 * A row from {@code prism_airtags} joined with the airtagged item's material and NBT data.
 *
 * @param airtag The airtag ID
 * @param itemMaterial The Bukkit material key of the airtagged item
 * @param itemData The serialized NBT of the airtagged item at the time of airtagging
 * @param createdAtSeconds The epoch-seconds timestamp of when the airtag was created
 */
public record AirtagSummary(String airtag, String itemMaterial, String itemData, long createdAtSeconds) {}
