CREATE PROCEDURE %prefix%get_or_create_world
    (IN `worldName` VARCHAR(255), IN `uuid` CHAR(36), OUT `worldId` INT)
BEGIN
    SELECT world_id INTO `worldId` FROM
    %prefix%worlds WHERE world_uuid = `uuid`;
    IF `worldId` IS NULL THEN
        INSERT INTO %prefix%worlds (`world`, `world_uuid`)
             VALUES (`worldName`, `uuid`);
        SET `worldId` = LAST_INSERT_ID();
    END IF;
END