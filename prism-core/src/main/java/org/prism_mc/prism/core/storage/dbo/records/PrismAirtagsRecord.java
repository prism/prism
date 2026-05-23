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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_AIRTAGS;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismAirtagsRecord
    extends UpdatableRecordImpl<PrismAirtagsRecord>
    implements Record4<UInteger, String, UInteger, UInteger> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_airtags.airtag_id</code>.
     */
    public PrismAirtagsRecord setAirtagId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_airtags.airtag_id</code>.
     */
    public UInteger getAirtagId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_airtags.airtag</code>.
     */
    public PrismAirtagsRecord setAirtag(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_airtags.airtag</code>.
     */
    public String getAirtag() {
        return (String) get(1);
    }

    /**
     * Setter for <code>prism_airtags.player_id</code>.
     */
    public PrismAirtagsRecord setPlayerId(UInteger value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>prism_airtags.player_id</code>.
     */
    public UInteger getPlayerId() {
        return (UInteger) get(2);
    }

    /**
     * Setter for <code>prism_airtags.created_at</code>.
     */
    public PrismAirtagsRecord setCreatedAt(UInteger value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>prism_airtags.created_at</code>.
     */
    public UInteger getCreatedAt() {
        return (UInteger) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<UInteger, String, UInteger, UInteger> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<UInteger, String, UInteger, UInteger> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return PRISM_AIRTAGS.AIRTAG_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_AIRTAGS.AIRTAG;
    }

    @Override
    public Field<UInteger> field3() {
        return PRISM_AIRTAGS.PLAYER_ID;
    }

    @Override
    public Field<UInteger> field4() {
        return PRISM_AIRTAGS.CREATED_AT;
    }

    @Override
    public UInteger component1() {
        return getAirtagId();
    }

    @Override
    public String component2() {
        return getAirtag();
    }

    @Override
    public UInteger component3() {
        return getPlayerId();
    }

    @Override
    public UInteger component4() {
        return getCreatedAt();
    }

    @Override
    public UInteger value1() {
        return getAirtagId();
    }

    @Override
    public PrismAirtagsRecord value1(UInteger value) {
        setAirtagId(value);
        return this;
    }

    @Override
    public String value2() {
        return getAirtag();
    }

    @Override
    public PrismAirtagsRecord value2(String value) {
        setAirtag(value);
        return this;
    }

    @Override
    public UInteger value3() {
        return getPlayerId();
    }

    @Override
    public PrismAirtagsRecord value3(UInteger value) {
        setPlayerId(value);
        return this;
    }

    @Override
    public UInteger value4() {
        return getCreatedAt();
    }

    @Override
    public PrismAirtagsRecord value4(UInteger value) {
        setCreatedAt(value);
        return this;
    }

    @Override
    public PrismAirtagsRecord values(UInteger value1, String value2, UInteger value3, UInteger value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismAirtagsRecord.
     */
    public PrismAirtagsRecord() {
        super(PRISM_AIRTAGS);
    }

    /**
     * Create a detached, initialised PrismAirtagsRecord.
     */
    public PrismAirtagsRecord(UInteger airtagId, String airtag, UInteger playerId, UInteger createdAt) {
        super(PRISM_AIRTAGS);
        setAirtagId(airtagId);
        setAirtag(airtag);
        setPlayerId(playerId);
        setCreatedAt(createdAt);
    }
}
