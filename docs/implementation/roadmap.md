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
- Support temporary and recurrent income fields.
- Add filters by account, participant, and period.

Deliverables:

- Income is available for dashboard and reports.

## Phase 10 - Imports

Goals:

- Implement Excel expense import preview.
- Validate categories, responsible participants, payment methods, dates, and amounts.
- Implement confirmation.
- Ensure confirmation is transactional.

Deliverables:

- Preview returns row-level validation errors.
- Confirmation creates expenses.
- Installment rows create derived debts and budget impacts.

## Phase 11 - Analytics

Goals:

- Implement dashboard summary.
- Implement expenses by category.
- Implement paid vs pending.
- Implement debt status.
- Implement budget execution.

Deliverables:

- Read endpoints for main dashboard.
- Report filters by date, category, responsible participant, and type.

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

