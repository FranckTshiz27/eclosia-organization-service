ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS uk_payment_receipt_number;

CREATE INDEX IF NOT EXISTS idx_payments_receipt_number ON payments (receipt_number);
