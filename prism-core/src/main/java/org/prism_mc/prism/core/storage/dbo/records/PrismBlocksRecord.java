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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_BLOCKS;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismBlocksRecord
    extends UpdatableRecordImpl<PrismBlocksRecord>
    implements Record5<UInteger, String, String, String, String>
{

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_blocks.block_id</code>.
     */
    public PrismBlocksRecord setBlockId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_blocks.block_id</code>.
     */
    public UInteger getBlockId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_blocks.ns</code>.
     */
    public PrismBlocksRecord setNs(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_blocks.ns</code>.
     */
    public String getNs() {
        return (String) get(1);
    }

    /**
     * Getter for <code>prism_blocks.name</code>.
     */
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>prism_blocks.name</code>.
     */
    public PrismBlocksRecord setName(String value) {
        set(2, value);
        return this;
    }

    /**
     * Setter for <code>prism_blocks.data</code>.
     */
    public PrismBlocksRecord setData(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>prism_blocks.data</code>.
     */
    public String getData() {
        return (String) get(3);
    }

    /**
     * Setter for <code>prism_blocks.nbt</code>.
     */
    public PrismBlocksRecord setNbt(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>prism_blocks.nbt</code>.
     */
    public String getNbt() {
        return (String) get(4);
    }

    /**
     * Setter for <code>prism_blocks.translation_key</code>.
     */
    public PrismBlocksRecord setTranslationKey(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>prism_blocks.translation_key</code>.
     */
    public String getTranslationKey() {
        return (String) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<UInteger, String, String, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<UInteger, String, String, String, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return PRISM_BLOCKS.BLOCK_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_BLOCKS.NS;
    }

    @Override
    public Field<String> field3() {
        return PRISM_BLOCKS.NAME;
    }

    @Override
    public Field<String> field4() {
        return PRISM_BLOCKS.DATA;
    }

    @Override
    public Field<String> field5() {
        return PRISM_BLOCKS.TRANSLATION_KEY;
    }

    @Override
    public UInteger component1() {
        return getBlockId();
    }

    @Override
    public String component2() {
        return getNs();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public String component4() {
        return getData();
    }

    @Override
    public String component5() {
        return getTranslationKey();
    }

    @Override
    public UInteger value1() {
        return getBlockId();
    }

    @Override
    public PrismBlocksRecord value1(UInteger value) {
        setBlockId(value);
        return this;
    }

    @Override
    public String value2() {
        return getNs();
    }

    @Override
    public PrismBlocksRecord value2(String value) {
        setNs(value);
        return this;
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public PrismBlocksRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public String value4() {
        return getData();
    }

    @Override
    public PrismBlocksRecord value4(String value) {
        setData(value);
        return this;
    }

    @Override
    public String value5() {
        return getTranslationKey();
    }

    @Override
    public PrismBlocksRecord value5(String value) {
        setTranslationKey(value);
        return this;
    }

    @Override
    public PrismBlocksRecord values(UInteger value1, String value2, String value3, String value4, String value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismBlocksRecord.
     */
    public PrismBlocksRecord() {
        super(PRISM_BLOCKS);
    }

    /**
     * Create a detached, initialised PrismBlocksRecord.
     */
    public PrismBlocksRecord(UInteger blockId, String ns, String name, String data, String translationKey) {
        super(PRISM_BLOCKS);
        setBlockId(blockId);
        setNs(ns);
        setName(name);
        setData(data);
        setTranslationKey(translationKey);
    }
}
