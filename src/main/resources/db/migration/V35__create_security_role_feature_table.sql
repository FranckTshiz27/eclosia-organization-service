CREATE TABLE IF NOT EXISTS security_role_feature (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL,
    feature_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_role_feature_role'
    ) THEN
        ALTER TABLE security_role_feature
            ADD CONSTRAINT fk_role_feature_role
                FOREIGN KEY (role_id) REFERENCES security_role (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_role_feature_feature'
    ) THEN
        ALTER TABLE security_role_feature
            ADD CONSTRAINT fk_role_feature_feature
                FOREIGN KEY (feature_id) REFERENCES security_feature (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_role_feature'
    ) THEN
        ALTER TABLE security_role_feature
            ADD CONSTRAINT uk_role_feature UNIQUE (role_id, feature_id);
    END IF;
END $$;
