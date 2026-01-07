CREATE PROCEDURE %prefix%get_or_create_item
(IN `materialKey` VARCHAR(45), IN `itemData` TEXT, OUT `itemId` INT)
BEGIN
    SELECT item_id INTO `itemId` FROM
        %prefix%items WHERE material = `materialKey` AND data = `itemData`;

    IF `itemId` IS NULL THEN
        INSERT INTO %prefix%items (`material`, `data`) VALUES (`materialKey`, `itemData`);

        SET `itemId` = LAST_INSERT_ID();
    END IF;
END