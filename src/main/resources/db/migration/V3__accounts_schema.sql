CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE TABLE account_participants (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_account_participants_account_participant UNIQUE (account_id, participant_id),
    CONSTRAINT fk_account_participants_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_participants_participant FOREIGN KEY (participant_id) REFERENCES participants (id),
    CONSTRAINT chk_account_participants_role CHECK (role IN ('ACCOUNT_ADMIN', 'ACCOUNT_MEMBER')),
    CONSTRAINT chk_account_participants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_accounts_status ON accounts (status);
CREATE INDEX idx_account_participants_account_id ON account_participants (account_id);
CREATE INDEX idx_account_participants_participant_id ON account_participants (participant_id);
CREATE INDEX idx_account_participants_status ON account_participants (status);
CREATE INDEX idx_account_participants_participant_status ON account_participants (participant_id, status);
