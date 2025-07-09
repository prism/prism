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

import java.util.UUID;

public record ActionData(
    String material,
    short itemQuantity,
    String itemData,
    String blockNamespace,
    String blockName,
    String blockData,
    String replacedBlockNamespace,
    String replacedBlockName,
    String replacedBlockData,
    String entityType,
    String customData,
    String descriptor,
    String metadata,
    short customDataVersion,
    String translationKey,
    String replacedBlockTranslationKey,
    String affectedPlayerName,
    UUID affectedPlayerUuid
) {}
