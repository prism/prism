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

package org.prism_mc.prism.core.storage.dbo.tables;

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_DATABASE;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row22;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;
import org.prism_mc.prism.core.storage.dbo.Indexes;
import org.prism_mc.prism.core.storage.dbo.Keys;
import org.prism_mc.prism.core.storage.dbo.records.PrismActivitiesRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismActivities extends TableImpl<PrismActivitiesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismActivitiesRecord> getRecordType() {
        return PrismActivitiesRecord.class;
    }

    /**
     * The column <code>prism_activities.activity_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> ACTIVITY_ID = createField(
        DSL.name("activity_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.timestamp</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> TIMESTAMP = createField(
        DSL.name("timestamp"),
        SQLDataType.INTEGERUNSIGNED.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.world_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> WORLD_ID = createField(
        DSL.name("world_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.x</code>.
     */
    public final TableField<PrismActivitiesRecord, Integer> X = createField(
        DSL.name("x"),
        SQLDataType.INTEGER.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.y</code>.
     */
    public final TableField<PrismActivitiesRecord, Integer> Y = createField(
        DSL.name("y"),
        SQLDataType.INTEGER.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.z</code>.
     */
    public final TableField<PrismActivitiesRecord, Integer> Z = createField(
        DSL.name("z"),
        SQLDataType.INTEGER.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.action_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> ACTION_ID = createField(
        DSL.name("action_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.item_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> AFFECTED_ITEM_ID = createField(
        DSL.name("affected_item_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.item_quantity</code>.
     */
    public final TableField<PrismActivitiesRecord, UShort> AFFECTED_ITEM_QUANTITY = createField(
        DSL.name("affected_item_quantity"),
        SQLDataType.SMALLINTUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.block_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> AFFECTED_BLOCK_ID = createField(
        DSL.name("affected_block_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.replaced_block_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> REPLACED_BLOCK_ID = createField(
        DSL.name("replaced_block_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.entity_type_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> AFFECTED_ENTITY_TYPE_ID = createField(
        DSL.name("affected_entity_type_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_causes.affected_player_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> AFFECTED_PLAYER_ID = createField(
        DSL.name("affected_player_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.cause_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> CAUSE_ID = createField(
        DSL.name("cause_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_causes.cause_player_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> CAUSE_PLAYER_ID = createField(
        DSL.name("cause_player_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_causes.cause_entity_type_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> CAUSE_ENTITY_TYPE_ID = createField(
        DSL.name("cause_entity_type_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_causes.cause_block_id</code>.
     */
    public final TableField<PrismActivitiesRecord, UInteger> CAUSE_BLOCK_ID = createField(
        DSL.name("cause_block_id"),
        SQLDataType.INTEGERUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.descriptor</code>.
     */
    public final TableField<PrismActivitiesRecord, String> DESCRIPTOR = createField(
        DSL.name("descriptor"),
        SQLDataType.VARCHAR(256),
        this,
        ""
    );

    /**
     * The column <code>prism_activities.metadata</code>.
     */
    public final TableField<PrismActivitiesRecord, String> METADATA = createField(
        DSL.name("metadata"),
        SQLDataType.VARCHAR(255),
        this,
        ""
    );

    /**
     * The column <code>prism_blocks.serializer_version</code>.
     */
    public final TableField<PrismActivitiesRecord, UShort> SERIALIZER_VERSION = createField(
        DSL.name("serializer_version"),
        SQLDataType.SMALLINTUNSIGNED,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.serialized_data</code>.
     */
    public final TableField<PrismActivitiesRecord, String> SERIALIZED_DATA = createField(
        DSL.name("serialized_data"),
        SQLDataType.CLOB,
        this,
        ""
    );

    /**
     * The column <code>prism_activities.reversed</code>.
     */
    public final TableField<PrismActivitiesRecord, Boolean> REVERSED = createField(
        DSL.name("reversed"),
        SQLDataType.BIT.nullable(false).defaultValue(false),
        this,
        ""
    );

    private PrismActivities(String prefix, Name alias, Table<PrismActivitiesRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismActivities(String prefix, Name alias, Table<PrismActivitiesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismActivities(String prefix) {
        this(prefix, DSL.name(prefix + "activities"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismActivities(String prefix, Table<O> child, ForeignKey<O, PrismActivitiesRecord> key) {
        super(child, key, PRISM_ACTIVITIES);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(
            Indexes.PRISM_ACTIVITIES_ACTION_ID,
            Indexes.PRISM_ACTIVITIES_AFFECTED_PLAYER_ID,
            Indexes.PRISM_ACTIVITIES_CAUSE_ID,
            Indexes.PRISM_ACTIVITIES_CAUSE_PLAYER_ID,
            Indexes.PRISM_ACTIVITIES_CAUSE_ENTITY_TYPE_ID,
            Indexes.PRISM_ACTIVITIES_AFFECTED_BLOCK_ID,
            Indexes.PRISM_ACTIVITIES_COORDINATE,
            Indexes.PRISM_ACTIVITIES_AFFECTED_ENTITY_TYPE_ID,
            Indexes.PRISM_ACTIVITIES_AFFECTED_ITEM_ID,
            Indexes.PRISM_ACTIVITIES_AFFECTED_BLOCK_ID,
            Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID,
            Indexes.PRISM_ACTIVITIES_WORLDID
        );
    }

    @Override
    public Identity<PrismActivitiesRecord, UInteger> getIdentity() {
        return (Identity<PrismActivitiesRecord, UInteger>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismActivitiesRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_ACTIVITIES_PRIMARY;
    }

    @Override
    public List<ForeignKey<PrismActivitiesRecord, ?>> getReferences() {
        return Arrays.asList(
            Keys.WORLDID,
            Keys.ACTIONID,
            Keys.ITEMID,
            Keys.BLOCKID,
            Keys.REPLACEDBLOCKID,
            Keys.ENTITYTYPEID,
            Keys.AFFECTEDPLAYERID,
            Keys.CAUSEID,
            Keys.CAUSEPLAYERID,
            Keys.CAUSEENTITYTYPEID,
            Keys.CAUSEBLOCKID
        );
    }

    private transient PrismWorlds prismWorlds;
    private transient PrismActions prismActions;
    private transient PrismItems itemid;
    private transient PrismBlocks blockid;
    private transient PrismBlocks replacedblockid;
    private transient PrismEntityTypes prismEntityTypes;
    private transient PrismCauses prismCauses;

    /**
     * Get the implicit join path to the <code>prism_worlds</code>
     * table.
     */
    public PrismWorlds prismWorlds() {
        if (prismWorlds == null) {
            prismWorlds = new PrismWorlds(prefix, this, Keys.WORLDID);
        }

        return prismWorlds;
    }

    /**
     * Get the implicit join path to the <code>prism_actions</code>
     * table.
     */
    public PrismActions prismActions() {
        if (prismActions == null) {
            prismActions = new PrismActions(prefix, this, Keys.ACTIONID);
        }

        return prismActions;
    }

    /**
     * Get the implicit join path to the <code>prism_items</code>
     * table, via the <code>itemId</code> key.
     */
    public PrismItems itemid() {
        if (itemid == null) {
            itemid = new PrismItems(prefix, this, Keys.ITEMID);
        }

        return itemid;
    }

    /**
     * Get the implicit join path to the <code>prism_blocks</code>
     * table, via the <code>blockId</code> key.
     */
    public PrismBlocks blockid() {
        if (blockid == null) {
            blockid = new PrismBlocks(prefix, this, Keys.BLOCKID);
        }

        return blockid;
    }

    /**
     * Get the implicit join path to the <code>prism_blocks</code>
     * table, via the <code>blockId</code> key.
     */
    public PrismBlocks replacedblockid() {
        if (replacedblockid == null) {
            replacedblockid = new PrismBlocks(prefix, this, Keys.REPLACEDBLOCKID);
        }

        return replacedblockid;
    }

    /**
     * Get the implicit join path to the
     * <code>prism_entity_types</code> table.
     */
    public PrismEntityTypes prismEntityTypes() {
        if (prismEntityTypes == null) {
            prismEntityTypes = new PrismEntityTypes(prefix, this, Keys.ENTITYTYPEID);
        }

        return prismEntityTypes;
    }

    @Override
    public PrismActivities as(String alias) {
        return new PrismActivities(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismActivities as(Name alias) {
        return new PrismActivities(prefix, alias, this);
    }

    @Override
    public PrismActivities rename(String name) {
        return new PrismActivities(prefix, DSL.name(name), null);
    }

    @Override
    public PrismActivities rename(Name name) {
        return new PrismActivities(prefix, name, null);
    }

    @Override
    public Row22<
        UInteger,
        UInteger,
        UInteger,
        Integer,
        Integer,
        Integer,
        UInteger,
        UInteger,
        UShort,
        UInteger,
        UInteger,
        UInteger,
        UInteger,
        UInteger,
        UInteger,
        UInteger,
        UInteger,
        String,
        String,
        UShort,
        String,
        Boolean
    > fieldsRow() {
        return (Row22) super.fieldsRow();
    }
}
