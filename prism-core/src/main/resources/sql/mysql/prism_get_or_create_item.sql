CREATE PROCEDURE %prefix%get_or_create_item
(IN `materialKey` VARCHAR(45), IN `itemData` TEXT, IN `p_airtag` CHAR(6), OUT `itemId` INT)
BEGIN
    DECLARE v_airtagId INT DEFAULT NULL;

    SELECT item_id INTO `itemId` FROM
        %prefix%items WHERE material = `materialKey` AND data = `itemData`;

    IF `itemId` IS NULL THEN
        IF `p_airtag` IS NOT NULL THEN
            SELECT airtag_id INTO v_airtagId FROM %prefix%airtags WHERE airtag = `p_airtag`;
        END IF;

        INSERT INTO %prefix%items (`material`, `data`, `airtag_id`)
        VALUES (`materialKey`, `itemData`, v_airtagId);

        SET `itemId` = LAST_INSERT_ID();
    END IF;
END
