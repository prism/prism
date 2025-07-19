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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIONS;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismActionsRecord extends UpdatableRecordImpl<PrismActionsRecord> implements Record2<UInteger, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_actions.action_id</code>.
     */
    public PrismActionsRecord setActionId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_actions.action_id</code>.
     */
    public UInteger getActionId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_actions.action</code>.
     */
    public PrismActionsRecord setAction(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_actions.action</code>.
     */
    public String getAction() {
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
    // Record2 type implementation
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
        return PRISM_ACTIONS.ACTION_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_ACTIONS.ACTION;
    }

    @Override
    public UInteger component1() {
        return getActionId();
    }

    @Override
    public String component2() {
        return getAction();
    }

    @Override
    public UInteger value1() {
        return getActionId();
    }

    @Override
    public PrismActionsRecord value1(UInteger value) {
        setActionId(value);
        return this;
    }

    @Override
    public String value2() {
        return getAction();
    }

    @Override
    public PrismActionsRecord value2(String value) {
        setAction(value);
        return this;
    }

    @Override
    public PrismActionsRecord values(UInteger value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismActionsRecord.
     */
    public PrismActionsRecord() {
        super(PRISM_ACTIONS);
    }

    /**
     * Create a detached, initialised PrismActionsRecord.
     */
    public PrismActionsRecord(UInteger actionId, String action) {
        super(PRISM_ACTIONS);
        setActionId(actionId);
        setAction(action);
    }
}
