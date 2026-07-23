-- Titulaire de classe (historique par année scolaire)
CREATE TABLE IF NOT EXISTS teacher_class_assignment (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    school_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    classroom_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tca_teacher'
    ) THEN
        ALTER TABLE teacher_class_assignment
            ADD CONSTRAINT fk_tca_teacher
                FOREIGN KEY (teacher_id) REFERENCES teacher (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tca_school'
    ) THEN
        ALTER TABLE teacher_class_assignment
            ADD CONSTRAINT fk_tca_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tca_academic_year'
    ) THEN
        ALTER TABLE teacher_class_assignment
            ADD CONSTRAINT fk_tca_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tca_classroom'
    ) THEN
        ALTER TABLE teacher_class_assignment
            ADD CONSTRAINT fk_tca_classroom
                FOREIGN KEY (classroom_id) REFERENCES classrooms (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_active_classroom_titular_per_year
    ON teacher_class_assignment (classroom_id, academic_year_id)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_tca_school_year
    ON teacher_class_assignment (school_id, academic_year_id);

CREATE INDEX IF NOT EXISTS idx_tca_teacher_year
    ON teacher_class_assignment (teacher_id, academic_year_id);

-- Affectation matière / cours (historique par année scolaire)
CREATE TABLE IF NOT EXISTS teacher_course_assignment (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL,
    school_id UUID NOT NULL,
    academic_year_id UUID NOT NULL,
    classroom_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    weekly_hours NUMERIC(5, 2),
    coefficient NUMERIC(5, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tcoa_teacher'
    ) THEN
        ALTER TABLE teacher_course_assignment
            ADD CONSTRAINT fk_tcoa_teacher
                FOREIGN KEY (teacher_id) REFERENCES teacher (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tcoa_school'
    ) THEN
        ALTER TABLE teacher_course_assignment
            ADD CONSTRAINT fk_tcoa_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tcoa_academic_year'
    ) THEN
        ALTER TABLE teacher_course_assignment
            ADD CONSTRAINT fk_tcoa_academic_year
                FOREIGN KEY (academic_year_id) REFERENCES academic_years (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tcoa_classroom'
    ) THEN
        ALTER TABLE teacher_course_assignment
            ADD CONSTRAINT fk_tcoa_classroom
                FOREIGN KEY (classroom_id) REFERENCES classrooms (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tcoa_subject'
    ) THEN
        ALTER TABLE teacher_course_assignment
            ADD CONSTRAINT fk_tcoa_subject
                FOREIGN KEY (subject_id) REFERENCES subjects (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_active_teacher_course
    ON teacher_course_assignment (teacher_id, classroom_id, subject_id, academic_year_id)
    WHERE active = TRUE;

CREATE INDEX IF NOT EXISTS idx_tcoa_school_year
    ON teacher_course_assignment (school_id, academic_year_id);

CREATE INDEX IF NOT EXISTS idx_tcoa_classroom_year
    ON teacher_course_assignment (classroom_id, academic_year_id);

CREATE INDEX IF NOT EXISTS idx_tcoa_subject_school_year
    ON teacher_course_assignment (subject_id, school_id, academic_year_id);

CREATE INDEX IF NOT EXISTS idx_tcoa_teacher_year
    ON teacher_course_assignment (teacher_id, academic_year_id);
