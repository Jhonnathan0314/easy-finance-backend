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

1. Create the expense with `expenseType = INSTALLMENT` (initial `paymentState = PENDING` unless the caller sets another valid state).
2. Create exactly one derived debt linked to the expense.
3. Generate monthly budget impacts for the debt installments.
4. Persist functional audit events (planned; not implemented today, this step is currently a no-op).
5. Commit all changes together.

If any step fails, the full transaction must rollback.

## Derived Debt Creation

Derived debt fields:

- `originExpenseId`: expense id.
- `accountId`: same account as the expense.
- `participantId`: inherited from the origin installment expense.
- `totalAmount`: principal/original debt amount.
- `scheduledTotalAmount`: financed total to pay, calculated as `installmentAmount * installmentCount`.
- `installmentCount`: number of installments.
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

Implemented rule (`Debt.calculateEndDate`, verified by `DebtTest` unit tests):

```text
endDate = startDate.plusMonths(installmentCount)
```

This adds one full calendar month past the month of the last installment. Installment `n` (1-indexed) is due
in `startDate.plusMonths(n - 1)`, so the last installment (`n = installmentCount`) is due one month before
`endDate`.

Example:

```text
startDate: 2026-01-15
installments: 3

installment 1 due: 2026-01
installment 2 due: 2026-02
installment 3 due: 2026-03
endDate: 2026-04-15
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
- status `ACTIVE`
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
- Capital amount must be greater than zero.
- Interest amount cannot be negative; it defaults to zero when omitted.
- A `CAPITAL_PAYMENT` cannot carry an interest amount (`DEBT_PAYMENT_CAPITAL_PAYMENT_INTEREST_NOT_ALLOWED`).
- Capital amount cannot exceed remaining balance (interest is not compared against remaining balance).
- Responsible participant must belong to the debt account.
- Payment type must be `INSTALLMENT` or `CAPITAL_PAYMENT`.

Capital/interest split (models a standard bank loan/credit amortization):

- Each debt payment is entered as `capitalAmount` + `interestAmount`. The split is optional per payment
  (`interestAmount` defaults to `0`), so debts without real financing cost keep working with a single value.
- Only `capitalAmount` reduces `Debt.remainingBalance`. This is the fix for a pre-existing inconsistency: before
  this split existed, the full paid amount (including any interest baked into the cuota) was subtracted from
  `remainingBalance`, which could exhaust the balance before all installments were paid on interest-bearing debts.
- `interestAmount` is stored on the `DebtPayment` for reporting/analytics only; it never affects `remainingBalance`.
- `DebtPayment.amount` is the derived total (`capitalAmount + interestAmount`) and is what budget impacts and the
  optional associated expense use - see below.
- No historical backfill: payments registered before this split treat their existing `amount` as 100% capital with
  `interestAmount = 0`, matching how they already behaved. This is not retroactively corrected.
- The Excel debt-payment import flow (`AplicaPagoDeuda`) is unaffected: it continues registering 100% capital,
  0 interest.

Transactional behavior:

1. Create `DebtPayment` with `capitalAmount`/`interestAmount`.
2. Reduce debt remaining balance by `capitalAmount` only.
3. Update debt state.
4. Update related budget impact progress using the total amount (`capitalAmount + interestAmount`) when applicable.
5. Optionally create an associated conceptual expense when requested explicitly.
6. Persist functional audit event (planned; not implemented today, this step is currently a no-op).

Associated expense behavior:

- `createExpense` defaults to `false`.
- When `createExpense = true`, the request must include an active account-scoped expense category, an active account-scoped payment method, and a non-blank expense description.
- The associated expense uses the debt payment's total amount (`capitalAmount + interestAmount`) and date - it reflects the real cash outflow, not just the capital portion. It is created as `SIMPLE`, `ACTIVE`, `PAID`, and is marked with `sourceType = DEBT_PAYMENT`.
- Cashflow still counts only the debt payment as real outflow; the associated expense remains available for conceptual expense analytics.

## Budget Impact Update on Payment

For installment payments:

- Apply payment progress to the earliest unpaid debt-derived impact for the debt, using the total paid amount
  (`capitalAmount + interestAmount`) - `expectedAmount` already represents the full cuota including any implicit
  financing cost, so this behavior is unchanged by the capital/interest split.
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

- Whether capital payments should reduce future budget impacts automatically.
