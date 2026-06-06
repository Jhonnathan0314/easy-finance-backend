ALTER TABLE expense_import_rows
    ADD COLUMN participant_label TEXT NULL,
    ADD COLUMN participant_id BIGINT NULL;

ALTER TABLE expense_import_rows
    ADD CONSTRAINT fk_expense_import_rows_account_participant
        FOREIGN KEY (account_id, participant_id)
        REFERENCES account_participants (account_id, participant_id)
        ON DELETE RESTRICT;

CREATE INDEX idx_expense_import_rows_account_participant
    ON expense_import_rows (account_id, participant_id);
