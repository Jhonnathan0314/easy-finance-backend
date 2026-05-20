CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NULL,
    account_id BIGINT NULL,
    actor_user_id BIGINT NULL,
    actor_participant_id BIGINT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(100) NULL,
    metadata_json JSONB NULL,
    previous_state_json JSONB NULL,
    new_state_json JSONB NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_events_account_occurred_at
    ON audit_events (account_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor_user_id
    ON audit_events (actor_user_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_aggregate
    ON audit_events (aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_audit_events_correlation_id
    ON audit_events (correlation_id);

