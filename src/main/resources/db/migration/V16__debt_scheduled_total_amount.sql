ALTER TABLE debts
    ADD COLUMN scheduled_total_amount NUMERIC(19, 2);

UPDATE debts
SET scheduled_total_amount = CASE
    WHEN source_type = 'INSTALLMENT_EXPENSE'
        AND installment_amount IS NOT NULL
        AND installment_count IS NOT NULL
    THEN installment_amount * installment_count
    ELSE total_amount
END
WHERE scheduled_total_amount IS NULL;

ALTER TABLE debts
    ALTER COLUMN scheduled_total_amount SET NOT NULL;

ALTER TABLE debts
    ADD CONSTRAINT chk_debts_scheduled_total_amount_valid
        CHECK (scheduled_total_amount >= total_amount);
