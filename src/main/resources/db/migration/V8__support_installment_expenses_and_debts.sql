ALTER TABLE expenses
    DROP CONSTRAINT chk_expenses_type;

ALTER TABLE expenses
    ADD CONSTRAINT chk_expenses_type CHECK (expense_type IN ('SIMPLE', 'INSTALLMENT'));

ALTER TABLE expenses
    ADD CONSTRAINT uq_expenses_account_id_id UNIQUE (account_id, id);

CREATE TABLE debts (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    origin_expense_id BIGINT NULL,
    source_type VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    total_currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    remaining_amount NUMERIC(19, 2) NOT NULL,
    remaining_currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    installment_count INTEGER NULL,
    installment_amount NUMERIC(19, 2) NULL,
    installment_currency VARCHAR(3) NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(1000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_debts_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_debts_account_participant FOREIGN KEY (account_id, participant_id) REFERENCES account_participants (account_id, participant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_debts_account_origin_expense FOREIGN KEY (account_id, origin_expense_id) REFERENCES expenses (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_debts_source_type CHECK (source_type IN ('MANUAL', 'INSTALLMENT_EXPENSE')),
    CONSTRAINT chk_debts_state CHECK (state IN ('ACTIVE', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_debts_total_amount_positive CHECK (total_amount > 0),
    CONSTRAINT chk_debts_remaining_amount_valid CHECK (remaining_amount >= 0 AND remaining_amount <= total_amount),
    CONSTRAINT chk_debts_total_currency CHECK (total_currency = 'COP'),
    CONSTRAINT chk_debts_remaining_currency CHECK (remaining_currency = 'COP'),
    CONSTRAINT chk_debts_installment_amount_positive CHECK (installment_amount IS NULL OR installment_amount > 0),
    CONSTRAINT chk_debts_installment_currency CHECK (installment_currency IS NULL OR installment_currency = 'COP'),
    CONSTRAINT chk_debts_installment_count_positive CHECK (installment_count IS NULL OR installment_count > 0),
    CONSTRAINT chk_debts_manual_source CHECK (
        (source_type = 'MANUAL' AND origin_expense_id IS NULL)
        OR source_type <> 'MANUAL'
    ),
    CONSTRAINT chk_debts_installment_source CHECK (
        (source_type = 'INSTALLMENT_EXPENSE'
            AND origin_expense_id IS NOT NULL
            AND installment_count IS NOT NULL
            AND installment_amount IS NOT NULL
            AND installment_currency IS NOT NULL)
        OR source_type <> 'INSTALLMENT_EXPENSE'
    )
);

CREATE UNIQUE INDEX uq_debts_origin_expense
    ON debts (origin_expense_id)
    WHERE origin_expense_id IS NOT NULL;

CREATE INDEX idx_debts_account_id ON debts (account_id);
CREATE INDEX idx_debts_account_state_start ON debts (account_id, state, start_date);
CREATE INDEX idx_debts_account_source_type ON debts (account_id, source_type);
CREATE INDEX idx_debts_account_participant ON debts (account_id, participant_id);
CREATE INDEX idx_debts_origin_expense ON debts (origin_expense_id);
