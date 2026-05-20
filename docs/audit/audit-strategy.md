# Audit Strategy

## Overview

Easy Finance requires two audit levels:

- Technical audit.
- Functional audit.

Technical audit answers: who created or updated a row and when.

Functional audit answers: what business operation happened, who did it, in which account, and what changed.

## Technical Audit

Use Spring Data JPA Auditing.

Fields:

```text
created_at
updated_at
created_by
updated_by
```

Recommended Java support:

```text
AuditorAware<Long>
```

The auditor should resolve the authenticated user id from the security context.

Tables requiring technical audit:

- `participants`
- `accounts`
- `account_participants`
- `categories`
- `payment_methods`
- `expenses`
- `debts`
- `debt_payments`
- `budgets`
- `sub_budgets`
- `budget_impacts`
- `incomes`

## Functional Audit

Use a dedicated `audit_events` table.

Functional audit events must be created from application use cases, not REST controllers.

This keeps audit close to business operations and makes it testable.

## audit_events Structure

Suggested columns:

```text
id BIGSERIAL PRIMARY KEY
event_type VARCHAR(100) NOT NULL
aggregate_type VARCHAR(100) NOT NULL
aggregate_id BIGINT NULL
account_id BIGINT NULL
actor_user_id BIGINT NULL
actor_participant_id BIGINT NULL
occurred_at TIMESTAMPTZ NOT NULL
correlation_id VARCHAR(100) NULL
metadata_json JSONB NULL
previous_state_json JSONB NULL
new_state_json JSONB NULL
```

Recommended indexes:

```text
idx_audit_events_account_occurred_at
idx_audit_events_actor_user_id
idx_audit_events_aggregate
idx_audit_events_correlation_id
```

## Auditable Events

Critical MVP events:

- `ACCOUNT_CREATED`
- `PARTICIPANT_ADDED_TO_ACCOUNT`
- `ACCOUNT_ROLE_CHANGED`
- `CATEGORY_CREATED`
- `PAYMENT_METHOD_CREATED`
- `EXPENSE_CREATED`
- `INSTALLMENT_EXPENSE_CREATED`
- `DEBT_CREATED`
- `DEBT_CREATED_FROM_EXPENSE`
- `DEBT_PAYMENT_REGISTERED`
- `DEBT_PAID`
- `MONTHLY_BUDGET_CREATED`
- `SUB_BUDGET_CREATED`
- `BUDGET_IMPACT_CREATED`
- `BUDGET_IMPACT_UPDATED`
- `INCOME_CREATED`
- `EXPENSE_IMPORT_PREVIEWED`
- `EXPENSE_IMPORT_CONFIRMED`

## Audit Metadata

Use `metadata_json` for contextual information.

Example:

```json
{
  "expenseId": 100,
  "debtId": 50,
  "installments": 6,
  "installmentAmount": "120000.00",
  "currency": "COP"
}
```

Use `previous_state_json` and `new_state_json` for relevant state changes.

Example for debt payment:

```json
{
  "remainingBalance": "500000.00",
  "state": "ACTIVE"
}
```

```json
{
  "remainingBalance": "380000.00",
  "state": "ACTIVE"
}
```

## Correlation ID

Every request should have a correlation id.

Sources:

- `X-Correlation-Id` request header when present.
- Generated server-side id otherwise.

The correlation id must appear in:

- logs
- API error responses
- audit events

## Transactional Behavior

Functional audit events for critical financial writes should be committed in the same transaction as the business operation.

If the business operation rolls back, the functional audit event should usually roll back too.

Separate failure-audit behavior can be added later if required.

## Security

Audit events must never contain:

- passwords
- password hashes
- raw JWTs
- full sensitive headers

## Retention

Retention policy is pending.

For MVP, keep audit events indefinitely unless storage constraints require otherwise.

## Outside the MVP

No mutual debt audit events.

## Pending Decisions

- Audit retention period.
- Whether audit event state snapshots should store full DTOs or minimal business fields.
- Whether failed operations require separate audit events.

