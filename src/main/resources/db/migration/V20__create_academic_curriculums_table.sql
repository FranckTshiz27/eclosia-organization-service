CREATE TABLE IF NOT EXISTS academic_curriculums (
    id UUID PRIMARY KEY,
    country_id UUID NOT NULL,
    academic_cycle_id UUID NOT NULL,
    academic_level_id UUID NOT NULL,
    academic_section_id UUID,
    academic_option_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_country'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT fk_academic_curriculums_country
                FOREIGN KEY (country_id) REFERENCES countries (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_cycle'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT fk_academic_curriculums_cycle
                FOREIGN KEY (academic_cycle_id) REFERENCES academic_cycles (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_level'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT fk_academic_curriculums_level
                FOREIGN KEY (academic_level_id) REFERENCES academic_levels (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_section'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT fk_academic_curriculums_section
                FOREIGN KEY (academic_section_id) REFERENCES academic_sections (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_curriculums_option'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT fk_academic_curriculums_option
                FOREIGN KEY (academic_option_id) REFERENCES academic_options (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_curriculum'
    ) THEN
        ALTER TABLE academic_curriculums
            ADD CONSTRAINT uk_curriculum UNIQUE (
                country_id,
                academic_cycle_id,
                academic_level_id,
                academic_section_id,
                academic_option_id
            );
    END IF;
END $$;
