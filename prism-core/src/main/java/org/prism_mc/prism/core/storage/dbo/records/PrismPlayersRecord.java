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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_PLAYERS;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismPlayersRecord
    extends UpdatableRecordImpl<PrismPlayersRecord>
    implements Record3<UInteger, String, String>
{

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_players.player_id</code>.
     */
    public PrismPlayersRecord setPlayerId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_players.player_id</code>.
     */
    public UInteger getPlayerId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_players.player</code>.
     */
    public PrismPlayersRecord setPlayer(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_players.player</code>.
     */
    public String getPlayer() {
        return (String) get(1);
    }

    /**
     * Setter for <code>prism_players.player_uuid</code>.
     */
    public PrismPlayersRecord setPlayerUuid(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>prism_players.player_uuid</code>.
     */
    public String getPlayerUuid() {
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
        return PRISM_PLAYERS.PLAYER_ID;
    }

    @Override
    public Field<String> field2() {
        return PRISM_PLAYERS.PLAYER;
    }

    @Override
    public Field<String> field3() {
        return PRISM_PLAYERS.PLAYER_UUID;
    }

    @Override
    public UInteger component1() {
        return getPlayerId();
    }

    @Override
    public String component2() {
        return getPlayer();
    }

    @Override
    public String component3() {
        return getPlayerUuid();
    }

    @Override
    public UInteger value1() {
        return getPlayerId();
    }

    @Override
    public PrismPlayersRecord value1(UInteger value) {
        setPlayerId(value);
        return this;
    }

    @Override
    public String value2() {
        return getPlayer();
    }

    @Override
    public PrismPlayersRecord value2(String value) {
        setPlayer(value);
        return this;
    }

    @Override
    public String value3() {
        return getPlayerUuid();
    }

    @Override
    public PrismPlayersRecord value3(String value) {
        setPlayerUuid(value);
        return this;
    }

    @Override
    public PrismPlayersRecord values(UInteger value1, String value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismPlayersRecord.
     */
    public PrismPlayersRecord() {
        super(PRISM_PLAYERS);
    }

    /**
     * Create a detached, initialised PrismPlayersRecord.
     */
    public PrismPlayersRecord(UInteger playerId, String player, String playerUuid) {
        super(PRISM_PLAYERS);
        setPlayerId(playerId);
        setPlayer(player);
        setPlayerUuid(playerUuid);
    }
}
