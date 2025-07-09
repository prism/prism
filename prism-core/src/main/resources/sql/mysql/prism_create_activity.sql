CREATE PROCEDURE %prefix%create_activity (
    IN `p_timestamp` INT,
    IN `p_world` VARCHAR(255),
    IN `p_worldUuid` CHAR(36),
    IN `p_x` INT,
    IN `p_y` INT,
    IN `p_z` INT,
    IN `p_action` VARCHAR(25),
    IN `p_affectedItemMaterial` VARCHAR(45),
    IN `p_affectedItemQuantity` SMALLINT,
    IN `p_affectedItemData` TEXT,
    IN `p_affectedBlockNamespace` VARCHAR(55),
    IN `p_affectedBlockName` VARCHAR(55),
    IN `p_affectedBlockData` VARCHAR(255),
    IN `p_affectedBlockTranslationKey` VARCHAR(155),
    IN `p_replacedBlockNamespace` VARCHAR(55),
    IN `p_replacedBlockName` VARCHAR(55),
    IN `p_replacedBlockData` VARCHAR(255),
    IN `p_replacedBlockTranslationKey` VARCHAR(155),
    IN `p_affectedEntityType` VARCHAR(25),
    IN `p_affectedEntityTypeTranslationKey` VARCHAR(155),
    IN `p_affectedPlayerName` VARCHAR(32),
    IN `p_affectedPlayerUuid` CHAR(36),
    IN `p_causeName` VARCHAR(155),
    IN `p_causePlayerName` VARCHAR(32),
    IN `p_causePlayerUuid` CHAR(36),
    IN `p_causeEntityType` VARCHAR(25),
    IN `p_causeEntityTypeTranslationKey` VARCHAR(155),
    IN `p_causeBlockNamespace` VARCHAR(55),
    IN `p_causeBlockName` VARCHAR(55),
    IN `p_causeBlockData` VARCHAR(255),
    IN `p_causeBlockTranslationKey` VARCHAR(155),
    IN `p_serializerVersion` SMALLINT,
    IN `p_serializedData` TEXT,
    IN `p_descriptor` VARCHAR(256),
    IN `p_metadata` VARCHAR(255)
)
BEGIN
    DECLARE v_affectedItemId INT DEFAULT NULL;
    DECLARE v_affectedBlockId INT DEFAULT NULL;
    DECLARE v_replacedBlockId INT DEFAULT NULL;
    DECLARE v_affectedEntityId INT DEFAULT NULL;
    DECLARE v_affectedPlayerId INT DEFAULT NULL;
    DECLARE v_causeId INT DEFAULT NULL;
    DECLARE v_causePlayerId INT DEFAULT NULL;
    DECLARE v_causeEntityTypeId INT DEFAULT NULL;
    DECLARE v_causeBlockId INT DEFAULT NULL;
    DECLARE v_worldId INT DEFAULT NULL;
    DECLARE v_actionId INT DEFAULT NULL;

    -- Create the action
    CALL %prefix%get_or_create_action(`p_action`, v_actionId);

    -- Create the affected item
    IF `p_affectedItemMaterial` IS NOT NULL THEN
        CALL %prefix%get_or_create_item(p_affectedItemMaterial, p_affectedItemData, v_affectedItemId);
    END IF;

    -- Create the affected block
    IF `p_affectedBlockName` IS NOT NULL THEN
        CALL %prefix%get_or_create_block(p_affectedBlockNamespace, p_affectedBlockName, p_affectedBlockData, p_affectedBlockTranslationKey, v_affectedBlockId);
    END IF;

    -- Create the replaced block
    IF `p_replacedBlockName` IS NOT NULL THEN
        CALL %prefix%get_or_create_block(p_replacedBlockNamespace, p_replacedBlockName, p_replacedBlockData, p_replacedBlockTranslationKey, v_replacedBlockId);
    END IF;

    -- Create the affected entity type
    IF `p_affectedEntityType` IS NOT NULL THEN
        CALL %prefix%get_or_create_entity_type(p_affectedEntityType, p_affectedEntityTypeTranslationKey, v_affectedEntityId);
    END IF;

    -- Create the affected player
    IF `p_affectedPlayerUuid` IS NOT NULL THEN
        CALL %prefix%get_or_create_player(p_affectedPlayerName, p_affectedPlayerUuid, v_affectedPlayerId);
    END IF;

    -- Create the named cause
    IF `p_causeName` IS NOT NULL THEN
        CALL %prefix%get_or_create_cause(p_causeName, v_causeId);
    END IF;

    -- Create the player cause
    IF `p_causePlayerUuid` IS NOT NULL THEN
        CALL %prefix%get_or_create_player(p_causePlayerName, p_causePlayerUuid, v_causePlayerId);
    END IF;

    -- Create the entity type cause
    IF `p_causeEntityType` IS NOT NULL THEN
        CALL %prefix%get_or_create_entity_type(p_causeEntityType, p_causeEntityTypeTranslationKey, v_causeEntityTypeId);
    END IF;

    -- Create the block cause
    IF `p_causeBlockName` IS NOT NULL THEN
        CALL %prefix%get_or_create_block(p_causeBlockNamespace, p_causeBlockName, p_causeBlockData, p_causeBlockTranslationKey, v_causeBlockId);
    END IF;

    -- Create the world
    CALL %prefix%get_or_create_world(p_world, p_worldUuid, v_worldId);

    -- Create the activities
    INSERT INTO `%prefix%activities` (
        `timestamp`,
        `world_id`,
        `x`,
        `y`,
        `z`,
        `action_id`,
        `affected_item_id`,
        `affected_item_quantity`,
        `affected_block_id`,
        `replaced_block_id`,
        `affected_entity_type_id`,
        `affected_player_id`,
        `cause_id`,
        `cause_player_id`,
        `cause_entity_type_id`,
        `cause_block_id`,
        `descriptor`,
        `metadata`,
        `serializer_version`,
        `serialized_data`
    ) VALUES (
        `p_timestamp`,
        v_worldId,
        `p_x`,
        `p_y`,
        `p_z`,
        v_actionId,
        v_affectedItemId,
        `p_affectedItemQuantity`,
        v_affectedBlockId,
        v_replacedBlockId,
        v_affectedEntityId,
        v_affectedPlayerId,
        v_causeId,
        v_causePlayerId,
        v_causeEntityTypeId,
        v_causeBlockId,
        `p_descriptor`,
        `p_metadata`,
        `p_serializerVersion`,
        `p_serializedData`
    );
END