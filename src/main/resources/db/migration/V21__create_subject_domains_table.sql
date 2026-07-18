CREATE TABLE IF NOT EXISTS subject_domains (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subject_domains_country'
    ) THEN
        ALTER TABLE subject_domains
            ADD CONSTRAINT fk_subject_domains_country
                FOREIGN KEY (country_id) REFERENCES countries (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_country_subject_domain_code'
    ) THEN
        ALTER TABLE subject_domains
            ADD CONSTRAINT uk_country_subject_domain_code UNIQUE (country_id, code);
    END IF;
END $$;
