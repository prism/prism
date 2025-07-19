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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_DATABASE;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
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
import org.prism_mc.prism.core.storage.dbo.Keys;
import org.prism_mc.prism.core.storage.dbo.records.PrismItemsRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismItems extends TableImpl<PrismItemsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismItemsRecord> getRecordType() {
        return PrismItemsRecord.class;
    }

    /**
     * The column <code>prism_items.item_id</code>.
     */
    public final TableField<PrismItemsRecord, UInteger> ITEM_ID = createField(
        DSL.name("item_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_items.material</code>.
     */
    public final TableField<PrismItemsRecord, String> MATERIAL = createField(
        DSL.name("material"),
        SQLDataType.VARCHAR(45),
        this,
        ""
    );

    /**
     * The column <code>prism_items.data</code>.
     */
    public final TableField<PrismItemsRecord, String> DATA = createField(DSL.name("data"), SQLDataType.CLOB, this, "");

    private PrismItems(String prefix, Name alias, Table<PrismItemsRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismItems(String prefix, Name alias, Table<PrismItemsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismItems(String prefix) {
        this(prefix, DSL.name(prefix + "items"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismItems(String prefix, Table<O> child, ForeignKey<O, PrismItemsRecord> key) {
        super(child, key, PRISM_ITEMS);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public Identity<PrismItemsRecord, UShort> getIdentity() {
        return (Identity<PrismItemsRecord, UShort>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismItemsRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_ITEMS_PRIMARY;
    }

    @Override
    public PrismItems as(String alias) {
        return new PrismItems(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismItems as(Name alias) {
        return new PrismItems(prefix, alias, this);
    }

    @Override
    public PrismItems rename(String name) {
        return new PrismItems(prefix, DSL.name(name), null);
    }

    @Override
    public PrismItems rename(Name name) {
        return new PrismItems(prefix, name, null);
    }

    @Override
    public Row3<UShort, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
