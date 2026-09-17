ALTER TABLE debt_payments
    DROP CONSTRAINT chk_debt_payments_capital_amount_positive;

ALTER TABLE debt_payments
    ADD CONSTRAINT chk_debt_payments_capital_amount_non_negative CHECK (capital_amount >= 0),
    ADD CONSTRAINT chk_debt_payments_total_amount_positive CHECK (capital_amount + interest_amount > 0);
