ALTER TABLE expenses
    ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN source_debt_payment_id BIGINT NULL;

ALTER TABLE expenses
    ADD CONSTRAINT chk_expenses_source_type CHECK (source_type IN ('MANUAL', 'IMPORT', 'DEBT_PAYMENT')),
    ADD CONSTRAINT fk_expenses_account_source_debt_payment
        FOREIGN KEY (account_id, source_debt_payment_id) REFERENCES debt_payments (account_id, id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_expenses_source_debt_payment
        CHECK (
            (source_type = 'DEBT_PAYMENT' AND source_debt_payment_id IS NOT NULL)
            OR (source_type <> 'DEBT_PAYMENT' AND source_debt_payment_id IS NULL)
        );

CREATE INDEX idx_expenses_account_source_type ON expenses (account_id, source_type);
CREATE INDEX idx_expenses_account_source_debt_payment ON expenses (account_id, source_debt_payment_id);
