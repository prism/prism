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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_WORLDS;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismWorldsRecord
    extends UpdatableRecordImpl<PrismWorldsRecord>
    implements Record3<UInteger, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_worlds.world_id</code>.
     */
    public PrismWorldsRecord setWorldId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_worlds.world_id</code>.
     */
    public UInteger getWorldId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_worlds.world</code>.
     */
    public PrismWorldsRecord setWorld(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_worlds.world</code>.
     */
    public String getWorld() {
        return (String) get(1);
    }

    /**
     * Setter for <code>prism_worlds.world_uuid</code>.
     */
    public PrismWorldsRecord setWorldUuid(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>prism_worlds.world_uuid</code>.
     */
    public String getWorldUuid() {
        return (String) get(2);
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
    public Row3<UInteger, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<UInteger, String, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return PRISM_WORLDS.WORLD_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_WORLDS.WORLD;
    }

    @Override
    public Field<String> field3() {
        return PRISM_WORLDS.WORLD_UUID;
    }

    @Override
    public UInteger component1() {
        return getWorldId();
    }

    @Override
    public String component2() {
        return getWorld();
    }

    @Override
    public String component3() {
        return getWorldUuid();
    }

    @Override
    public UInteger value1() {
        return getWorldId();
    }

    @Override
    public PrismWorldsRecord value1(UInteger value) {
        setWorldId(value);
        return this;
    }

    @Override
    public String value2() {
        return getWorld();
    }

    @Override
    public PrismWorldsRecord value2(String value) {
        setWorld(value);
        return this;
    }

    @Override
    public String value3() {
        return getWorldUuid();
    }

    @Override
    public PrismWorldsRecord value3(String value) {
        setWorldUuid(value);
        return this;
    }

    @Override
    public PrismWorldsRecord values(UInteger value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismWorldsRecord.
     */
    public PrismWorldsRecord() {
        super(PRISM_WORLDS);
    }

    /**
     * Create a detached, initialised PrismWorldsRecord.
     */
    public PrismWorldsRecord(UInteger worldId, String world, String worldUuid) {
        super(PRISM_WORLDS);
        setWorldId(worldId);
        setWorld(world);
        setWorldUuid(worldUuid);
    }
}
