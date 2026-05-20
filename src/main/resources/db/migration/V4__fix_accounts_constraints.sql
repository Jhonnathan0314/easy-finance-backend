ALTER TABLE account_participants
    DROP CONSTRAINT fk_account_participants_account;

ALTER TABLE account_participants
    ADD CONSTRAINT fk_account_participants_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT;
