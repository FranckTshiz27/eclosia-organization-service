CREATE TABLE IF NOT EXISTS currency_rates (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    source_currency_id UUID NOT NULL,
    target_currency_id UUID NOT NULL,
    rate NUMERIC(18, 6) NOT NULL,
    effective_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    rate_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_currency_rates_school'
    ) THEN
        ALTER TABLE currency_rates
            ADD CONSTRAINT fk_currency_rates_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_currency_rates_source_currency'
    ) THEN
        ALTER TABLE currency_rates
            ADD CONSTRAINT fk_currency_rates_source_currency
                FOREIGN KEY (source_currency_id) REFERENCES currencies (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_currency_rates_target_currency'
    ) THEN
        ALTER TABLE currency_rates
            ADD CONSTRAINT fk_currency_rates_target_currency
                FOREIGN KEY (target_currency_id) REFERENCES currencies (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_currency_rate'
    ) THEN
        ALTER TABLE currency_rates
            ADD CONSTRAINT uk_currency_rate UNIQUE (
                school_id,
                source_currency_id,
                target_currency_id,
                effective_date
            );
    END IF;
END $$;
