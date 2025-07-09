CREATE OR REPLACE FUNCTION %prefix%get_or_create_cause(causeStr VARCHAR(155))
RETURNS INTEGER AS $$
DECLARE
    causeId INTEGER;
BEGIN
    SELECT cause_id INTO causeId FROM %prefix%causes WHERE cause = causeStr;

    IF causeId IS NULL THEN
        INSERT INTO %prefix%causes (cause) VALUES (causeStr) RETURNING cause_id INTO causeId;
    END IF;

    RETURN causeId;
END;
$$ LANGUAGE plpgsql;