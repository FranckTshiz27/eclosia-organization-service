CREATE TABLE IF NOT EXISTS features (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL,
    code VARCHAR(100) NOT NULL,
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
        ALTER TABLE features
            ADD CONSTRAINT fk_feature_module
                FOREIGN KEY (module_id) REFERENCES security_modules (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_feature_code'
    ) THEN
        ALTER TABLE features
            ADD CONSTRAINT uk_feature_code UNIQUE (code);
    END IF;
END $$;
