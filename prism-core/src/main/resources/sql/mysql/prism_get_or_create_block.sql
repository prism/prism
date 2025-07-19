CREATE PROCEDURE %prefix%get_or_create_block
(
    IN `blockNamespace` VARCHAR(55),
    IN `blockName` VARCHAR(55),
    IN `blockData` VARCHAR(255),
    IN `translationKey` VARCHAR(155),
    OUT `blockId` INT
)
BEGIN
    IF blockData IS NOT NULL THEN
        SELECT block_id INTO `blockId` FROM
            %prefix%blocks WHERE ns = `blockNamespace` AND name = `blockName` AND data = `blockData`;
    ELSE
        SELECT block_id INTO `blockId` FROM
            %prefix%blocks WHERE ns = `blockNamespace` AND name = `blockName` AND data IS NULL;
    END IF;

    IF `blockId` IS NULL THEN
        INSERT INTO %prefix%blocks (`ns`, `name`, `data`, `translation_key`)
        VALUES (`blockNamespace`, `blockName`, `blockData`, `translationKey`);

        SET `blockId` = LAST_INSERT_ID();
    END IF;
END