CREATE OR REPLACE FUNCTION %prefix%get_or_create_action(actionkey VARCHAR(25))
RETURNS INTEGER AS $$
DECLARE
    actionid INTEGER;
BEGIN
    SELECT action_id INTO actionid FROM %prefix%actions WHERE action = actionkey;
    IF actionid IS NULL THEN
        INSERT INTO %prefix%actions (action) VALUES (actionkey) RETURNING action_id INTO actionid;
    END IF;
    RETURN actionid;
END;
$$ LANGUAGE plpgsql;