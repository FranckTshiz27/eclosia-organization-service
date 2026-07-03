CREATE TABLE IF NOT EXISTS fee_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    comment VARCHAR(500),
    school_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_fee_categories_school'
    ) THEN
        ALTER TABLE fee_categories
            ADD CONSTRAINT fk_fee_categories_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_fee_category_code'
    ) THEN
        ALTER TABLE fee_categories
            ADD CONSTRAINT uk_school_fee_category_code UNIQUE (school_id, code);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_fee_category_name'
    ) THEN
        ALTER TABLE fee_categories
            ADD CONSTRAINT uk_school_fee_category_name UNIQUE (school_id, name);
    END IF;
END $$;
