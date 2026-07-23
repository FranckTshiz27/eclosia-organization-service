DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'features' AND column_name = 'resource'
    ) THEN
        ALTER TABLE features ADD COLUMN resource VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'features' AND column_name = 'action'
    ) THEN
        ALTER TABLE features ADD COLUMN action VARCHAR(50);
    END IF;
END $$;

UPDATE features
SET resource = 'STUDENT'
WHERE resource IS NULL;

UPDATE features
SET action = 'VIEW'
WHERE action IS NULL;

ALTER TABLE features
    ALTER COLUMN resource SET NOT NULL;

ALTER TABLE features
    ALTER COLUMN action SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_feature_code'
    ) THEN
        ALTER TABLE features DROP CONSTRAINT uk_feature_code;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_feature_module_resource_action'
    ) THEN
        ALTER TABLE features
            ADD CONSTRAINT uk_feature_module_resource_action
                UNIQUE (module_id, resource, action);
    END IF;
END $$;
