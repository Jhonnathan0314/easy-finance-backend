CREATE TABLE expense_import_batches (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_rows INTEGER NOT NULL,
    valid_rows INTEGER NOT NULL,
    invalid_rows INTEGER NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_expense_import_batches_account_id_id UNIQUE (account_id, id),
    CONSTRAINT fk_expense_import_batches_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_import_batches_account_participant FOREIGN KEY (account_id, participant_id) REFERENCES account_participants (account_id, participant_id) ON DELETE RESTRICT,
    CONSTRAINT chk_expense_import_batches_status CHECK (status IN ('PREVIEW', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_expense_import_batches_counts CHECK (total_rows >= 0 AND valid_rows >= 0 AND invalid_rows >= 0 AND total_rows = valid_rows + invalid_rows)
);

CREATE INDEX idx_expense_import_batches_account_id ON expense_import_batches (account_id);
CREATE INDEX idx_expense_import_batches_account_status ON expense_import_batches (account_id, status);
CREATE INDEX idx_expense_import_batches_account_participant ON expense_import_batches (account_id, participant_id);

CREATE TABLE expense_import_rows (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    row_number INTEGER NOT NULL,
    expense_date DATE NULL,
    description TEXT NULL,
    amount NUMERIC(19,2) NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'COP',
    category_name TEXT NULL,
    category_id BIGINT NULL,
    payment_method_name TEXT NULL,
    payment_method_id BIGINT NULL,
    payment_state VARCHAR(20) NULL,
    valid BOOLEAN NOT NULL,
    errors_json JSONB NULL,
    created_expense_id BIGINT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_expense_import_rows_account_id_id UNIQUE (account_id, id),
    CONSTRAINT uq_expense_import_rows_account_batch_row UNIQUE (account_id, batch_id, row_number),
    CONSTRAINT fk_expense_import_rows_account_batch FOREIGN KEY (account_id, batch_id) REFERENCES expense_import_batches (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_import_rows_account_category FOREIGN KEY (account_id, category_id) REFERENCES categories (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_import_rows_account_payment_method FOREIGN KEY (account_id, payment_method_id) REFERENCES payment_methods (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_import_rows_account_expense FOREIGN KEY (account_id, created_expense_id) REFERENCES expenses (account_id, id) ON DELETE RESTRICT,
    CONSTRAINT chk_expense_import_rows_row_number CHECK (row_number > 0),
    CONSTRAINT chk_expense_import_rows_amount CHECK (amount IS NULL OR amount > 0),
    CONSTRAINT chk_expense_import_rows_currency CHECK (currency = 'COP'),
    CONSTRAINT chk_expense_import_rows_payment_state CHECK (payment_state IS NULL OR payment_state IN ('PENDING', 'PARTIAL', 'PAID'))
);

CREATE INDEX idx_expense_import_rows_account_id ON expense_import_rows (account_id);
CREATE INDEX idx_expense_import_rows_account_batch ON expense_import_rows (account_id, batch_id);
CREATE INDEX idx_expense_import_rows_account_valid ON expense_import_rows (account_id, valid);
CREATE INDEX idx_expense_import_rows_account_created_expense ON expense_import_rows (account_id, created_expense_id);
