CREATE TABLE IF NOT EXISTS student_grades (
    id UUID PRIMARY KEY,
    student_enrollment_id UUID NOT NULL,
    academic_period_id UUID NOT NULL,
    academic_curriculum_subject_id UUID NOT NULL,
    score NUMERIC(6, 2) NOT NULL,
    observation VARCHAR(500),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_grades_enrollment'
    ) THEN
        ALTER TABLE student_grades
            ADD CONSTRAINT fk_student_grades_enrollment
                FOREIGN KEY (student_enrollment_id) REFERENCES enrollments (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_grades_academic_period'
    ) THEN
        ALTER TABLE student_grades
            ADD CONSTRAINT fk_student_grades_academic_period
                FOREIGN KEY (academic_period_id) REFERENCES academic_periods (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_grades_academic_curriculum_subject'
    ) THEN
        ALTER TABLE student_grades
            ADD CONSTRAINT fk_student_grades_academic_curriculum_subject
                FOREIGN KEY (academic_curriculum_subject_id) REFERENCES academic_curriculum_subjects (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_period_subject'
    ) THEN
        ALTER TABLE student_grades
            ADD CONSTRAINT uk_student_period_subject UNIQUE (
                student_enrollment_id,
                academic_period_id,
                academic_curriculum_subject_id
            );
    END IF;
END $$;
