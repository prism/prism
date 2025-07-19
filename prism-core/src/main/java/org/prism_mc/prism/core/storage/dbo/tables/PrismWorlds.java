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
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_WORLDS;

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
import org.jooq.types.UInteger;
import org.prism_mc.prism.core.storage.dbo.Keys;
import org.prism_mc.prism.core.storage.dbo.records.PrismWorldsRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismWorlds extends TableImpl<PrismWorldsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismWorldsRecord> getRecordType() {
        return PrismWorldsRecord.class;
    }

    /**
     * The column <code>prism_worlds.world_id</code>.
     */
    public final TableField<PrismWorldsRecord, UInteger> WORLD_ID = createField(
        DSL.name("world_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_worlds.world</code>.
     */
    public final TableField<PrismWorldsRecord, String> WORLD = createField(
        DSL.name("world"),
        SQLDataType.VARCHAR(255).nullable(false),
        this,
        ""
    );

    /**
     * The column <code>prism_worlds.world_uuid</code>.
     */
    public final TableField<PrismWorldsRecord, String> WORLD_UUID = createField(
        DSL.name("world_uuid"),
        SQLDataType.CHAR(36).nullable(false),
        this,
        ""
    );

    private PrismWorlds(String prefix, Name alias, Table<PrismWorldsRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismWorlds(String prefix, Name alias, Table<PrismWorldsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismWorlds(String prefix) {
        this(prefix, DSL.name(prefix + "worlds"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismWorlds(String prefix, Table<O> child, ForeignKey<O, PrismWorldsRecord> key) {
        super(child, key, PRISM_WORLDS);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public Identity<PrismWorldsRecord, UInteger> getIdentity() {
        return (Identity<PrismWorldsRecord, UInteger>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismWorldsRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_WORLDS_PRIMARY;
    }

    @Override
    public List<UniqueKey<PrismWorldsRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_PRISM_WORLDS_WORLD_UUID);
    }

    @Override
    public PrismWorlds as(String alias) {
        return new PrismWorlds(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismWorlds as(Name alias) {
        return new PrismWorlds(prefix, alias, this);
    }

    @Override
    public PrismWorlds rename(String name) {
        return new PrismWorlds(prefix, DSL.name(name), null);
    }

    @Override
    public PrismWorlds rename(Name name) {
        return new PrismWorlds(prefix, name, null);
    }

    @Override
    public Row3<UInteger, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
