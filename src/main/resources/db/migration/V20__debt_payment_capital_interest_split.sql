ALTER TABLE debt_payments
    ADD COLUMN capital_amount NUMERIC(19, 2),
    ADD COLUMN interest_amount NUMERIC(19, 2);

UPDATE debt_payments
SET capital_amount = amount,
    interest_amount = 0
WHERE capital_amount IS NULL;

ALTER TABLE debt_payments
    ALTER COLUMN capital_amount SET NOT NULL,
    ALTER COLUMN interest_amount SET NOT NULL,
    ALTER COLUMN interest_amount SET DEFAULT 0;

ALTER TABLE debt_payments
    ADD CONSTRAINT chk_debt_payments_capital_amount_positive CHECK (capital_amount > 0),
    ADD CONSTRAINT chk_debt_payments_interest_amount_non_negative CHECK (interest_amount >= 0);
