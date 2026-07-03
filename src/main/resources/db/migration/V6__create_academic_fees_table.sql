CREATE TABLE IF NOT EXISTS academic_fees (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    amount NUMERIC(18, 2) NOT NULL,
    payable_by_installment BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    comment VARCHAR(500),
    school_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    fee_category_id UUID NOT NULL,
    academic_cycle_id UUID NOT NULL,
    academic_level_id UUID NOT NULL,
    academic_section_id UUID,
    academic_option_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_school'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_academic_year'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_fee_category'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_fee_category
                FOREIGN KEY (fee_category_id) REFERENCES fee_categories (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_academic_cycle'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_academic_cycle
                FOREIGN KEY (academic_cycle_id) REFERENCES academic_cycles (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_academic_level'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_academic_level
                FOREIGN KEY (academic_level_id) REFERENCES academic_levels (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_academic_section'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_academic_section
                FOREIGN KEY (academic_section_id) REFERENCES academic_sections (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_academic_option'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_academic_option
                FOREIGN KEY (academic_option_id) REFERENCES academic_options (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_academic_fee'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT uk_academic_fee UNIQUE (
                school_id,
                academic_year_id,
                academic_cycle_id,
                academic_level_id,
                academic_section_id,
                academic_option_id,
                code
            );
    END IF;
END $$;
