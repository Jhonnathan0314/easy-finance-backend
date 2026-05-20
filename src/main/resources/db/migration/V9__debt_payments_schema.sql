ALTER TABLE debts
    ADD CONSTRAINT uq_debts_account_id_id UNIQUE (account_id, id);

CREATE TABLE debt_payments (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    debt_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    payment_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    payment_date DATE NOT NULL,
    notes VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_debt_payments_account_debt FOREIGN KEY (account_id, debt_id) REFERENCES debts (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_debt_payments_account_participant FOREIGN KEY (account_id, participant_id) REFERENCES account_participants (account_id, participant_id) ON DELETE RESTRICT,
    CONSTRAINT chk_debt_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_debt_payments_currency CHECK (currency = 'COP'),
    CONSTRAINT chk_debt_payments_type CHECK (payment_type IN ('INSTALLMENT', 'CAPITAL_PAYMENT')),
    CONSTRAINT chk_debt_payments_status CHECK (status IN ('ACTIVE', 'CANCELLED'))
);

CREATE INDEX idx_debt_payments_account_id ON debt_payments (account_id);
CREATE INDEX idx_debt_payments_account_debt ON debt_payments (account_id, debt_id);
CREATE INDEX idx_debt_payments_account_payment_date ON debt_payments (account_id, payment_date);
CREATE INDEX idx_debt_payments_account_participant ON debt_payments (account_id, participant_id);
CREATE INDEX idx_debt_payments_account_status ON debt_payments (account_id, status);
CREATE INDEX idx_debt_payments_account_payment_type ON debt_payments (account_id, payment_type);
