-- Profil personnel sur security_user (hors Teacher métier)
ALTER TABLE security_user
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100);

ALTER TABLE security_user
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

ALTER TABLE security_user
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20);

ALTER TABLE security_user
    ADD COLUMN IF NOT EXISTS birth_date DATE;

ALTER TABLE security_user
    ADD COLUMN IF NOT EXISTS address VARCHAR(500);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_security_user_phone'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT uk_security_user_phone UNIQUE (phone);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS teacher (
    id UUID PRIMARY KEY,
    security_user_id UUID NOT NULL,
    school_id UUID NOT NULL,
    registration_number VARCHAR(100) NOT NULL,
    hiring_date DATE NOT NULL,
    leaving_date DATE,
    qualification VARCHAR(255),
    specialty VARCHAR(255),
    grade VARCHAR(100),
    titular BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    remarks VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_teacher_registration_number'
    ) THEN
        ALTER TABLE teacher
            ADD CONSTRAINT uk_teacher_registration_number UNIQUE (registration_number);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_teacher_security_user'
    ) THEN
        ALTER TABLE teacher
            ADD CONSTRAINT uk_teacher_security_user UNIQUE (security_user_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_teacher_security_user'
    ) THEN
        ALTER TABLE teacher
            ADD CONSTRAINT fk_teacher_security_user
                FOREIGN KEY (security_user_id) REFERENCES security_user (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_teacher_school'
    ) THEN
        ALTER TABLE teacher
            ADD CONSTRAINT fk_teacher_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;
