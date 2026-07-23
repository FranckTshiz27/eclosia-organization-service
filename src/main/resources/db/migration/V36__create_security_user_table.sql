CREATE TABLE IF NOT EXISTS security_user (
    id UUID PRIMARY KEY,
    keycloak_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role_id UUID NOT NULL,
    school_id UUID,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_security_user_keycloak_id'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT uk_security_user_keycloak_id UNIQUE (keycloak_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_security_user_username'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT uk_security_user_username UNIQUE (username);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_security_user_email'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT uk_security_user_email UNIQUE (email);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_role'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT fk_security_user_role
                FOREIGN KEY (role_id) REFERENCES security_role (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_security_user_school'
    ) THEN
        ALTER TABLE security_user
            ADD CONSTRAINT fk_security_user_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;
