CREATE TABLE IF NOT EXISTS school_currencies (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    currency_id UUID NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_school_currencies_school'
    ) THEN
        ALTER TABLE school_currencies
            ADD CONSTRAINT fk_school_currencies_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_school_currencies_currency'
    ) THEN
        ALTER TABLE school_currencies
            ADD CONSTRAINT fk_school_currencies_currency
                FOREIGN KEY (currency_id) REFERENCES currencies (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_currency'
    ) THEN
        ALTER TABLE school_currencies
            ADD CONSTRAINT uk_school_currency UNIQUE (school_id, currency_id);
    END IF;
END $$;
