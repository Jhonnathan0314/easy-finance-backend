ALTER TABLE sub_budgets
    ADD COLUMN debt_id BIGINT NULL;

UPDATE sub_budgets sb
SET debt_id = src.debt_id
FROM (
    SELECT account_id, sub_budget_id, MIN(debt_id) AS debt_id
    FROM budget_impacts
    GROUP BY account_id, sub_budget_id
    HAVING COUNT(DISTINCT debt_id) = 1
) src
WHERE sb.account_id = src.account_id
  AND sb.id = src.sub_budget_id
  AND sb.source_type = 'DEBT_DERIVED';

ALTER TABLE sub_budgets
    ADD CONSTRAINT fk_sub_budgets_account_debt
        FOREIGN KEY (account_id, debt_id) REFERENCES debts (account_id, id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_sub_budgets_source_debt_identity
        CHECK (
            (source_type = 'MANUAL' AND debt_id IS NULL)
            OR (source_type = 'DEBT_DERIVED' AND debt_id IS NOT NULL)
        );

CREATE UNIQUE INDEX uq_sub_budgets_account_budget_debt_derived
    ON sub_budgets (account_id, budget_id, debt_id)
    WHERE source_type = 'DEBT_DERIVED';

CREATE INDEX idx_sub_budgets_account_debt
    ON sub_budgets (account_id, debt_id);
