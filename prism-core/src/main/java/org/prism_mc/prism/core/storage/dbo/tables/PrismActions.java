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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIONS;
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_DATABASE;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
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
import org.prism_mc.prism.core.storage.dbo.records.PrismActionsRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismActions extends TableImpl<PrismActionsRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismActionsRecord> getRecordType() {
        return PrismActionsRecord.class;
    }

    /**
     * The column <code>prism_actions.action_id</code>.
     */
    public final TableField<PrismActionsRecord, UInteger> ACTION_ID = createField(
        DSL.name("action_id"),
        SQLDataType.INTEGERUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_actions.action</code>.
     */
    public final TableField<PrismActionsRecord, String> ACTION = createField(
        DSL.name("action"),
        SQLDataType.VARCHAR(25).nullable(false),
        this,
        ""
    );

    private PrismActions(String prefix, Name alias, Table<PrismActionsRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismActions(String prefix, Name alias, Table<PrismActionsRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismActions(String prefix) {
        this(prefix, DSL.name(prefix + "actions"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismActions(String prefix, Table<O> child, ForeignKey<O, PrismActionsRecord> key) {
        super(child, key, PRISM_ACTIONS);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public Identity<PrismActionsRecord, UInteger> getIdentity() {
        return (Identity<PrismActionsRecord, UInteger>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismActionsRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_ACTIONS_PRIMARY;
    }

    @Override
    public List<UniqueKey<PrismActionsRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_PRISM_ACTIONS_ACTION);
    }

    @Override
    public PrismActions as(String alias) {
        return new PrismActions(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismActions as(Name alias) {
        return new PrismActions(prefix, alias, this);
    }

    @Override
    public PrismActions rename(String name) {
        return new PrismActions(prefix, DSL.name(name), null);
    }

    @Override
    public PrismActions rename(Name name) {
        return new PrismActions(prefix, name, null);
    }

    @Override
    public Row2<UInteger, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
