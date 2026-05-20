ALTER TABLE categories
    ADD CONSTRAINT uq_categories_account_id_id UNIQUE (account_id, id);

ALTER TABLE payment_methods
    ADD CONSTRAINT uq_payment_methods_account_id_id UNIQUE (account_id, id);

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_account_category
        FOREIGN KEY (account_id, category_id)
        REFERENCES categories (account_id, id)
        ON DELETE RESTRICT;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_account_payment_method
        FOREIGN KEY (account_id, payment_method_id)
        REFERENCES payment_methods (account_id, id)
        ON DELETE RESTRICT;

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_account_participant
        FOREIGN KEY (account_id, participant_id)
        REFERENCES account_participants (account_id, participant_id)
        ON DELETE RESTRICT;
