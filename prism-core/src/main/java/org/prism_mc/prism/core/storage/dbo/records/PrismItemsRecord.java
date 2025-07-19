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

package org.prism_mc.prism.core.storage.dbo.records;

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ITEMS;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismItemsRecord
    extends UpdatableRecordImpl<PrismItemsRecord>
    implements Record3<UInteger, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_items.item_id</code>.
     */
    public PrismItemsRecord setItemId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_items.item_id</code>.
     */
    public UInteger getItemId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_items.material</code>.
     */
    public PrismItemsRecord setMaterial(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_items.material</code>.
     */
    public String getMaterial() {
        return (String) get(1);
    }

    /**
     * Setter for <code>prism_items.data</code>.
     */
    public PrismItemsRecord setData(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>prism_items.data</code>.
     */
    public String getData() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UShort> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<UInteger, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<UInteger, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return PRISM_ITEMS.ITEM_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_ITEMS.MATERIAL;
    }

    @Override
    public Field<String> field3() {
        return PRISM_ITEMS.DATA;
    }

    @Override
    public UInteger component1() {
        return getItemId();
    }

    @Override
    public String component2() {
        return getMaterial();
    }

    @Override
    public String component3() {
        return getData();
    }

    @Override
    public UInteger value1() {
        return getItemId();
    }

    @Override
    public PrismItemsRecord value1(UInteger value) {
        setItemId(value);
        return this;
    }

    @Override
    public String value2() {
        return getMaterial();
    }

    @Override
    public PrismItemsRecord value2(String value) {
        setMaterial(value);
        return this;
    }

    @Override
    public String value3() {
        return getData();
    }

    @Override
    public PrismItemsRecord value3(String value) {
        setData(value);
        return this;
    }

    @Override
    public PrismItemsRecord values(UInteger value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismItemsRecord.
     */
    public PrismItemsRecord() {
        super(PRISM_ITEMS);
    }

    /**
     * Create a detached, initialised PrismItemsRecord.
     */
    public PrismItemsRecord(UInteger itemId, String material, String data) {
        super(PRISM_ITEMS);
        setItemId(itemId);
        setMaterial(material);
        setData(data);
    }
}
