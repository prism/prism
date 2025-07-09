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

import static org.prism_mc.prism.core.storage.adapters.sql.AbstractSqlStorageAdapter.PRISM_ACTIVITIES;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record22;
import org.jooq.Row22;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;

@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PrismActivitiesRecord
    extends UpdatableRecordImpl<PrismActivitiesRecord>
    implements
        Record22<
            UInteger,
            UInteger,
            UByte,
            Integer,
            Integer,
            Integer,
            UByte,
            UShort,
            UShort,
            UInteger,
            UInteger,
            UShort,
            UInteger,
            UInteger,
            UInteger,
            UShort,
            UInteger,
            String,
            String,
            UShort,
            String,
            Boolean
        > {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>prism_activities.activity_id</code>.
     */
    public PrismActivitiesRecord setActivityId(UInteger value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.activity_id</code>.
     */
    public UInteger getActivityId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>prism_activities.timestamp</code>.
     */
    public PrismActivitiesRecord setTimestamp(UInteger value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.timestamp</code>.
     */
    public UInteger getTimestamp() {
        return (UInteger) get(1);
    }

    /**
     * Setter for <code>prism_activities.world_id</code>.
     */
    public PrismActivitiesRecord setWorldId(UByte value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.world_id</code>.
     */
    public UByte getWorldId() {
        return (UByte) get(2);
    }

    /**
     * Setter for <code>prism_activities.x</code>.
     */
    public PrismActivitiesRecord setX(Integer value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.x</code>.
     */
    public Integer getX() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>prism_activities.y</code>.
     */
    public PrismActivitiesRecord setY(Integer value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.y</code>.
     */
    public Integer getY() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>prism_activities.z</code>.
     */
    public PrismActivitiesRecord setZ(Integer value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.z</code>.
     */
    public Integer getZ() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>prism_activities.action_id</code>.
     */
    public PrismActivitiesRecord setActionId(UByte value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.action_id</code>.
     */
    public UByte getActionId() {
        return (UByte) get(6);
    }

    /**
     * Setter for <code>prism_activities.item_id</code>.
     */
    public PrismActivitiesRecord setItemId(UShort value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.item_id</code>.
     */
    public UShort getItemId() {
        return (UShort) get(7);
    }

    /**
     * Setter for <code>prism_activities.item_quantity</code>.
     */
    public PrismActivitiesRecord setItemQuantity(UShort value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.item_quantity</code>.
     */
    public UShort getItemQuantity() {
        return (UShort) get(8);
    }

    /**
     * Setter for <code>prism_activities.block_id</code>.
     */
    public PrismActivitiesRecord setBlockId(UInteger value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.block_id</code>.
     */
    public UInteger getBlockId() {
        return (UInteger) get(9);
    }

    /**
     * Setter for <code>prism_activities.replaced_block_id</code>.
     */
    public PrismActivitiesRecord setReplacedBlockId(UInteger value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.replaced_block_id</code>.
     */
    public UInteger getReplacedBlockId() {
        return (UInteger) get(10);
    }

    /**
     * Setter for <code>prism_activities.entity_type_id</code>.
     */
    public PrismActivitiesRecord setEntityTypeId(UShort value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.entity_type_id</code>.
     */
    public UShort getEntityTypeId() {
        return (UShort) get(11);
    }

    /**
     * Setter for <code>prism_activities.affected_player_id</code>.
     */
    public PrismActivitiesRecord setAffectedPlayerId(UInteger value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.affected_player_id</code>.
     */
    public UInteger getAffectedPlayerId() {
        return (UInteger) get(12);
    }

    /**
     * Setter for <code>prism_activities.cause_id</code>.
     */
    public PrismActivitiesRecord setCauseId(UInteger value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.cause_id</code>.
     */
    public UInteger getCauseId() {
        return (UInteger) get(13);
    }

    /**
     * Setter for <code>prism_activities.cause_player_id</code>.
     */
    public PrismActivitiesRecord setCausePlayerId(UInteger value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.cause_player_id</code>.
     */
    public UInteger getCausePlayerId() {
        return (UInteger) get(14);
    }

    /**
     * Setter for <code>prism_activities.cause_entity_type_id</code>.
     */
    public PrismActivitiesRecord setCauseEntityTypeId(UShort value) {
        set(15, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.cause_entity_type_id</code>.
     */
    public UShort getCauseEntityTypeId() {
        return (UShort) get(15);
    }

    /**
     * Setter for <code>prism_activities.cause_block_id</code>.
     */
    public PrismActivitiesRecord setCauseBlockId(UInteger value) {
        set(16, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.cause_block_id</code>.
     */
    public UInteger getCauseBlockId() {
        return (UInteger) get(16);
    }

    /**
     * Setter for <code>prism_activities.descriptor</code>.
     */
    public PrismActivitiesRecord setDescriptor(String value) {
        set(17, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.descriptor</code>.
     */
    public String getDescriptor() {
        return (String) get(17);
    }

    /**
     * Setter for <code>prism_activities.metadata</code>.
     */
    public PrismActivitiesRecord setMetadata(String value) {
        set(18, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.metadata</code>.
     */
    public String getMetadata() {
        return (String) get(18);
    }

    /**
     * Setter for <code>prism_activities.serializer_version</code>.
     */
    public PrismActivitiesRecord setSerializerVersion(UShort value) {
        set(19, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.serializer_version</code>.
     */
    public UShort getSerializerVersion() {
        return (UShort) get(19);
    }

    /**
     * Setter for <code>prism_activities.serialized_data</code>.
     */
    public PrismActivitiesRecord setSerializedData(String value) {
        set(20, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.serialized_data</code>.
     */
    public String getSerializedData() {
        return (String) get(20);
    }

    /**
     * Setter for <code>prism_activities.reversed</code>.
     */
    public PrismActivitiesRecord setReversed(Boolean value) {
        set(21, value);
        return this;
    }

    /**
     * Getter for <code>prism_activities.reversed</code>.
     */
    public Boolean getReversed() {
        return (Boolean) get(21);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record13 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row22<
        UInteger,
        UInteger,
        UByte,
        Integer,
        Integer,
        Integer,
        UByte,
        UShort,
        UShort,
        UInteger,
        UInteger,
        UShort,
        UInteger,
        UInteger,
        UInteger,
        UShort,
        UInteger,
        String,
        String,
        UShort,
        String,
        Boolean
    > fieldsRow() {
        return (Row22) super.fieldsRow();
    }

    @Override
    public Row22<
        UInteger,
        UInteger,
        UByte,
        Integer,
        Integer,
        Integer,
        UByte,
        UShort,
        UShort,
        UInteger,
        UInteger,
        UShort,
        UInteger,
        UInteger,
        UInteger,
        UShort,
        UInteger,
        String,
        String,
        UShort,
        String,
        Boolean
    > valuesRow() {
        return (Row22) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return PRISM_ACTIVITIES.ACTIVITY_ID;
    }

    @Override
    public Field<UInteger> field2() {
        return PRISM_ACTIVITIES.TIMESTAMP;
    }

    @Override
    public Field<UByte> field3() {
        return PRISM_ACTIVITIES.WORLD_ID;
    }

    @Override
    public Field<Integer> field4() {
        return PRISM_ACTIVITIES.X;
    }

    @Override
    public Field<Integer> field5() {
        return PRISM_ACTIVITIES.Y;
    }

    @Override
    public Field<Integer> field6() {
        return PRISM_ACTIVITIES.Z;
    }

    @Override
    public Field<UByte> field7() {
        return PRISM_ACTIVITIES.ACTION_ID;
    }

    @Override
    public Field<UShort> field8() {
        return PRISM_ACTIVITIES.AFFECTED_ITEM_ID;
    }

    @Override
    public Field<UShort> field9() {
        return PRISM_ACTIVITIES.AFFECTED_ITEM_QUANTITY;
    }

    @Override
    public Field<UInteger> field10() {
        return PRISM_ACTIVITIES.AFFECTED_BLOCK_ID;
    }

    @Override
    public Field<UInteger> field11() {
        return PRISM_ACTIVITIES.REPLACED_BLOCK_ID;
    }

    @Override
    public Field<UShort> field12() {
        return PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID;
    }

    @Override
    public Field<UInteger> field13() {
        return PRISM_ACTIVITIES.AFFECTED_PLAYER_ID;
    }

    @Override
    public Field<UInteger> field14() {
        return PRISM_ACTIVITIES.CAUSE_ID;
    }

    @Override
    public Field<UInteger> field15() {
        return PRISM_ACTIVITIES.CAUSE_PLAYER_ID;
    }

    @Override
    public Field<UShort> field16() {
        return PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID;
    }

    @Override
    public Field<UInteger> field17() {
        return PRISM_ACTIVITIES.CAUSE_BLOCK_ID;
    }

    @Override
    public Field<String> field18() {
        return PRISM_ACTIVITIES.DESCRIPTOR;
    }

    @Override
    public Field<String> field19() {
        return PRISM_ACTIVITIES.METADATA;
    }

    @Override
    public Field<UShort> field20() {
        return PRISM_ACTIVITIES.SERIALIZER_VERSION;
    }

    @Override
    public Field<String> field21() {
        return PRISM_ACTIVITIES.SERIALIZED_DATA;
    }

    @Override
    public Field<Boolean> field22() {
        return PRISM_ACTIVITIES.REVERSED;
    }

    @Override
    public UInteger component1() {
        return getActivityId();
    }

    @Override
    public UInteger component2() {
        return getTimestamp();
    }

    @Override
    public UByte component3() {
        return getWorldId();
    }

    @Override
    public Integer component4() {
        return getX();
    }

    @Override
    public Integer component5() {
        return getY();
    }

    @Override
    public Integer component6() {
        return getZ();
    }

    @Override
    public UByte component7() {
        return getActionId();
    }

    @Override
    public UShort component8() {
        return getItemId();
    }

    @Override
    public UShort component9() {
        return getItemQuantity();
    }

    @Override
    public UInteger component10() {
        return getBlockId();
    }

    @Override
    public UInteger component11() {
        return getReplacedBlockId();
    }

    @Override
    public UShort component12() {
        return getEntityTypeId();
    }

    @Override
    public UInteger component13() {
        return getAffectedPlayerId();
    }

    @Override
    public UInteger component14() {
        return getCauseId();
    }

    @Override
    public UInteger component15() {
        return getCausePlayerId();
    }

    @Override
    public UShort component16() {
        return getCauseEntityTypeId();
    }

    @Override
    public UInteger component17() {
        return getCauseBlockId();
    }

    @Override
    public String component18() {
        return getDescriptor();
    }

    @Override
    public String component19() {
        return getMetadata();
    }

    @Override
    public UShort component20() {
        return getSerializerVersion();
    }

    @Override
    public String component21() {
        return getSerializedData();
    }

    @Override
    public Boolean component22() {
        return getReversed();
    }

    @Override
    public UInteger value1() {
        return getActivityId();
    }

    @Override
    public PrismActivitiesRecord value1(UInteger value) {
        setActivityId(value);
        return this;
    }

    @Override
    public UInteger value2() {
        return getTimestamp();
    }

    @Override
    public PrismActivitiesRecord value2(UInteger value) {
        setTimestamp(value);
        return this;
    }

    @Override
    public UByte value3() {
        return getWorldId();
    }

    @Override
    public PrismActivitiesRecord value3(UByte value) {
        setWorldId(value);
        return this;
    }

    @Override
    public Integer value4() {
        return getX();
    }

    @Override
    public PrismActivitiesRecord value4(Integer value) {
        setX(value);
        return this;
    }

    @Override
    public Integer value5() {
        return getY();
    }

    @Override
    public PrismActivitiesRecord value5(Integer value) {
        setY(value);
        return this;
    }

    @Override
    public Integer value6() {
        return getZ();
    }

    @Override
    public PrismActivitiesRecord value6(Integer value) {
        setZ(value);
        return this;
    }

    @Override
    public UByte value7() {
        return getActionId();
    }

    @Override
    public PrismActivitiesRecord value7(UByte value) {
        setActionId(value);
        return this;
    }

    @Override
    public UShort value8() {
        return getItemId();
    }

    @Override
    public PrismActivitiesRecord value8(UShort value) {
        setItemId(value);
        return this;
    }

    @Override
    public UShort value9() {
        return getItemQuantity();
    }

    @Override
    public PrismActivitiesRecord value9(UShort value) {
        setItemQuantity(value);
        return this;
    }

    @Override
    public UInteger value10() {
        return getBlockId();
    }

    @Override
    public PrismActivitiesRecord value10(UInteger value) {
        setBlockId(value);
        return this;
    }

    @Override
    public UInteger value11() {
        return getReplacedBlockId();
    }

    @Override
    public PrismActivitiesRecord value11(UInteger value) {
        setReplacedBlockId(value);
        return this;
    }

    @Override
    public UShort value12() {
        return getEntityTypeId();
    }

    @Override
    public PrismActivitiesRecord value12(UShort value) {
        setEntityTypeId(value);
        return this;
    }

    @Override
    public UInteger value13() {
        return getAffectedPlayerId();
    }

    @Override
    public PrismActivitiesRecord value13(UInteger value) {
        setAffectedPlayerId(value);
        return this;
    }

    @Override
    public UInteger value14() {
        return getCauseId();
    }

    @Override
    public PrismActivitiesRecord value14(UInteger value) {
        setCauseId(value);
        return this;
    }

    @Override
    public UInteger value15() {
        return getCausePlayerId();
    }

    @Override
    public PrismActivitiesRecord value15(UInteger value) {
        setCausePlayerId(value);
        return this;
    }

    @Override
    public UShort value16() {
        return getCauseEntityTypeId();
    }

    @Override
    public PrismActivitiesRecord value16(UShort value) {
        setCauseEntityTypeId(value);
        return this;
    }

    @Override
    public UInteger value17() {
        return getCauseBlockId();
    }

    @Override
    public PrismActivitiesRecord value17(UInteger value) {
        setCauseBlockId(value);
        return this;
    }

    @Override
    public String value18() {
        return getDescriptor();
    }

    @Override
    public PrismActivitiesRecord value18(String value) {
        setDescriptor(value);
        return this;
    }

    @Override
    public String value19() {
        return getMetadata();
    }

    @Override
    public PrismActivitiesRecord value19(String value) {
        setMetadata(value);
        return this;
    }

    @Override
    public UShort value20() {
        return getSerializerVersion();
    }

    @Override
    public PrismActivitiesRecord value20(UShort value) {
        setSerializerVersion(value);
        return this;
    }

    @Override
    public String value21() {
        return getSerializedData();
    }

    @Override
    public PrismActivitiesRecord value21(String value) {
        setSerializedData(value);
        return this;
    }

    @Override
    public Boolean value22() {
        return getReversed();
    }

    @Override
    public PrismActivitiesRecord value22(Boolean value) {
        setReversed(value);
        return this;
    }

    @Override
    public PrismActivitiesRecord values(
        UInteger value1,
        UInteger value2,
        UByte value3,
        Integer value4,
        Integer value5,
        Integer value6,
        UByte value7,
        UShort value8,
        UShort value9,
        UInteger value10,
        UInteger value11,
        UShort value12,
        UInteger value13,
        UInteger value14,
        UInteger value15,
        UShort value16,
        UInteger value17,
        String value18,
        String value19,
        UShort value20,
        String value21,
        Boolean value22
    ) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        value18(value18);
        value19(value19);
        value20(value20);
        value21(value21);
        value22(value22);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached PrismActivitiesRecord.
     */
    public PrismActivitiesRecord() {
        super(PRISM_ACTIVITIES);
    }

    /**
     * Create a detached, initialised PrismActivitiesRecord.
     */
    public PrismActivitiesRecord(
        UInteger activityId,
        UInteger timestamp,
        UByte worldId,
        Integer x,
        Integer y,
        Integer z,
        UByte actionId,
        UShort itemId,
        UShort itemQuantity,
        UInteger blockId,
        UInteger replacedBlockId,
        UShort entityTypeId,
        UInteger affectedPlayerId,
        UInteger causeId,
        UInteger causePlayerId,
        UShort causeEntityTypeId,
        UInteger causeBlockId,
        String descriptor,
        String metadata,
        UShort serializerVersion,
        String serializedData,
        Boolean reversed
    ) {
        super(PRISM_ACTIVITIES);
        setActivityId(activityId);
        setTimestamp(timestamp);
        setWorldId(worldId);
        setX(x);
        setY(y);
        setZ(z);
        setActionId(actionId);
        setItemId(itemId);
        setItemQuantity(itemQuantity);
        setBlockId(blockId);
        setReplacedBlockId(replacedBlockId);
        setEntityTypeId(entityTypeId);
        setAffectedPlayerId(affectedPlayerId);
        setCauseId(causeId);
        setCausePlayerId(causePlayerId);
        setCauseEntityTypeId(causeEntityTypeId);
        setCauseBlockId(causeBlockId);
        setDescriptor(descriptor);
        setMetadata(metadata);
        setSerializerVersion(serializerVersion);
        setSerializedData(serializedData);
        setReversed(reversed);
    }
}
