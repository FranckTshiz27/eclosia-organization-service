ALTER TABLE academic_fees
    ADD COLUMN IF NOT EXISTS payment_installment_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_academic_fees_payment_installment'
    ) THEN
        ALTER TABLE academic_fees
            ADD CONSTRAINT fk_academic_fees_payment_installment
                FOREIGN KEY (payment_installment_id) REFERENCES payment_installments (id);
    END IF;
END $$;
