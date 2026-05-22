# API Overview

Base URL local: `http://localhost:8080`

All protected requests use:

```http
Authorization: Bearer <accessToken>
```

## Auth

| Method | Path | Auth | Purpose |
|---|---|---:|---|
| POST | `/api/v1/auth/register` | No | Register user and participant, returns JWT. |
| POST | `/api/v1/auth/login` | No | Login and return JWT. |
| GET | `/api/v1/auth/me` | Yes | Get current authenticated user. |

## Accounts

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts` | Create account; creator becomes `ACCOUNT_ADMIN`. |
| GET | `/api/v1/accounts` | List accounts where current participant is active member. |
| GET | `/api/v1/accounts/{accountId}` | Get account detail. |
| PUT | `/api/v1/accounts/{accountId}` | Update account; admin only. |
| PATCH | `/api/v1/accounts/{accountId}/archive` | Archive account; admin only. |

## Members

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/accounts/{accountId}/members` | List members. |
| POST | `/api/v1/accounts/{accountId}/members` | Add/reactivate member by email; admin only. |
| PATCH | `/api/v1/accounts/{accountId}/members/{participantId}/role` | Change member role; admin only. |
| DELETE | `/api/v1/accounts/{accountId}/members/{participantId}` | Soft-remove member; admin only. |

## Categories

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts/{accountId}/categories` | Create category; admin only. |
| GET | `/api/v1/accounts/{accountId}/categories` | List categories. Query: `search`, `type`, `status`, `page`, `size`, `sort`. |
| GET | `/api/v1/accounts/{accountId}/categories/{categoryId}` | Get category. |
| PUT | `/api/v1/accounts/{accountId}/categories/{categoryId}` | Update category; admin only. Type changes are rejected by business rule. |
| DELETE | `/api/v1/accounts/{accountId}/categories/{categoryId}` | Deactivate category; admin only. |

## Payment Methods

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts/{accountId}/payment-methods` | Create payment method; admin only. |
| GET | `/api/v1/accounts/{accountId}/payment-methods` | List payment methods. Query: `search`, `type`, `status`, `page`, `size`, `sort`. |
| GET | `/api/v1/accounts/{accountId}/payment-methods/{paymentMethodId}` | Get payment method. |
| PUT | `/api/v1/accounts/{accountId}/payment-methods/{paymentMethodId}` | Update payment method; admin only. Type changes are rejected. |
| DELETE | `/api/v1/accounts/{accountId}/payment-methods/{paymentMethodId}` | Deactivate payment method; admin only. |

## Expenses

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts/{accountId}/expenses` | Create simple expense. |
| POST | `/api/v1/accounts/{accountId}/expenses/installments` | Create installment expense, derived debt, and budget impacts. |
| GET | `/api/v1/accounts/{accountId}/expenses` | List expenses. Query: `from`, `to`, `categoryId`, `paymentMethodId`, `participantId`, `paymentState`, `status`, `search`, `page`, `size`, `sort`. |
| GET | `/api/v1/accounts/{accountId}/expenses/{expenseId}` | Get expense. |
| PUT | `/api/v1/accounts/{accountId}/expenses/{expenseId}` | Update simple active expense if owner or admin. |
| PATCH | `/api/v1/accounts/{accountId}/expenses/{expenseId}/cancel` | Cancel simple active expense if owner or admin. |
| POST | `/api/v1/accounts/{accountId}/expenses/{expenseId}/duplicate` | Duplicate simple active expense if owner or admin. Body: `expenseDate` required, optional `amount`, `description`, `paymentState`. |

## Debts

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts/{accountId}/debts` | Create manual debt. |
| GET | `/api/v1/accounts/{accountId}/debts` | List debts. Query: `state`, `sourceType`, `participantId`, `from`, `to`, `page`, `size`, `sort`. |
| GET | `/api/v1/accounts/{accountId}/debts/{debtId}` | Get debt. |
| PATCH | `/api/v1/accounts/{accountId}/debts/{debtId}/cancel` | Cancel manual debt if owner or admin. Derived debt cancellation is blocked in this MVP. |

## Debt Payments

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts/{accountId}/debts/{debtId}/payments` | Register payment and update debt/budget impacts. Optional `createExpense=true` creates a conceptual expense with `sourceType=DEBT_PAYMENT`. |
| GET | `/api/v1/accounts/{accountId}/debts/{debtId}/payments` | List payments. Query: `from`, `to`, `paymentType`, `status`, `page`, `size`, `sort`. |
| GET | `/api/v1/accounts/{accountId}/debts/{debtId}/payments/{paymentId}` | Get payment detail. |

## Budgets

| Method | Path | Purpose |
|---|---|---|
| PUT | `/api/v1/accounts/{accountId}/budgets/{year}/{month}` | Create/update monthly budget; admin only. |
| GET | `/api/v1/accounts/{accountId}/budgets/{year}/{month}` | Get budget detail with sub-budgets and impacts. Manual sub-budget `spentAmount` is calculated from active simple expenses in the month/category. |
| POST | `/api/v1/accounts/{accountId}/budgets/{sourceYear}/{sourceMonth}/duplicate` | Duplicate a monthly budget into a target period; admin only. |
| GET | `/api/v1/accounts/{accountId}/budgets` | List budgets. Query: `year`, `status`, `sort`, `page`, `size`. |
| POST | `/api/v1/accounts/{accountId}/budgets/{budgetId}/sub-budgets` | Create manual sub-budget; admin only. |
| PUT | `/api/v1/accounts/{accountId}/budgets/{budgetId}/sub-budgets/{subBudgetId}` | Update manual sub-budget; admin only. |
| DELETE | `/api/v1/accounts/{accountId}/budgets/{budgetId}/sub-budgets/{subBudgetId}` | Deactivate manual sub-budget; admin only. |

## Income

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts/{accountId}/incomes` | Create income. |
| GET | `/api/v1/accounts/{accountId}/incomes` | List incomes. Query: `from`, `to`, `categoryId`, `participantId`, `status`, `search`, `page`, `size`, `sort`. |
| GET | `/api/v1/accounts/{accountId}/incomes/{incomeId}` | Get income. |
| PUT | `/api/v1/accounts/{accountId}/incomes/{incomeId}` | Update active income if owner or admin. |
| PATCH | `/api/v1/accounts/{accountId}/incomes/{incomeId}/cancel` | Cancel active income if owner or admin. |
| POST | `/api/v1/accounts/{accountId}/incomes/{incomeId}/duplicate` | Duplicate active income to another date if owner or admin. Body: `incomeDate` required, optional `amount`, optional `description`. |

## Analytics

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/accounts/{accountId}/analytics/monthly-summary?year=&month=` | Monthly financial summary. |
| GET | `/api/v1/accounts/{accountId}/analytics/cashflow-summary?from=&to=` | Real-money cashflow totals. Query: `participantId`, `categoryId`, `paymentMethodId`. |
| GET | `/api/v1/accounts/{accountId}/analytics/expense-summary?from=&to=` | Conceptual expense/purchase totals. Query: `categoryId`, `paymentMethodId`, `participantId`, `status`, `paymentState`, `expenseType`. |
| GET | `/api/v1/accounts/{accountId}/analytics/cashflow?from=&to=&groupBy=` | Real-money cashflow series grouped by `DAY`, `WEEK`, or `MONTH`. Query: `participantId`. |
| GET | `/api/v1/accounts/{accountId}/analytics/expenses-by-category?from=&to=` | Expense category breakdown. Query: `categoryId`, `paymentMethodId`, `participantId`, `status`, `paymentState`, `expenseType`. |
| GET | `/api/v1/accounts/{accountId}/analytics/expenses-by-payment-method?from=&to=` | Expense payment-method breakdown. Query: `categoryId`, `paymentMethodId`, `participantId`, `status`, `paymentState`, `expenseType`. |
| GET | `/api/v1/accounts/{accountId}/analytics/incomes-by-category?from=&to=` | Income category breakdown. Query: `categoryId`, `participantId`, `status`. |
| GET | `/api/v1/accounts/{accountId}/analytics/debt-summary` | Debt totals and counts. |
| GET | `/api/v1/accounts/{accountId}/analytics/budget-summary?year=&month=` | Monthly budget summary combining manual sub-budgets, dynamic manual expense execution, and debt impacts. |
| GET | `/api/v1/accounts/{accountId}/analytics/budget-vs-expenses-by-category?year=&month=` | Monthly comparison of active manual sub-budget planned amounts vs active conceptual expenses by category. |

## Expense Imports

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/accounts/{accountId}/imports/expenses/template` | Download account-scoped `.xlsx` template with valid categories, payment methods, payment states, and active debt options for preview. |
| POST | `/api/v1/accounts/{accountId}/imports/expenses/preview` | Multipart `.xlsx` preview; field name `file`. |
| POST | `/api/v1/accounts/{accountId}/imports/expenses/{batchId}/confirm` | Confirm valid rows, create expenses, and register debt payments for rows marked as debt payments. |
| GET | `/api/v1/accounts/{accountId}/imports/expenses/{batchId}` | Get import batch and row errors. |

Debt payment registration accepts optional `createExpense`, `categoryId`, `paymentMethodId`, and `expenseDescription`. When `createExpense=true`, backend creates a conceptual expense with `sourceType=DEBT_PAYMENT`; cashflow still counts only the debt payment outflow.
