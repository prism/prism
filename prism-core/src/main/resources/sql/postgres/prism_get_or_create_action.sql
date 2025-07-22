CREATE OR REPLACE FUNCTION %prefix%get_or_create_action(actionKey VARCHAR(25))
RETURNS INTEGER AS $$
DECLARE
    actionId INTEGER;
BEGIN
    SELECT action_id INTO actionId FROM %prefix%actions WHERE action = actionKey;
    IF actionId IS NULL THEN
        INSERT INTO %prefix%actions (action) VALUES (actionKey) RETURNING action_id INTO actionId;
    END IF;
    RETURN actionId;
END;
$$ LANGUAGE plpgsql;