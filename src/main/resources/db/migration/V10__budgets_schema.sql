CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    name VARCHAR(150) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_budgets_account_year_month UNIQUE (account_id, year, month),
    CONSTRAINT uq_budgets_account_id_id UNIQUE (account_id, id),
    CONSTRAINT fk_budgets_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_budgets_period CHECK (year BETWEEN 2000 AND 2100 AND month BETWEEN 1 AND 12),
    CONSTRAINT chk_budgets_status CHECK (status IN ('ACTIVE', 'CLOSED', 'ARCHIVED'))
);

CREATE INDEX idx_budgets_account_id ON budgets (account_id);
CREATE INDEX idx_budgets_account_year_month ON budgets (account_id, year, month);
CREATE INDEX idx_budgets_account_status ON budgets (account_id, status);

CREATE TABLE sub_budgets (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    budget_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    name VARCHAR(150) NOT NULL,
    planned_amount NUMERIC(19, 2) NOT NULL,
    planned_currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    spent_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    spent_currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_sub_budgets_account_id_id UNIQUE (account_id, id),
    CONSTRAINT fk_sub_budgets_account_budget FOREIGN KEY (account_id, budget_id) REFERENCES budgets (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_sub_budgets_account_category FOREIGN KEY (account_id, category_id) REFERENCES categories (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_sub_budgets_amounts CHECK (planned_amount >= 0 AND spent_amount >= 0),
    CONSTRAINT chk_sub_budgets_currencies CHECK (planned_currency = 'COP' AND spent_currency = 'COP'),
    CONSTRAINT chk_sub_budgets_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_sub_budgets_source_type CHECK (source_type IN ('MANUAL', 'DEBT_DERIVED'))
);

CREATE INDEX idx_sub_budgets_account_id ON sub_budgets (account_id);
CREATE INDEX idx_sub_budgets_account_budget ON sub_budgets (account_id, budget_id);
CREATE INDEX idx_sub_budgets_account_category ON sub_budgets (account_id, category_id);
CREATE INDEX idx_sub_budgets_account_status ON sub_budgets (account_id, status);
CREATE INDEX idx_sub_budgets_account_source_type ON sub_budgets (account_id, source_type);

CREATE TABLE budget_impacts (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    budget_id BIGINT NOT NULL,
    sub_budget_id BIGINT NOT NULL,
    debt_id BIGINT NOT NULL,
    expense_id BIGINT NULL,
    period_year INTEGER NOT NULL,
    period_month INTEGER NOT NULL,
    expected_amount NUMERIC(19, 2) NOT NULL,
    expected_currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    paid_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
    paid_currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source_type VARCHAR(30) NOT NULL DEFAULT 'DEBT_INSTALLMENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_budget_impacts_account_debt_period UNIQUE (account_id, debt_id, period_year, period_month),
    CONSTRAINT fk_budget_impacts_account_budget FOREIGN KEY (account_id, budget_id) REFERENCES budgets (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget_impacts_account_sub_budget FOREIGN KEY (account_id, sub_budget_id) REFERENCES sub_budgets (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget_impacts_account_debt FOREIGN KEY (account_id, debt_id) REFERENCES debts (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget_impacts_account_expense FOREIGN KEY (account_id, expense_id) REFERENCES expenses (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_budget_impacts_period CHECK (period_year BETWEEN 2000 AND 2100 AND period_month BETWEEN 1 AND 12),
    CONSTRAINT chk_budget_impacts_amounts CHECK (expected_amount > 0 AND paid_amount >= 0 AND paid_amount <= expected_amount),
    CONSTRAINT chk_budget_impacts_currencies CHECK (expected_currency = 'COP' AND paid_currency = 'COP'),
    CONSTRAINT chk_budget_impacts_status CHECK (status IN ('ACTIVE', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_budget_impacts_source_type CHECK (source_type IN ('DEBT_INSTALLMENT'))
);

CREATE INDEX idx_budget_impacts_account_id ON budget_impacts (account_id);
CREATE INDEX idx_budget_impacts_account_budget ON budget_impacts (account_id, budget_id);
CREATE INDEX idx_budget_impacts_account_debt ON budget_impacts (account_id, debt_id);
CREATE INDEX idx_budget_impacts_account_period ON budget_impacts (account_id, period_year, period_month);
CREATE INDEX idx_budget_impacts_account_status ON budget_impacts (account_id, status);
CREATE INDEX idx_budget_impacts_account_source_type ON budget_impacts (account_id, source_type);
