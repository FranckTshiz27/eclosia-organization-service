CREATE TABLE IF NOT EXISTS subject_sub_domains (
    id UUID PRIMARY KEY,
    subject_domain_id UUID NOT NULL,
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
        WHERE conname = 'fk_subject_sub_domains_subject_domain'
    ) THEN
        ALTER TABLE subject_sub_domains
            ADD CONSTRAINT fk_subject_sub_domains_subject_domain
                FOREIGN KEY (subject_domain_id) REFERENCES subject_domains (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_subject_sub_domain_code'
    ) THEN
        ALTER TABLE subject_sub_domains
            ADD CONSTRAINT uk_subject_sub_domain_code UNIQUE (subject_domain_id, code);
    END IF;
END $$;
