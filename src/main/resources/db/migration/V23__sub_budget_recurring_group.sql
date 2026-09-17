ALTER TABLE sub_budgets
    ADD COLUMN recurring_group_id UUID;

CREATE INDEX idx_sub_budgets_recurring_group ON sub_budgets (account_id, recurring_group_id) WHERE recurring_group_id IS NOT NULL;
