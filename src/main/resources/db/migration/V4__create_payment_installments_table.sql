CREATE TABLE IF NOT EXISTS payment_installments (
    id UUID PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL,
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
        WHERE conname = 'fk_payment_installments_school'
    ) THEN
        ALTER TABLE payment_installments
            ADD CONSTRAINT fk_payment_installments_school
                FOREIGN KEY (school_id) REFERENCES schools (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_payment_installment_code'
    ) THEN
        ALTER TABLE payment_installments
            ADD CONSTRAINT uk_school_payment_installment_code UNIQUE (school_id, code);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_school_payment_installment_order'
    ) THEN
        ALTER TABLE payment_installments
            ADD CONSTRAINT uk_school_payment_installment_order UNIQUE (school_id, display_order);
    END IF;
END $$;
