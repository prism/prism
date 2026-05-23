CREATE OR REPLACE FUNCTION %prefix%get_or_create_item(materialKey VARCHAR(45), itemData TEXT, p_airtag CHAR(6))
RETURNS INTEGER AS $$
DECLARE
    itemId INTEGER;
    v_airtagId INTEGER;
BEGIN
    SELECT item_id INTO itemId FROM
    %prefix%items WHERE material = materialKey AND data = itemData;

    IF itemId IS NULL THEN
        IF p_airtag IS NOT NULL THEN
            SELECT airtag_id INTO v_airtagId FROM %prefix%airtags WHERE airtag = p_airtag;
        END IF;

        INSERT INTO %prefix%items (material, data, airtag_id)
        VALUES (materialKey, itemData, v_airtagId) RETURNING item_id INTO itemId;
    END IF;

    RETURN itemId;
END;
$$ LANGUAGE plpgsql;
