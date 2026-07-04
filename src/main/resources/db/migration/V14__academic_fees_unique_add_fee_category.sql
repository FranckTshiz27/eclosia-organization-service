ALTER TABLE academic_fees DROP CONSTRAINT IF EXISTS uk_academic_fee;

ALTER TABLE academic_fees
    ADD CONSTRAINT uk_academic_fee UNIQUE (
        school_id,
        academic_year_id,
        fee_category_id,
        academic_cycle_id,
        academic_level_id,
        academic_section_id,
        academic_option_id,
        student_category_id,
        payment_installment_id,
        code
    );
