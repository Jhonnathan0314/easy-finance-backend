CREATE TABLE incomes (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    income_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_incomes_account_id_id UNIQUE (account_id, id),
    CONSTRAINT fk_incomes_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_incomes_account_category FOREIGN KEY (account_id, category_id) REFERENCES categories(account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_incomes_account_participant FOREIGN KEY (account_id, participant_id) REFERENCES account_participants(account_id, participant_id) ON DELETE RESTRICT,
    CONSTRAINT chk_incomes_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_incomes_currency CHECK (currency = 'COP'),
    CONSTRAINT chk_incomes_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE INDEX idx_incomes_account_id ON incomes(account_id);
CREATE INDEX idx_incomes_account_date ON incomes(account_id, income_date);
CREATE INDEX idx_incomes_account_category ON incomes(account_id, category_id);
CREATE INDEX idx_incomes_account_participant ON incomes(account_id, participant_id);
CREATE INDEX idx_incomes_account_status ON incomes(account_id, status);
