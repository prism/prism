CREATE OR REPLACE FUNCTION %prefix%get_or_create_world(worldName VARCHAR(255), uuid CHAR(36))
RETURNS INTEGER AS $$
DECLARE
    worldId INTEGER;
BEGIN
    SELECT world_id INTO worldId FROM %prefix%worlds WHERE world = worldName;

    IF worldId IS NULL THEN
        INSERT INTO %prefix%worlds (world, world_uuid) VALUES (worldName, uuid) RETURNING world_id INTO worldId;
    ELSE
        UPDATE %prefix%worlds SET world_uuid = uuid WHERE world_id = worldId;
    END IF;

    RETURN worldId;
END;
$$ LANGUAGE plpgsql;
