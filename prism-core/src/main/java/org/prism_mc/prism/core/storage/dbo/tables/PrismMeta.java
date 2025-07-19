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
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_META;

import java.util.Arrays;
import java.util.List;
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
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.prism_mc.prism.core.storage.dbo.Keys;
import org.prism_mc.prism.core.storage.dbo.records.PrismMetaRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismMeta extends TableImpl<PrismMetaRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismMetaRecord> getRecordType() {
        return PrismMetaRecord.class;
    }

    /**
     * The column <code>prism_meta.meta_id</code>.
     */
    public final TableField<PrismMetaRecord, UInteger> META_ID = createField(
        DSL.name("meta_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_meta.k</code>.
     */
    public final TableField<PrismMetaRecord, String> K = createField(
        DSL.name("k"),
        SQLDataType.VARCHAR(25).nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_meta.v</code>.
     */
    public final TableField<PrismMetaRecord, String> V = createField(
        DSL.name("v"),
        SQLDataType.VARCHAR(155).nullable(false),
        this,
        ""
    );

    private PrismMeta(String prefix, Name alias, Table<PrismMetaRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismMeta(String prefix, Name alias, Table<PrismMetaRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismMeta(String prefix) {
        this(prefix, DSL.name(prefix + "meta"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismMeta(String prefix, Table<O> child, ForeignKey<O, PrismMetaRecord> key) {
        super(child, key, PRISM_META);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public Identity<PrismMetaRecord, UByte> getIdentity() {
        return (Identity<PrismMetaRecord, UByte>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismMetaRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_META_PRIMARY;
    }

    @Override
    public List<UniqueKey<PrismMetaRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_PRISM_META_K);
    }

    @Override
    public PrismMeta as(String alias) {
        return new PrismMeta(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismMeta as(Name alias) {
        return new PrismMeta(prefix, alias, this);
    }

    @Override
    public PrismMeta rename(String name) {
        return new PrismMeta(prefix, DSL.name(name), null);
    }

    @Override
    public PrismMeta rename(Name name) {
        return new PrismMeta(prefix, name, null);
    }

    @Override
    public Row3<UByte, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
