CREATE TABLE IF NOT EXISTS currencies (
    id UUID PRIMARY KEY,
    code VARCHAR(3) NOT NULL,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    decimal_places INTEGER NOT NULL DEFAULT 2,
    numeric_code VARCHAR(3),
    symbol_position VARCHAR(20) NOT NULL DEFAULT 'BEFORE',
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
        WHERE conname = 'uk_currency_code'
    ) THEN
        ALTER TABLE currencies
            ADD CONSTRAINT uk_currency_code UNIQUE (code);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_currency_name'
    ) THEN
        ALTER TABLE currencies
            ADD CONSTRAINT uk_currency_name UNIQUE (name);
    END IF;
END $$;
