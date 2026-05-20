# Forms Validation Guide

Mirror backend validation to reduce failed submissions.

## Common

- Required: block submit and show inline message.
- Money: number, min `0.01` unless planned/spent budget where min is `0.00`.
- Currency: always `COP`; do not expose currency selector in MVP.
- Dates: use `YYYY-MM-DD`.
- Date ranges: `from <= to`.
- Pagination: default `page=0`, `size=20`.

## Auth

- email required and valid.
- password required.
- register password min length 8.
- fullName required.

## Accounts

- name required, max 120.
- description max 500.

## Catalogs

- name required, max 120.
- description max 500.
- category type required.
- payment method type required.
- Do not allow changing type in UI after create.

## Expenses

- categoryId required.
- paymentMethodId required.
- description required, max 500.
- amount required, min 0.01.
- expenseDate required.
- paymentState optional on create; default backend behavior is `PAID`.
- For installment expense:
  - totalAmount required, min 0.01.
  - installmentCount required, min 1.
  - installmentAmount required, min 0.01.
  - firstInstallmentDate required.
  - validate `installmentAmount * installmentCount == totalAmount`.

## Debts

- name required, max 150.
- description max 500.
- totalAmount required, min 0.01.
- installmentCount optional but if present min 1.
- installmentAmount optional but if present min 0.01.
- startDate required.
- notes max 1000.

## Debt Payments

- paymentType required.
- amount required, min 0.01.
- paymentDate required.
- amount must not exceed current `remainingAmount`.
- notes max 1000.

## Budgets

- year range: 2000-2100.
- month range: 1-12.
- manual subBudget name required.
- plannedAmount required, min 0.00.
- Do not allow editing `DEBT_DERIVED` sub-budgets.

## Income

- categoryId required and must be an `INCOME` category.
- description required.
- amount required, min 0.01.
- incomeDate required.

## Imports

- file required.
- extension `.xlsx`.
- max file size: 5MB unless backend config changes.
- first sheet headers:
  - `Fecha`
  - `Descripción`
  - `Monto`
  - `Categoría`
  - `MedioPago`
  - `EstadoPago`
- max rows: backend-configured, currently documented as 1000.
- formulas are rejected.

