# Implementation Roadmap

## Phase 0: Bootstrap Angular

- Create Angular app.
- Configure environments.
- Add lint/format/test setup.
- Add shared layout primitives.

## Phase 1: Auth

- Login/register pages.
- Auth service/store.
- Bearer token interceptor.
- Error interceptor.
- Auth guard.
- `/auth/me` bootstrap.

## Phase 2: Layout App + Account Selection

- App shell.
- Account list and selector.
- Create account flow.
- Role-aware menu.

## Phase 3: Accounts/Catalogs

- Account detail/settings.
- Members page.
- Categories page.
- Payment methods page.
- Admin-only controls.

## Phase 4: Expenses

- Simple expense CRUD.
- Filters and pagination.
- Installment expense create form with total validation.
- Block update/cancel controls for installment expenses.

## Phase 5: Debts/Payments

- Debt list/detail.
- Manual debt creation.
- Register payment flow.
- Debt remaining balance display.

## Phase 6: Budgets

- Monthly budget view.
- Manual sub-budget management.
- Debt-derived impacts display.

## Phase 7: Income

- Income CRUD.
- Filters by date/category/status.

## Phase 8: Analytics Dashboard

- Monthly summary cards.
- Expenses by category.
- Incomes by category.
- Debt summary.
- Budget summary.

## Phase 9: Imports Excel

- Upload preview.
- Row error table.
- Confirm batch.
- Batch detail page.

## Phase 10: Polish/Hardening

- Empty states.
- Loading states.
- Mobile layouts.
- Accessibility pass.
- Error messages by backend code.
- Smoke test script.

