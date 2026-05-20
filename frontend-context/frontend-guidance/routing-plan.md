# Routing Plan

```text
/login
/register
/app
/app/accounts
/app/accounts/:accountId/dashboard
/app/accounts/:accountId/expenses
/app/accounts/:accountId/debts
/app/accounts/:accountId/budgets
/app/accounts/:accountId/income
/app/accounts/:accountId/catalogs
/app/accounts/:accountId/imports
/app/accounts/:accountId/settings/members
```

## Guards

- Public routes: `/login`, `/register`.
- Authenticated routes: `/app/**`.
- Account routes require selected `accountId`.
- Do not call backend with a stale `accountId`; clear selected account if `/auth/me` fails.

## Suggested Route Modules

- `features/auth/auth.routes.ts`
- `features/accounts/accounts.routes.ts`
- `features/expenses/expenses.routes.ts`
- `features/debts/debts.routes.ts`
- `features/budgets/budgets.routes.ts`
- `features/income/income.routes.ts`
- `features/imports/imports.routes.ts`
- `features/analytics/analytics.routes.ts`

