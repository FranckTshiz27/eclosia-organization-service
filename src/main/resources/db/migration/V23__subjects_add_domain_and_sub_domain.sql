ALTER TABLE subjects
    ADD COLUMN IF NOT EXISTS subject_domain_id UUID,
    ADD COLUMN IF NOT EXISTS subject_sub_domain_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subjects_parent_subject'
    ) THEN
        ALTER TABLE subjects
            DROP CONSTRAINT fk_subjects_parent_subject;
    END IF;
END $$;

ALTER TABLE subjects
    DROP COLUMN IF EXISTS parent_subject_id;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subjects_subject_domain'
    ) THEN
        ALTER TABLE subjects
            ADD CONSTRAINT fk_subjects_subject_domain
                FOREIGN KEY (subject_domain_id) REFERENCES subject_domains (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subjects_subject_sub_domain'
    ) THEN
        ALTER TABLE subjects
            ADD CONSTRAINT fk_subjects_subject_sub_domain
                FOREIGN KEY (subject_sub_domain_id) REFERENCES subject_sub_domains (id);
    END IF;
END $$;
