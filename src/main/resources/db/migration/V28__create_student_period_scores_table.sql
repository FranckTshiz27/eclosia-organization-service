CREATE TABLE IF NOT EXISTS student_period_scores (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL,
    academic_period_id UUID NOT NULL,
    academic_curriculum_subject_id UUID NOT NULL,
    score NUMERIC(8, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_student_period_scores_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_student_period_scores_period
        FOREIGN KEY (academic_period_id) REFERENCES academic_periods (id),
    CONSTRAINT fk_student_period_scores_curriculum_subject
        FOREIGN KEY (academic_curriculum_subject_id) REFERENCES academic_curriculum_subjects (id),
    CONSTRAINT uk_enrollment_period_subject
        UNIQUE (enrollment_id, academic_period_id, academic_curriculum_subject_id)
);
