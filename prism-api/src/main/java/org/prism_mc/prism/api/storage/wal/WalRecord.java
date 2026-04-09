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

package org.prism_mc.prism.api.storage.wal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A flat representation of an activity for write-ahead log persistence.
 *
 * <p>Contains all data needed to replay an activity into the database,
 * using only primitive and string types to avoid platform-specific
 * object dependencies.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = false)
public class WalRecord {

    // Core fields
    private long sequence;
    private long timestamp;
    private int x;
    private int y;
    private int z;
    private String worldUuid;
    private String worldName;
    private String actionKey;
    private String descriptor;

    // Cause fields (polymorphic - one set populated based on causeType)
    private String causeType;
    private String causePlayerUuid;
    private String causePlayerName;
    private String causeBlockNamespace;
    private String causeBlockName;
    private String causeBlockData;
    private String causeBlockTranslationKey;
    private String causeEntityType;
    private String causeEntityTranslationKey;
    private String causeString;

    // Entity action fields
    private String entityType;
    private String entityTranslationKey;

    // Item action fields
    private String itemMaterial;
    private String itemData;
    private int itemQuantity;

    // Block action fields
    private String blockNamespace;
    private String blockName;
    private String blockData;
    private String blockTranslationKey;

    // Replaced block fields
    private String replacedBlockNamespace;
    private String replacedBlockName;
    private String replacedBlockData;
    private String replacedBlockTranslationKey;

    // Player action fields
    private String affectedPlayerUuid;
    private String affectedPlayerName;

    // Metadata and custom data
    private String metadata;
    private short serializerVersion;
    private String serializedData;
}
