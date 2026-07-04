CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    receipt_number VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(50) NOT NULL,
    enrollment_id UUID NOT NULL,
    academic_fee_id UUID NOT NULL,
    currency_rate_id UUID,
    amount NUMERIC(18, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    comment VARCHAR(500),
    payment_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_payments_enrollment'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_enrollment
                FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_payments_academic_fee'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_academic_fee
                FOREIGN KEY (academic_fee_id) REFERENCES academic_fees (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_payments_currency_rate'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_currency_rate
                FOREIGN KEY (currency_rate_id) REFERENCES currency_rates (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_payment_receipt_number'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT uk_payment_receipt_number UNIQUE (receipt_number);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_payment_transaction_reference'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT uk_payment_transaction_reference UNIQUE (transaction_reference);
    END IF;
END $$;
