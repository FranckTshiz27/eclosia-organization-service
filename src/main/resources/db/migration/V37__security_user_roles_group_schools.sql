-- Add group scope on security_user
ALTER TABLE security_user
    ADD COLUMN IF NOT EXISTS group_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_group'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT fk_security_user_group
                FOREIGN KEY (group_id) REFERENCES groups (id);
    END IF;
END $$;

-- Join table: user <-> roles
CREATE TABLE IF NOT EXISTS security_user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_role_user'
    ) THEN
        ALTER TABLE security_user_role
            ADD CONSTRAINT fk_security_user_role_user
                FOREIGN KEY (user_id) REFERENCES security_user (id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_role_role'
    ) THEN
        ALTER TABLE security_user_role
            ADD CONSTRAINT fk_security_user_role_role
                FOREIGN KEY (role_id) REFERENCES security_role (id);
    END IF;
END $$;

-- Join table: user <-> schools
CREATE TABLE IF NOT EXISTS security_user_school (
    user_id UUID NOT NULL,
    school_id UUID NOT NULL,
    PRIMARY KEY (user_id, school_id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_school_user'
    ) THEN
        ALTER TABLE security_user_school
            ADD CONSTRAINT fk_security_user_school_user
                FOREIGN KEY (user_id) REFERENCES security_user (id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_school_school'
    ) THEN
        ALTER TABLE security_user_school
            ADD CONSTRAINT fk_security_user_school_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

-- Migrate existing single role / school if present
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'security_user' AND column_name = 'role_id'
    ) THEN
        INSERT INTO security_user_role (user_id, role_id)
        SELECT id, role_id
        FROM security_user
        WHERE role_id IS NOT NULL
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'security_user' AND column_name = 'school_id'
    ) THEN
        INSERT INTO security_user_school (user_id, school_id)
        SELECT id, school_id
        FROM security_user
        WHERE school_id IS NOT NULL
        ON CONFLICT DO NOTHING;

        UPDATE security_user u
        SET group_id = s.group_id
        FROM schools s
        WHERE u.school_id = s.id
          AND u.group_id IS NULL
          AND s.group_id IS NOT NULL;
    END IF;
END $$;

-- Drop old single FK columns
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_role'
    ) THEN
        ALTER TABLE security_user DROP CONSTRAINT fk_security_user_role;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_school'
    ) THEN
        ALTER TABLE security_user DROP CONSTRAINT fk_security_user_school;
    END IF;
END $$;

ALTER TABLE security_user DROP COLUMN IF EXISTS role_id;
ALTER TABLE security_user DROP COLUMN IF EXISTS school_id;
