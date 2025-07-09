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
import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ENTITY_TYPES;

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
import org.jooq.types.UShort;
import org.prism_mc.prism.core.storage.dbo.Keys;
import org.prism_mc.prism.core.storage.dbo.records.PrismEntityTypesRecord;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismEntityTypes extends TableImpl<PrismEntityTypesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The table prefix.
     */
    private final String prefix;

    /**
     * The class holding records for this type.
     */
    @Override
    public Class<PrismEntityTypesRecord> getRecordType() {
        return PrismEntityTypesRecord.class;
    }

    /**
     * The column <code>prism_entity_types.entity_type_id</code>.
     */
    public final TableField<PrismEntityTypesRecord, UShort> ENTITY_TYPE_ID = createField(
        DSL.name("entity_type_id"),
        SQLDataType.SMALLINTUNSIGNED.nullable(false).identity(true),
        this,
        ""
    );

    /**
     * The column <code>prism_entity_types.entity_type</code>.
     */
    public final TableField<PrismEntityTypesRecord, String> ENTITY_TYPE = createField(
        DSL.name("entity_type"),
        SQLDataType.VARCHAR(45),
        this,
        ""
    );

    /**
     * The column <code>prism_entity_types.translation_key</code>.
     */
    public final TableField<PrismEntityTypesRecord, String> TRANSLATION_KEY = createField(
        DSL.name("translation_key"),
        SQLDataType.VARCHAR(155),
        this,
        ""
    );

    private PrismEntityTypes(String prefix, Name alias, Table<PrismEntityTypesRecord> aliased) {
        this(prefix, alias, aliased, null);
    }

    private PrismEntityTypes(String prefix, Name alias, Table<PrismEntityTypesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
        this.prefix = prefix;
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     */
    public PrismEntityTypes(String prefix) {
        this(prefix, DSL.name(prefix + "entity_types"), null);
    }

    /**
     * Constructor.
     *
     * @param prefix The prefix
     * @param child The child table
     * @param key The key
     * @param <O> The record type
     */
    public <O extends Record> PrismEntityTypes(
        String prefix,
        Table<O> child,
        ForeignKey<O, PrismEntityTypesRecord> key
    ) {
        super(child, key, PRISM_ENTITY_TYPES);
        this.prefix = prefix;
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PRISM_DATABASE;
    }

    @Override
    public Identity<PrismEntityTypesRecord, UShort> getIdentity() {
        return (Identity<PrismEntityTypesRecord, UShort>) super.getIdentity();
    }

    @Override
    public UniqueKey<PrismEntityTypesRecord> getPrimaryKey() {
        return Keys.KEY_PRISM_ENTITY_TYPES_PRIMARY;
    }

    @Override
    public List<UniqueKey<PrismEntityTypesRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_PRISM_ENTITY_TYPES_ENTITYTYPE);
    }

    @Override
    public PrismEntityTypes as(String alias) {
        return new PrismEntityTypes(prefix, DSL.name(alias), this);
    }

    @Override
    public PrismEntityTypes as(Name alias) {
        return new PrismEntityTypes(prefix, alias, this);
    }

    @Override
    public PrismEntityTypes rename(String name) {
        return new PrismEntityTypes(prefix, DSL.name(name), null);
    }

    @Override
    public PrismEntityTypes rename(Name name) {
        return new PrismEntityTypes(prefix, name, null);
    }

    @Override
    public Row3<UShort, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
