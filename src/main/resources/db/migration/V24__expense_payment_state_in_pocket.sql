ALTER TABLE expenses DROP CONSTRAINT chk_expenses_payment_state;
ALTER TABLE expenses ADD CONSTRAINT chk_expenses_payment_state
    CHECK (payment_state IN ('PENDING', 'PARTIAL', 'IN_POCKET', 'PAID'));

ALTER TABLE expense_import_rows DROP CONSTRAINT chk_expense_import_rows_payment_state;
ALTER TABLE expense_import_rows ADD CONSTRAINT chk_expense_import_rows_payment_state
    CHECK (payment_state IS NULL OR payment_state IN ('PENDING', 'PARTIAL', 'IN_POCKET', 'PAID'));
