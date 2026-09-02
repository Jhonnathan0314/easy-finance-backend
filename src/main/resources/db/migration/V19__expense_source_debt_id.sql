ALTER TABLE expenses
    ADD COLUMN source_debt_id BIGINT NULL;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_account_source_debt
        FOREIGN KEY (account_id, source_debt_id) REFERENCES debts (account_id, id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_expenses_source_debt
        CHECK (source_type = 'DEBT_PAYMENT' OR source_debt_id IS NULL);

CREATE INDEX idx_expenses_account_source_debt ON expenses (account_id, source_debt_id);
