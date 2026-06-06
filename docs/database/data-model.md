# Logical Data Model

## Database

Use PostgreSQL.

Identifiers:

- PostgreSQL type: `BIGSERIAL`.
- Java type: `Long`.
- Primary key column name: `id`.

Currency:

- MVP currency is COP.
- Monetary columns should use `NUMERIC(19,2)`.
- Currency column should use `VARCHAR(3)` with default `COP` where applicable.

## Main Tables

### users

Purpose: authentication identity.

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `email VARCHAR(150) NOT NULL`
- `password_hash VARCHAR(255) NOT NULL`
- `state VARCHAR(30) NOT NULL`
- `last_login TIMESTAMPTZ NULL`
- audit fields

Constraints:

- `uq_users_email`
- `chk_users_state`

### global_roles

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `name VARCHAR(50) NOT NULL`
- `description VARCHAR(255) NULL`

Constraints:

- `uq_global_roles_name`
- allowed values include `SUPER_ADMIN`, `USER`

### user_global_roles

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL`
- `global_role_id BIGINT NOT NULL`

Constraints:

- unique `(user_id, global_role_id)`
- foreign keys to `users` and `global_roles`

### participants

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `user_id BIGINT NOT NULL`
- `name VARCHAR(120) NOT NULL`
- `phone VARCHAR(30) NULL`
- `state VARCHAR(30) NOT NULL`
- audit fields

Constraints:

- `uq_participants_user_id`
- foreign key to `users`

### accounts

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `name VARCHAR(120) NOT NULL`
- `description VARCHAR(255) NULL`
- `state VARCHAR(30) NOT NULL`
- audit fields

Indexes:

- `idx_accounts_state`

### account_participants

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `participant_id BIGINT NOT NULL`
- `account_role VARCHAR(50) NOT NULL`
- `joined_at TIMESTAMPTZ NOT NULL`
- `state VARCHAR(30) NOT NULL`
- audit fields

Constraints:

- unique `(account_id, participant_id)`
- account role allowed values: `ACCOUNT_ADMIN`, `ACCOUNT_MEMBER`
- foreign keys to `accounts` and `participants`

Indexes:

- `idx_account_participants_account_id`
- `idx_account_participants_participant_id`

### categories

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `name VARCHAR(100) NOT NULL`
- `description VARCHAR(255) NULL`
- `state VARCHAR(30) NOT NULL`
- audit fields

Constraints:

- unique `(account_id, name)`
- foreign key to `accounts`

### payment_methods

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `name VARCHAR(120) NOT NULL`
- `normalized_name VARCHAR(120) NOT NULL`
- `description VARCHAR(500) NULL`
- `type VARCHAR(50) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- audit fields

Constraints:

- active uniqueness by `(account_id, normalized_name)` for `ACTIVE` records
- allowed types: `CASH`, `BANK_ACCOUNT`, `CREDIT_CARD`, `DEBIT_CARD`, `DIGITAL_WALLET`, `OTHER`
- allowed statuses: `ACTIVE`, `INACTIVE`
- foreign key to `accounts`

Indexes:

- `idx_payment_methods_account_id`
- `idx_payment_methods_account_status`
- `idx_payment_methods_account_type_status`

### expenses

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `category_id BIGINT NOT NULL`
- `payment_method_id BIGINT NOT NULL`
- `participant_id BIGINT NOT NULL`
- `description VARCHAR(500) NOT NULL`
- `amount NUMERIC(19,2) NOT NULL`
- `currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `expense_date DATE NOT NULL`
- `payment_state VARCHAR(30) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- `expense_type VARCHAR(30) NOT NULL`
- `source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL'`
- `source_debt_payment_id BIGINT NULL`
- audit fields

Constraints:

- `amount > 0`
- allowed payment states: `PENDING`, `PARTIAL`, `PAID`
- allowed statuses: `ACTIVE`, `CANCELLED`
- allowed expense types: `SIMPLE`, `INSTALLMENT`
- allowed source types: `MANUAL`, `IMPORT`, `DEBT_PAYMENT`
- `source_debt_payment_id` is required only when `source_type = DEBT_PAYMENT`
- foreign keys to account, category, participant, and payment method
- composite foreign key `(account_id, source_debt_payment_id)` to `debt_payments(account_id, id)`

Indexes:

- `idx_expenses_account_date`
- `idx_expenses_account_category`
- `idx_expenses_account_participant`

### debts

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `participant_id BIGINT NOT NULL`
- `origin_expense_id BIGINT NULL`
- `source_type VARCHAR(30) NOT NULL`
- `name VARCHAR(150) NOT NULL`
- `description VARCHAR(500) NULL`
- `total_amount NUMERIC(19,2) NOT NULL`
- `scheduled_total_amount NUMERIC(19,2) NOT NULL`
- `total_currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `remaining_amount NUMERIC(19,2) NOT NULL`
- `remaining_currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `installment_count INTEGER NULL`
- `installment_amount NUMERIC(19,2) NULL`
- `installment_currency VARCHAR(3) NULL`
- `start_date DATE NOT NULL`
- `end_date DATE NULL`
- `state VARCHAR(30) NOT NULL`
- `notes VARCHAR(1000) NULL`
- audit fields

Constraints:

- `total_amount > 0`
- `scheduled_total_amount >= total_amount`
- `remaining_amount >= 0`
- `remaining_amount <= total_amount`
- installment count/amount are required only for `INSTALLMENT_EXPENSE`
- allowed source types: `MANUAL`, `INSTALLMENT_EXPENSE`
- allowed states: `ACTIVE`, `PAID`, `CANCELLED`
- foreign keys to account, account participant membership, and optional origin expense

Indexes:

- `idx_debts_account_id`
- `idx_debts_account_state_start`
- `idx_debts_account_source_type`
- `idx_debts_account_participant`
- `idx_debts_origin_expense`

### debt_payments

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `debt_id BIGINT NOT NULL`
- `participant_id BIGINT NOT NULL`
- `payment_type VARCHAR(30) NOT NULL`
- `amount NUMERIC(19,2) NOT NULL`
- `currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `payment_date DATE NOT NULL`
- `notes VARCHAR(1000) NULL`
- `status VARCHAR(20) NOT NULL`
- audit fields

Constraints:

- `amount > 0`
- allowed payment types: `INSTALLMENT`, `CAPITAL_PAYMENT`
- allowed statuses: `ACTIVE`, `CANCELLED`
- foreign keys to account-scoped debts and account participant membership

Indexes:

- `idx_debt_payments_account_id`
- `idx_debt_payments_account_debt`
- `idx_debt_payments_account_payment_date`
- `idx_debt_payments_account_participant`
- `idx_debt_payments_account_status`
- `idx_debt_payments_account_payment_type`

### budgets

Monthly account budget.

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `year INTEGER NOT NULL`
- `month INTEGER NOT NULL`
- `name VARCHAR(120) NOT NULL`
- `description VARCHAR(255) NULL`
- `state VARCHAR(30) NOT NULL`
- audit fields

Constraints:

- unique `(account_id, year, month)`
- `month BETWEEN 1 AND 12`
- foreign key to accounts

Indexes:

- `idx_budgets_account_period`

### sub_budgets

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `budget_id BIGINT NOT NULL`
- `category_id BIGINT NULL`
- `participant_id BIGINT NULL`
- `debt_id BIGINT NULL`
- `name VARCHAR(120) NOT NULL`
- `planned_amount NUMERIC(19,2) NOT NULL`
- `planned_currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `spent_amount NUMERIC(19,2) NOT NULL DEFAULT 0`
- `spent_currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `source_type VARCHAR(50) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- audit fields

Constraints:

- `planned_amount >= 0`
- `spent_amount >= 0`
- allowed source types: `MANUAL`, `DEBT_DERIVED`
- allowed statuses: `ACTIVE`, `INACTIVE`
- debt-derived sub-budgets require `debt_id`
- manual sub-budgets cannot reference `debt_id`
- foreign keys to budgets, categories, optional participant membership, and optional debt

Read model note:

- Manual sub-budget `spentAmount` in API responses is calculated dynamically from active simple expenses in the same account/month/category with `source_type IN ('MANUAL', 'IMPORT')`.
- When `participant_id` is present, dynamic manual execution is scoped to that participant. When it is null, execution remains global by category.
- `source_type = 'DEBT_PAYMENT'` expenses are excluded from manual budget execution to avoid double counting debt payments.

Indexes:

- `idx_sub_budgets_budget_id`
- `idx_sub_budgets_account_participant`
- `idx_sub_budgets_debt_id`

### budget_impacts

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `budget_id BIGINT NOT NULL`
- `sub_budget_id BIGINT NULL`
- `expense_id BIGINT NULL`
- `debt_id BIGINT NULL`
- `period_year INTEGER NOT NULL`
- `period_month INTEGER NOT NULL`
- `expected_amount NUMERIC(19,2) NOT NULL`
- `expected_currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `paid_amount NUMERIC(19,2) NOT NULL DEFAULT 0`
- `paid_currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `status VARCHAR(30) NOT NULL`
- `source_type VARCHAR(50) NOT NULL`
- audit fields

Constraints:

- `expected_amount > 0`
- `paid_amount >= 0`
- `paid_amount <= expected_amount`
- `period_month BETWEEN 1 AND 12`
- allowed statuses: `ACTIVE`, `PAID`, `CANCELLED`
- allowed source types: `DEBT_INSTALLMENT`
- current impacts are debt/installment-oriented and require `debt_id`

Indexes:

- `idx_budget_impacts_account_period`
- `idx_budget_impacts_budget_id`
- `idx_budget_impacts_debt_id`
- `idx_budget_impacts_expense_id`

### incomes

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `account_id BIGINT NOT NULL`
- `category_id BIGINT NOT NULL`
- `participant_id BIGINT NOT NULL`
- `description VARCHAR(500) NOT NULL`
- `amount NUMERIC(19,2) NOT NULL`
- `currency VARCHAR(3) NOT NULL DEFAULT 'COP'`
- `income_date DATE NOT NULL`
- `status VARCHAR(30) NOT NULL`
- audit fields

Indexes:

- `idx_incomes_account_period`
- `idx_incomes_account_participant`

### audit_events

Columns:

- `id BIGSERIAL PRIMARY KEY`
- `event_type VARCHAR(100) NOT NULL`
- `aggregate_type VARCHAR(100) NOT NULL`
- `aggregate_id BIGINT NULL`
- `account_id BIGINT NULL`
- `actor_user_id BIGINT NULL`
- `actor_participant_id BIGINT NULL`
- `occurred_at TIMESTAMPTZ NOT NULL`
- `correlation_id VARCHAR(100) NULL`
- `metadata_json JSONB NULL`
- `previous_state_json JSONB NULL`
- `new_state_json JSONB NULL`

Indexes:

- `idx_audit_events_account_occurred_at`
- `idx_audit_events_actor_user_id`
- `idx_audit_events_aggregate`
- `idx_audit_events_correlation_id`

## Audit Fields

Use these fields on main financial tables:

```text
created_at TIMESTAMPTZ NOT NULL
updated_at TIMESTAMPTZ NOT NULL
created_by BIGINT NULL
updated_by BIGINT NULL
```

Main tables requiring audit fields:

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

## Recommended Constraint Strategy

- Use foreign keys for referential integrity.
- Use unique constraints for account-scoped names where needed.
- Use `CHECK` constraints for states and positive amounts.
- Avoid PostgreSQL enum types in the MVP to keep migrations easier.

## Outside the MVP

No mutual debt tables.

## Pending Decisions

- Whether `created_by` and `updated_by` reference `users(id)` or are nullable denormalized actor ids for operational resilience. Recommended default: reference `users(id)` when possible.
- Whether `budget_impacts.sub_budget_id` is mandatory for debt-derived impacts.
- Exact state values for archival behavior.
