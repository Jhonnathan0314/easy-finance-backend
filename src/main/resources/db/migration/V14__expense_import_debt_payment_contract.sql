ALTER TABLE debt_payments
    ADD CONSTRAINT uq_debt_payments_account_id_id UNIQUE (account_id, id);

ALTER TABLE expense_import_rows
    ADD COLUMN applies_debt_payment BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN debt_id BIGINT NULL,
    ADD COLUMN debt_label TEXT NULL,
    ADD COLUMN debt_payment_type VARCHAR(30) NULL,
    ADD COLUMN debt_payment_notes VARCHAR(1000) NULL,
    ADD COLUMN created_debt_payment_id BIGINT NULL;

ALTER TABLE expense_import_rows
    ADD CONSTRAINT fk_expense_import_rows_account_debt
        FOREIGN KEY (account_id, debt_id) REFERENCES debts (account_id, id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_expense_import_rows_account_created_debt_payment
        FOREIGN KEY (account_id, created_debt_payment_id) REFERENCES debt_payments (account_id, id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_expense_import_rows_debt_payment_type
        CHECK (debt_payment_type IS NULL OR debt_payment_type IN ('INSTALLMENT', 'CAPITAL_PAYMENT'));

CREATE INDEX idx_expense_import_rows_account_debt ON expense_import_rows (account_id, debt_id);
CREATE INDEX idx_expense_import_rows_account_created_debt_payment ON expense_import_rows (account_id, created_debt_payment_id);
CREATE INDEX idx_expense_import_rows_account_applies_debt_payment ON expense_import_rows (account_id, applies_debt_payment);
