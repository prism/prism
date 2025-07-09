CREATE PROCEDURE %prefix%get_or_create_cause
    (IN `causeStr` VARCHAR(155), OUT `causeId` INT)
BEGIN
    SELECT cause_id INTO `causeId` FROM %prefix%causes WHERE cause = `causeStr`;

    IF `causeId` IS NULL THEN
        INSERT INTO %prefix%causes (`cause`) VALUES (`causeStr`);
        SET `causeId` = LAST_INSERT_ID();
    END IF;
END