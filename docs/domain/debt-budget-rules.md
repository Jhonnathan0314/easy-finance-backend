# Debt and Budget Rules

This document defines the critical financial flow:

```text
installment expense -> derived debt -> monthly installments -> budget impacts -> debt payments
```

## Purpose

When a user registers an expense in installments, the system must avoid duplicate concepts and financial inconsistencies.

The expense records the original purchase. The debt records the future obligation. Monthly budgets show the expected installment impact.

## Installment Expense Creation

Input:

- account
- category
- responsible participant
- payment method
- expense name
- total amount
- expense date
- number of installments
- installment amount
- debt start date

Rules:

- The account must exist and be active.
- The authenticated participant must belong to the account.
- Category, payment method, and responsible participant must belong to the same account.
- Number of installments must be greater than zero.
- Total amount must be greater than zero.
- Installment amount must be greater than zero.
- Currency is COP.

Transactional behavior:

1. Create the expense with payment type `INSTALLMENTS`.
2. Create exactly one derived debt linked to the expense.
3. Generate monthly budget impacts for the debt installments.
4. Persist functional audit events.
5. Commit all changes together.

If any step fails, the full transaction must rollback.

## Derived Debt Creation

Derived debt fields:

- `originExpenseId`: expense id.
- `accountId`: same account as the expense.
- `participantId`: same participant unless explicitly changed by future rules.
- `paymentMethodId`: same payment method as the expense.
- `totalAmount`: principal/original debt amount.
- `scheduledTotalAmount`: financed total to pay, calculated as `installmentAmount * installments`.
- `installments`: number of installments.
- `installmentAmount`: provided installment amount.
- `remainingBalance`: initialized with debt principal amount.
- `startDate`: provided debt start date.
- `endDate`: calculated.
- `state`: `ACTIVE`.

The origin expense keeps the original purchase or advance amount. The financed debt total may be greater
than the origin expense amount when the installments include interest, insurance, or financing costs. The
financed debt total cannot be lower than the origin expense amount.

## End Date Calculation

Debt end date is calculated by adding calendar months from the start date according to the number of installments.

Recommended rule:

```text
endDate = startDate.plusMonths(installments - 1)
```

Rationale:

- Installment 1 belongs to the start month.
- Installment 2 belongs to the next month.
- A 1-installment debt ends in the start month.

Example:

```text
startDate: 2026-01-15
installments: 3

installment 1: 2026-01
installment 2: 2026-02
installment 3: 2026-03
endDate: 2026-03-15
```

## Monthly Budget Impact Generation

For each installment number `n`, starting at 1:

```text
impactDate = startDate.plusMonths(n - 1)
period = year/month of impactDate
```

For each period:

1. Find monthly budget by `accountId`, `year`, and `month`.
2. If not found, create it automatically.
3. Create a debt-derived sub-budget or impact.
4. Link the impact to the debt and origin expense.

Recommended source type:

```text
DEBT_INSTALLMENT
```

Recommended impact amount:

```text
installmentAmount
```

## Budget Creation When Missing

If no monthly budget exists for the target month:

- Create budget automatically.
- Use a predictable name.

Example:

```text
Budget May 2026
```

Required fields:

- account id
- year
- month
- name
- state `ACTIVE`
- audit fields

## Budget Impact Fields

Recommended fields:

```text
id
account_id
budget_id
sub_budget_id
expense_id
debt_id
period_year
period_month
amount
paid_amount
source_type
created_at
updated_at
created_by
updated_by
```

## Debt Payment Flow

Input:

- debt id
- payment date
- amount
- payment type
- responsible participant

Rules:

- Debt must exist.
- Debt must be active.
- Payment amount must be greater than zero.
- Payment amount cannot exceed remaining balance.
- Responsible participant must belong to the debt account.
- Payment type must be `INSTALLMENT` or `CAPITAL_PAYMENT`.

Transactional behavior:

1. Create `DebtPayment`.
2. Reduce debt remaining balance.
3. Update debt state.
4. Update related budget impact progress when applicable.
5. Optionally create an associated conceptual expense when requested explicitly.
6. Persist functional audit event.

Associated expense behavior:

- `createExpense` defaults to `false`.
- When `createExpense = true`, the request must include an active account-scoped expense category, an active account-scoped payment method, and a non-blank expense description.
- The associated expense uses the debt payment amount and date, is created as `SIMPLE`, `ACTIVE`, `PAID`, and is marked with `sourceType = DEBT_PAYMENT`.
- Cashflow still counts only the debt payment as real outflow; the associated expense remains available for conceptual expense analytics.

## Budget Impact Update on Payment

For installment payments:

- Apply payment progress to the earliest unpaid debt-derived impact for the debt.
- Increase `paidAmount`.
- Do not allow `paidAmount` to exceed `amount`.

For capital payments:

- Reduce debt remaining balance.
- Record audit event.
- Budget impact behavior remains pending decision unless the business defines reallocation rules.

## Important Invariants

- One installment expense creates exactly one derived debt.
- A derived debt keeps a permanent reference to its origin expense.
- A debt-derived budget impact keeps a permanent reference to the debt.
- Debt remaining balance cannot be negative.
- Budget impact paid amount cannot be negative.
- Budget impact paid amount cannot exceed impact amount.
- Manual budget execution is not stored as budget impacts. Budget detail and budget summary calculate manual execution dynamically from active simple expenses by category/month.
- Dynamic manual execution includes expense `sourceType = MANUAL` and `IMPORT`; `DEBT_PAYMENT` is excluded to avoid double counting payments already represented by debt impacts.
- Financial writes must be transactional.

## Outside the MVP

Mutual debts are outside the MVP and must not participate in this flow.

## Pending Decisions

- Whether `endDate = startDate.plusMonths(installments - 1)` or `startDate.plusMonths(installments)` is preferred by the business wording. This document recommends `installments - 1` because the first installment belongs to the start month.
- Whether capital payments should reduce future budget impacts automatically.
