ALTER TABLE sub_budgets
    ADD COLUMN participant_id BIGINT NULL;

ALTER TABLE sub_budgets
    ADD CONSTRAINT fk_sub_budgets_account_participant
        FOREIGN KEY (account_id, participant_id) REFERENCES account_participants (account_id, participant_id) ON DELETE RESTRICT;

CREATE INDEX idx_sub_budgets_account_participant
    ON sub_budgets (account_id, participant_id);
