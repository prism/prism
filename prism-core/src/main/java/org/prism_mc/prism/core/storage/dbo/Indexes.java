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

package org.prism_mc.prism.core.storage.dbo;

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    public static final Index PRISM_ACTIVITIES_COORDINATE = Internal.createIndex(
        DSL.name("idx_prism_coordinates"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.X, PRISM_ACTIVITIES.Z, PRISM_ACTIVITIES.Y, PRISM_ACTIVITIES.TIMESTAMP },
        false
    );

    public static final Index PRISM_ACTIVITIES_ACTION_ID = Internal.createIndex(
        DSL.name("idx_prism_actionId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.ACTION_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_AFFECTED_ENTITY_TYPE_ID = Internal.createIndex(
        DSL.name("idx_prism_affectedEntityTypeId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_AFFECTED_ITEM_ID = Internal.createIndex(
        DSL.name("idx_prism_affectedItemId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.AFFECTED_ITEM_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_AFFECTED_BLOCK_ID = Internal.createIndex(
        DSL.name("idx_prism_affectedBlockId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.AFFECTED_BLOCK_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_REPLACED_BLOCK_ID = Internal.createIndex(
        DSL.name("idx_prism_replacedBlockId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.REPLACED_BLOCK_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_AFFECTED_PLAYER_ID = Internal.createIndex(
        DSL.name("idx_prism_affectedPlayerId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.AFFECTED_PLAYER_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_CAUSE_ID = Internal.createIndex(
        DSL.name("idx_prism_causeId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.CAUSE_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_CAUSE_ENTITY_TYPE_ID = Internal.createIndex(
        DSL.name("idx_prism_causeEntityTypeId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_CAUSE_BLOCK_ID = Internal.createIndex(
        DSL.name("idx_prism_causeBlockId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.CAUSE_BLOCK_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_CAUSE_PLAYER_ID = Internal.createIndex(
        DSL.name("idx_prism_causePlayerId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.CAUSE_PLAYER_ID },
        false
    );

    public static final Index PRISM_ACTIVITIES_WORLDID = Internal.createIndex(
        DSL.name("idx_prism_worldId"),
        PRISM_ACTIVITIES,
        new OrderField[] { PRISM_ACTIVITIES.WORLD_ID },
        false
    );
}
