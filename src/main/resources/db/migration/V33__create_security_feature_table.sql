DROP TABLE IF EXISTS features CASCADE;

CREATE TABLE IF NOT EXISTS security_feature (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_feature BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_feature_module'
    ) THEN
        ALTER TABLE security_feature
            ADD CONSTRAINT fk_feature_module
                FOREIGN KEY (module_id) REFERENCES security_modules (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_feature_module_action'
    ) THEN
        ALTER TABLE security_feature
            ADD CONSTRAINT uk_feature_module_action
                UNIQUE (module_id, action);
    END IF;
END $$;
