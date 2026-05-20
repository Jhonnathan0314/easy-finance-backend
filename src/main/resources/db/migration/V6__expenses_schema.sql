CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    expense_date DATE NOT NULL,
    payment_state VARCHAR(30) NOT NULL DEFAULT 'PAID',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    expense_type VARCHAR(30) NOT NULL DEFAULT 'SIMPLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_expenses_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expenses_participant FOREIGN KEY (participant_id) REFERENCES participants (id) ON DELETE RESTRICT,
    CONSTRAINT chk_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_expenses_currency CHECK (currency = 'COP'),
    CONSTRAINT chk_expenses_payment_state CHECK (payment_state IN ('PENDING', 'PARTIAL', 'PAID')),
    CONSTRAINT chk_expenses_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT chk_expenses_type CHECK (expense_type IN ('SIMPLE'))
);

CREATE INDEX idx_expenses_account_id ON expenses (account_id);
CREATE INDEX idx_expenses_account_status_date ON expenses (account_id, status, expense_date);
CREATE INDEX idx_expenses_account_category ON expenses (account_id, category_id);
CREATE INDEX idx_expenses_account_payment_method ON expenses (account_id, payment_method_id);
CREATE INDEX idx_expenses_account_participant ON expenses (account_id, participant_id);
CREATE INDEX idx_expenses_account_payment_state ON expenses (account_id, payment_state);
