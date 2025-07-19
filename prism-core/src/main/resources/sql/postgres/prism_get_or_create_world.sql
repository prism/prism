CREATE OR REPLACE FUNCTION %prefix%get_or_create_world(worldName VARCHAR(255), uuid CHAR(36))
RETURNS INTEGER AS $$
DECLARE
    worldId INTEGER;
BEGIN
    SELECT world_id INTO worldId FROM %prefix%worlds WHERE world_uuid = uuid;

    IF worldId IS NULL THEN
        INSERT INTO %prefix%worlds (world, world_uuid) VALUES (worldName, uuid) RETURNING world_id INTO worldId;
    END IF;

    RETURN worldId;
END;
$$ LANGUAGE plpgsql;