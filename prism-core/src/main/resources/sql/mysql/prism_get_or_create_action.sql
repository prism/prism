CREATE PROCEDURE %prefix%get_or_create_action
    (IN `actionKey` VARCHAR(25), OUT `actionId` INT)
BEGIN
    SELECT action_id INTO `actionId` FROM %prefix%actions WHERE action = `actionKey`;
    IF `actionId` IS NULL THEN
        INSERT INTO %prefix%actions (`action`) VALUES (`actionKey`);
        SET `actionId` = LAST_INSERT_ID();
    END IF;
END