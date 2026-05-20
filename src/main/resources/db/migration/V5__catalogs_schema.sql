CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_categories_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_categories_type CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_categories_active_account_type_name
    ON categories (account_id, type, normalized_name)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_categories_account_id ON categories (account_id);
CREATE INDEX idx_categories_account_status ON categories (account_id, status);
CREATE INDEX idx_categories_account_type_status ON categories (account_id, type, status);

CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT fk_payment_methods_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_methods_type CHECK (type IN ('CASH', 'BANK_ACCOUNT', 'CREDIT_CARD', 'DEBIT_CARD', 'DIGITAL_WALLET', 'OTHER')),
    CONSTRAINT chk_payment_methods_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uq_payment_methods_active_account_name
    ON payment_methods (account_id, normalized_name)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_payment_methods_account_id ON payment_methods (account_id);
CREATE INDEX idx_payment_methods_account_status ON payment_methods (account_id, status);
CREATE INDEX idx_payment_methods_account_type_status ON payment_methods (account_id, type, status);
