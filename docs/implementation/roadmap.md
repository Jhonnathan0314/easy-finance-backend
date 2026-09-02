# Implementation Roadmap

## Principles

- Build the backend incrementally.
- Keep each phase deployable and testable.
- Do not implement features outside the MVP.
- Protect financial invariants from the first financial use case.
- Add migrations and tests with each feature.

## Phase 1 - Bootstrap

Goals:

- Create Maven project.
- Configure Java 21.
- Configure Spring Boot 4.
- Add base modules/packages.
- Configure PostgreSQL.
- Add Flyway.
- Add centralized error handling.
- Add structured logging and correlation id.
- Add Actuator.
- Add baseline test setup.

Deliverables:

- Application starts.
- Health endpoint works.
- Flyway runs.
- Testcontainers PostgreSQL works.

## Phase 2 - Security

Goals:

- Implement users.
- Implement global roles.
- Implement password hashing.
- Implement login.
- Implement JWT validation.
- Implement current user provider.

Deliverables:

- Authenticated requests work.
- JWT contains user identity and global roles.
- Technical audit can resolve `created_by` and `updated_by`.

## Phase 3 - Accounts and Participants

Goals:

- Implement participant 1:1 with user.
- Implement accounts.
- Implement account memberships.
- Implement account roles.
- Implement account authorization service.

Deliverables:

- User can create account.
- Creator becomes `ACCOUNT_ADMIN`.
- Account-scoped operations can validate membership.

## Phase 4 - Catalogs

Goals:

- Implement categories.
- Implement payment methods.
- Enforce account-scoped uniqueness and ownership.

Deliverables:

- `ACCOUNT_ADMIN` can manage categories and payment methods.
- Account members can read active catalogs.

## Phase 5 - Expenses

Goals:

- Implement simple expense creation.
- Implement expense listing with filters.
- Implement manual payment state.
- Validate category, participant, and payment method account consistency.

Deliverables:

- Simple expenses can be created and queried.
- Expense audit events are created.

## Phase 6 - Debts

Goals:

- Implement manual debts.
- Implement derived debt from installment expense.
- Implement debt end date calculation.
- Implement debt state rules.

Deliverables:

- Installment expense creates exactly one debt.
- Expense and debt are persisted in one transaction.

## Phase 7 - Debt Payments

Goals:

- Implement debt payment registration.
- Implement remaining balance update.
- Implement automatic state transition to `PAID`.
- Implement audit events.

Deliverables:

- Debt payments cannot exceed remaining balance.
- Paid debts cannot receive payments.

## Phase 8 - Budgets

Goals:

- Implement monthly budgets.
- Implement sub-budgets.
- Implement automatic budget creation for debt-derived impacts.
- Implement budget impacts for installment debts.
- Update budget impact progress on debt payment.

Deliverables:

- Installment debt generates monthly budget impacts.
- Missing monthly budgets are created automatically.

## Phase 9 - Income

Goals:

- Implement income records.
- Keep incomes event-based with a specific `incomeDate`.
- Support duplication to another date as the incremental UX helper.
- Add filters by account, participant, and period.
- Add text search by description.

Deliverables:

- Income is available for dashboard and reports.

## Phase 10 - Imports

Goals (original scope, expense import only):

- Implement Excel expense import preview.
- Validate categories, responsible participants, payment methods, dates, and amounts.
- Generate an account-scoped dynamic Excel template.
- Support optional debt-payment metadata in imported rows.
- Implement confirmation.
- Ensure confirmation is transactional.

Deliverables (original scope, expense import only):

- Preview returns row-level validation errors.
- Confirmation creates expenses.
- Rows marked as debt payments create the imported expense and register the debt payment in one transaction.

Current status (this phase grew beyond its original expense-only scope; verified against controllers/use cases):

- Expense import: **implemented**. Batch-persisted preview + confirm, optional debt-payment metadata, optional row-level `Participante` (blank falls back to the confirming participant). `ACCOUNT_MEMBER` or `ACCOUNT_ADMIN`.
- Income import: **implemented**. Direct validate-then-create (no preview batch persisted), optional row-level `Participante` (blank falls back to the participant running the import). `ACCOUNT_MEMBER` or `ACCOUNT_ADMIN`.
- Category import: **implemented**. Direct validate-then-create, no `Participante` column (account-level catalog). `ACCOUNT_ADMIN` only.
- Payment method import: **implemented**. Direct validate-then-create, no `Participante` column (account-level catalog). `ACCOUNT_ADMIN` only.
- Annual budget import: **implemented**. Direct validate-then-create of all 12 monthly budgets with `MANUAL`/`ACTIVE` sub-budgets, optional row-level `Participante` (blank means a global sub-budget instead of falling back to the importing participant). `ACCOUNT_ADMIN` only. Distinct from `POST /budgets/annual` (direct JSON creation, no Excel file).
- Debt import: **not implemented**.

## Phase 11 - Analytics

Goals:

- Implement monthly summary.
- Implement cashflow summary and grouped cashflow.
- Implement expenses by category.
- Implement incomes by category.
- Implement expenses by payment method.
- Implement debt status.
- Implement budget execution.
- Implement budget-vs-expenses by category.

Deliverables:

- Read endpoints for main dashboard.
- Analytics filters by date range, category, participant, payment method, status, payment state, and type where applicable.
- Cashflow avoids double counting expenses associated with debt payments.

## Phase 12 - Hardening

Goals:

- Review indexes.
- Add architecture tests.
- Add security regression tests.
- Add audit review.
- Add OpenAPI documentation.
- Tune logs and metrics.
- Validate production configuration.

Deliverables:

- MVP backend is ready for frontend integration and staging.

## Outside the MVP

Do not implement:

- mutual debts
- multi-currency
- distributed microservices
- reactive WebFlux architecture
- payment integrations

## Pending Decisions

- Whether to use Maven multi-module from day one.
- Exact frontend authentication flow.
- Refresh token strategy.
- Import batch persistence strategy.
