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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_AIRTAGS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_DATABASE;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;
import org.prism_mc.prism.core.storage.dbo.Keys;
import org.prism_mc.prism.core.storage.dbo.records.PrismAirtagsRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismAirtags extends TableImpl<PrismAirtagsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismAirtagsRecord> getRecordType() {
        return PrismAirtagsRecord.class;
    }

    /**
     * The column <code>prism_airtags.airtag_id</code>.
     */
    public final TableField<PrismAirtagsRecord, UInteger> AIRTAG_ID = createField(
        DSL.name("airtag_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_airtags.airtag</code>.
     */
    public final TableField<PrismAirtagsRecord, String> AIRTAG = createField(
        DSL.name("airtag"),
        SQLDataType.CHAR(6).nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_airtags.player_id</code>.
     */
    public final TableField<PrismAirtagsRecord, UInteger> PLAYER_ID = createField(
        DSL.name("player_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_airtags.created_at</code>.
     */
    public final TableField<PrismAirtagsRecord, UInteger> CREATED_AT = createField(
        DSL.name("created_at"),
        SQLDataType.INTEGERUNSIGNED.nullable(false),
        this,
        ""
    );

    private PrismAirtags(String prefix, Name alias, Table<PrismAirtagsRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismAirtags(String prefix, Name alias, Table<PrismAirtagsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismAirtags(String prefix) {
        this(prefix, DSL.name(prefix + "airtags"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismAirtags(String prefix, Table<O> child, ForeignKey<O, PrismAirtagsRecord> key) {
        super(child, key, PRISM_AIRTAGS);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public Identity<PrismAirtagsRecord, UInteger> getIdentity() {
        return (Identity<PrismAirtagsRecord, UInteger>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismAirtagsRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_AIRTAGS_PRIMARY;
    }

    @Override
    public List<UniqueKey<PrismAirtagsRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_PRISM_AIRTAGS_AIRTAG);
    }

    @Override
    public PrismAirtags as(String alias) {
        return new PrismAirtags(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismAirtags as(Name alias) {
        return new PrismAirtags(prefix, alias, this);
    }

    @Override
    public PrismAirtags rename(String name) {
        return new PrismAirtags(prefix, DSL.name(name), null);
    }

    @Override
    public PrismAirtags rename(Name name) {
        return new PrismAirtags(prefix, name, null);
    }

    @Override
    public Row4<UInteger, String, UInteger, UInteger> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
