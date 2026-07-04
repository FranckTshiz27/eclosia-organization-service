ALTER TABLE academic_fees
    ADD COLUMN IF NOT EXISTS student_category_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_student_category'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_student_category
                FOREIGN KEY (student_category_id) REFERENCES student_categories (id);
    END IF;
END $$;

ALTER TABLE academic_fees DROP CONSTRAINT IF EXISTS uk_academic_fee;

ALTER TABLE academic_fees
    ADD CONSTRAINT uk_academic_fee UNIQUE (
        school_id,
        academic_year_id,
        academic_cycle_id,
        academic_level_id,
        academic_section_id,
        academic_option_id,
        student_category_id,
        payment_installment_id,
        code
    );
