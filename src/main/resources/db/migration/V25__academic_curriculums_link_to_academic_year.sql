-- AcademicCurriculum is scoped by AcademicYear (national year) instead of Country directly.

ALTER TABLE academic_curriculums
    ADD COLUMN IF NOT EXISTS academic_year_id UUID,
    ADD COLUMN IF NOT EXISTS code VARCHAR(30),
    ADD COLUMN IF NOT EXISTS name VARCHAR(200);

-- Table is expected empty in current environments; keep a safe default if any row exists.
UPDATE academic_curriculums
SET code = COALESCE(code, 'CURR'),
    name = COALESCE(name, 'Curriculum');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM academic_curriculums
        WHERE academic_year_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot migrate academic_curriculums: academic_year_id is required and table still has rows without it. Truncate or backfill before migrating.';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_curriculum'
    ) THEN
        ALTER TABLE academic_curriculums
            DROP CONSTRAINT uk_curriculum;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_country'
    ) THEN
        ALTER TABLE academic_curriculums
            DROP CONSTRAINT fk_academic_curriculums_country;
    END IF;
END $$;

ALTER TABLE academic_curriculums
    DROP COLUMN IF EXISTS country_id;

ALTER TABLE academic_curriculums
    ALTER COLUMN academic_year_id SET NOT NULL,
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN name SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_academic_year'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT fk_academic_curriculums_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_academic_curriculum'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT uk_academic_curriculum UNIQUE (
                academic_year_id,
                academic_cycle_id,
                academic_level_id,
                academic_section_id,
                academic_option_id
            );
    END IF;
END $$;
