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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_CAUSES;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismCausesRecord extends UpdatableRecordImpl<PrismCausesRecord> implements Record2<UInteger, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_causes.cause_id</code>.
     */
    public PrismCausesRecord setCauseId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_causes.cause_id</code>.
     */
    public UInteger getCauseId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_causes.cause</code>.
     */
    public PrismCausesRecord setCause(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_causes.cause</code>.
     */
    public String getCause() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<UInteger, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<UInteger, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return PRISM_CAUSES.CAUSE_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_CAUSES.CAUSE;
    }

    @Override
    public UInteger component1() {
        return getCauseId();
    }

    @Override
    public String component2() {
        return getCause();
    }

    @Override
    public UInteger value1() {
        return getCauseId();
    }

    @Override
    public PrismCausesRecord value1(UInteger value) {
        setCauseId(value);
        return this;
    }

    @Override
    public String value2() {
        return getCause();
    }

    @Override
    public PrismCausesRecord value2(String value) {
        setCause(value);
        return this;
    }

    @Override
    public PrismCausesRecord values(UInteger value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismCausesRecord.
     */
    public PrismCausesRecord() {
        super(PRISM_CAUSES);
    }

    /**
     * Create a detached, initialised PrismCausesRecord.
     */
    public PrismCausesRecord(UInteger causeId, String cause) {
        super(PRISM_CAUSES);
        setCauseId(causeId);
        setCause(cause);
    }
}
