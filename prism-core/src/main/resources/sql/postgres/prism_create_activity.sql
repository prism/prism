CREATE OR REPLACE FUNCTION %prefix%create_activity (
    p_timestamp BIGINT,
    p_world VARCHAR(255),
    p_worldUuid CHAR(36),
    p_x INTEGER,
    p_y INTEGER,
    p_z INTEGER,
    p_action VARCHAR(25),
    p_affectedItemMaterial VARCHAR(45),
    p_affectedItemQuantity INTEGER,
    p_affectedItemData TEXT,
    p_affectedBlockNamespace VARCHAR(55),
    p_affectedBlockName VARCHAR(55),
    p_affectedBlockData VARCHAR(255),
    p_affectedBlockTranslationKey VARCHAR(155),
    p_replacedBlockNamespace VARCHAR(55),
    p_replacedBlockName VARCHAR(55),
    p_replacedBlockData VARCHAR(255),
    p_replacedBlockTranslationKey VARCHAR(155),
    p_affectedEntityType VARCHAR(25),
    p_affectedEntityTypeTranslationKey VARCHAR(155),
    p_affectedPlayerName VARCHAR(32),
    p_affectedPlayerUuid CHAR(36),
    p_cause VARCHAR(155),
    p_causePlayerName VARCHAR(32),
    p_causePlayerUuid CHAR(36),
    p_causeEntityType VARCHAR(25),
    p_causeEntityTypeTranslationKey VARCHAR(155),
    p_causeBlockNamespace VARCHAR(55),
    p_causeBlockName VARCHAR(55),
    p_causeBlockData VARCHAR(255),
    p_causeTranslationKey VARCHAR(155),
    p_serializerVersion INTEGER,
    p_serializedData TEXT,
    p_descriptor VARCHAR(256),
    p_metadata VARCHAR(255)
)
RETURNS VOID AS $$
DECLARE
    v_actionId INTEGER;
    v_affectedItemId INTEGER;
    v_affectedBlockId INTEGER;
    v_replacedBlockId INTEGER;
    v_affectedEntityTypeId INTEGER;
    v_affectedPlayerId INTEGER;
    v_causeId INTEGER;
    v_causePlayerId INTEGER;
    v_causeEntityTypeId INTEGER;
    v_causeBlockId INTEGER;
    v_worldId INTEGER;
    v_activityId INTEGER;
BEGIN
    -- Get or create action
    SELECT %prefix%get_or_create_action(p_action) INTO v_actionId;

     -- Get or create affected item
    IF p_affectedItemMaterial IS NOT NULL THEN
        SELECT %prefix%get_or_create_item(p_affectedItemMaterial, p_affectedItemData) INTO v_affectedItemId;
    END IF;

    -- Get or create affected block
    IF p_affectedBlockName IS NOT NULL THEN
        SELECT %prefix%get_or_create_block(p_affectedBlockNamespace, p_affectedBlockName, p_affectedBlockData, p_affectedBlockTranslationKey) INTO v_affectedBlockId;
    END IF;

    -- Get or create replaced block
    IF p_replacedBlockName IS NOT NULL THEN
        SELECT %prefix%get_or_create_block(p_replacedBlockNamespace, p_replacedBlockName, p_replacedBlockData, p_replacedBlockTranslationKey)
        INTO v_replacedBlockId;
    END IF;

    -- Get or create affected entity type
    IF p_affectedEntityType IS NOT NULL THEN
        SELECT %prefix%get_or_create_entity_type(p_affectedEntityType, p_affectedEntityTypeTranslationKey) INTO v_affectedEntityTypeId;
    END IF;

    -- Get or create affected player
    IF p_affectedPlayerUuid IS NOT NULL THEN
        SELECT %prefix%get_or_create_player(p_affectedPlayerName, p_affectedPlayerUuid) INTO v_affectedPlayerId;
    END IF;

    -- Get or create named cause
    IF p_cause IS NOT NULL THEN
        SELECT %prefix%get_or_create_cause(p_cause) INTO v_causeId;
    END IF;

    -- Get or create cause player
    IF p_causePlayerUuid IS NOT NULL THEN
        SELECT %prefix%get_or_create_player(p_causePlayerName, p_causePlayerUuid) INTO v_causePlayerId;
    END IF;

    -- Get or create cause entity type
    IF p_causeEntityType IS NOT NULL THEN
        SELECT %prefix%get_or_create_entity_type(p_causeEntityType, p_causeEntityTypeTranslationKey) INTO v_causeEntityTypeId;
    END IF;

    -- Get or create cause block id
    IF p_causeBlockName IS NOT NULL THEN
        SELECT %prefix%get_or_create_block(p_causeBlockNamespace, p_causeBlockName, p_causeBlockData, p_causeTranslationKey) INTO v_causeBlockId;
    END IF;

    -- Get or create world
    SELECT %prefix%get_or_create_world(p_world, p_worldUuid) INTO v_worldId;

    -- Insert into activities table
    INSERT INTO %prefix%activities (
        "timestamp",
        world_id,
        x,
        y,
        z,
        action_id,
        affected_item_id,
        affected_item_quantity,
        affected_block_id,
        replaced_block_id,
        affected_entity_type_id,
        affected_player_id,
        cause_id,
        cause_player_id,
        cause_entity_type_id,
        cause_block_id,
        descriptor,
        metadata,
        serializer_version,
        serialized_data
    ) VALUES (
        p_timestamp,
        v_worldId,
        p_x,
        p_y,
        p_z,
        v_actionId,
        v_affectedItemId,
        p_affectedItemQuantity,
        v_affectedBlockId,
        v_replacedBlockId,
        v_affectedEntityTypeId,
        v_affectedPlayerId,
        v_causeId,
        v_causePlayerId,
        v_causeEntityTypeId,
        v_causeBlockId,
        p_descriptor,
        p_metadata,
        p_serializerVersion,
        p_serializedData
    )
    RETURNING activity_id INTO v_activityId;
END;
$$ LANGUAGE plpgsql;