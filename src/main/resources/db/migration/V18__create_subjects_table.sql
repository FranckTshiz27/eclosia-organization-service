CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    abbreviation VARCHAR(20),
    parent_subject_id UUID,
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
        WHERE conname = 'fk_subjects_country'
    ) THEN
        ALTER TABLE subjects
            ADD CONSTRAINT fk_subjects_country
                FOREIGN KEY (country_id) REFERENCES countries (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subjects_parent_subject'
    ) THEN
        ALTER TABLE subjects
            ADD CONSTRAINT fk_subjects_parent_subject
                FOREIGN KEY (parent_subject_id) REFERENCES subjects (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_country_subject_code'
    ) THEN
        ALTER TABLE subjects
            ADD CONSTRAINT uk_country_subject_code UNIQUE (country_id, code);
    END IF;
END $$;
