# Debt Payment Capital/Interest Split - Implementation Plan

## Objective

Today `Debt.applyPayment(amount)` subtracts the **full paid amount** from `remainingBalance` (capital pendiente),
regardless of `DebtPaymentType`. For an installment debt with an implicit financing cost
(`scheduledTotalAmount = installmentAmount * installmentCount` greater than `totalAmount`, i.e. the cuota includes
interest), this is incorrect: `remainingBalance` gets drained by the interest portion too, so the debt can reach
`PAID` (or reject further payments as "exceeds remaining balance") before all installments are actually paid.

This plan adds an explicit capital/interest split to `INSTALLMENT`-type debt payments so that:
- Only the capital portion reduces `Debt.remainingBalance`.
- The interest portion is recorded on the payment for future reporting/graphs, without affecting the debt balance.
- Budget impact tracking ("was this month's cuota paid") keeps using the total paid amount, since
  `BudgetImpact.expectedAmount` already represents the full cuota (capital + interest) - this does not change.

This models a standard bank loan/credit amortization (cuota fija: parte a capital, parte a interés).

## Decisions Already Made

Confirmed with the product owner before drafting this plan; do not revisit without a new explicit decision:

- **Contract shape**: replace the single `amount` input with two explicit fields, `capitalAmount` and
  `interestAmount`, for both payment types (not just a "total + interest" pair). This removes ambiguity about what
  "amount" means once interest exists.
- **`CAPITAL_PAYMENT` never carries interest.** "Abono a capital" is by definition 100% capital; `interestAmount`
  must be zero/absent for this payment type (new validation).
- **The split is optional, not mandatory.** `interestAmount` defaults to `0` for `INSTALLMENT` payments too, so
  debts without real interest keep working exactly as before (single capital value, no extra data entry).
- **The associated conceptual expense (`createExpense=true`) uses the total paid amount** (capital + interest),
  because that is the real cash outflow from the account - not just the capital portion.
- **No historical backfill/recalculation.** Existing `debt_payments` rows did not track this split; they are
  migrated by treating their full existing `amount` as `capitalAmount` with `interestAmount = 0` (this matches how
  they have always behaved functionally - the whole amount already reduced `remainingBalance`). No attempt is made
  to reconstruct what portion of past payments was "really" interest. This only changes behavior for payments
  registered from now on.

## Out Of Scope

- Auto-calculating expected interest (e.g., from a rate) - the user enters both values manually every time.
- Adding a `hasInterest`/interest-rate attribute to `Debt` itself - not needed since the split is optional per
  payment, not gated by a debt-level flag.
- Splitting `BudgetImpact.expectedAmount`/`paidAmount` into capital/interest sub-tracking - out of scope; impacts
  keep tracking the total cuota as today.
- Excel debt-payment import (`ExpenseImportManagementUseCase`, `AplicaPagoDeuda` rows): continues to register
  100% capital / 0 interest, same as today. Adding capital/interest columns to that template is a separate,
  future change.
- Retroactively adjusting `remainingBalance` for debts that already have interest-bearing payment history.

## Backend Design

Follows the existing hexagonal structure of the `debts` module.

### Domain layer

`debts/domain/model/DebtPayment.java`:
- Replace the single `amount` field with `capitalAmount` and `interestAmount` (both `Money`, COP).
- Keep an `amount()` accessor for backward compatibility with existing call sites/response mapping, now returning
  the derived total: `capitalAmount.add(interestAmount)`.
- Add `capitalAmount()` and `interestAmount()` accessors.
- Validation (private constructor):
  - `capitalAmount` must be positive (reuses the existing `DEBT_PAYMENT_AMOUNT_INVALID` code, same message intent).
  - `interestAmount` must be `>= 0` (new code `DEBT_PAYMENT_INTEREST_AMOUNT_INVALID` if negative).
  - If `paymentType == CAPITAL_PAYMENT` and `interestAmount` is greater than zero, throw new code
    `DEBT_PAYMENT_CAPITAL_PAYMENT_INTEREST_NOT_ALLOWED`.
- `create(...)`/`restore(...)` factories take `capitalAmount` and `interestAmount` instead of `amount`.

`debts/domain/model/Debt.java`:
- `applyPayment(Money paymentAmount)` keeps its exact current logic and signature, but callers now pass
  `payment.capitalAmount()` instead of `payment.amount()`. No changes needed inside `Debt` itself - the "amount
  applied to remainingBalance" concept stays the same, only what the caller feeds it changes.

### Application layer

`debts/application/command/RegisterDebtPaymentCommand.java`:
- Replace `Money amount` with `Money capitalAmount` and `Money interestAmount`.
- Add `Money totalAmount()` helper: `capitalAmount.add(interestAmount)`.
- Update both convenience constructors (the 6-arg and 10-arg ones) to accept `capitalAmount`/`interestAmount`
  instead of a single `amount`; keep them defaulting `interestAmount` to `Money.zeroCop()` where callers do not
  pass it explicitly, so `ExpenseImportManagementUseCase` (Excel import) keeps compiling with 100% capital and no
  behavior change.

`debts/application/usecase/DebtPaymentManagementUseCase.registerDebtPayment`:
- `DebtPayment.create(...)` now takes `command.capitalAmount()` and `command.interestAmount()`.
- `debt.applyPayment(...)` now receives `savedPayment.capitalAmount()` (capital only) instead of
  `savedPayment.amount()`.
- `budgetDebtImpactPort.applyDebtPaymentToImpacts(...)` keeps receiving `savedPayment.amount()` (now the derived
  total capital+interest) - **unchanged call**, since `amount()` still returns the total. Budget impact tracking
  behavior does not change.
- `createDebtPaymentExpensePort.createDebtPaymentExpense(...)` now passes `command.totalAmount()` instead of
  `command.amount()` (the associated expense reflects the real cash outflow).

`budgets` module: no changes needed. `ApplyDebtPaymentImpactCommand`/`applyDebtPaymentToImpacts` already receive
"the amount to apply to impacts" as a plain `Money`; the caller is simply now passing a derived total instead of
the old single field.

### Infrastructure layer

`debts/infrastructure/persistence/jpa/DebtPaymentJpaEntity.java`:
- Add `capitalAmount` (`capital_amount`, `NOT NULL`) and `interestAmount` (`interest_amount`, `NOT NULL`, default
  `0`) columns. Keep the existing `amount` column as-is (still `NOT NULL`) - it continues to store the total,
  written by the mapper as `capitalAmount + interestAmount`, so any raw SQL/reporting relying on `amount` today
  keeps working unchanged.

`debts/infrastructure/mapper/DebtPaymentPersistenceMapper.java`:
- `toDomain`: build `Money` for `capitalAmount`/`interestAmount` from the new columns, call
  `DebtPayment.restore(..., capitalAmount, interestAmount, ...)`.
- `copyToEntity`: set `capital_amount`/`interest_amount` from the domain object, and set `amount` to
  `payment.amount()` (the derived total) exactly as before, so the column stays consistent.

### Migration

New `V20__debt_payment_capital_interest_split.sql`:

```sql
ALTER TABLE debt_payments
    ADD COLUMN capital_amount NUMERIC(19, 2),
    ADD COLUMN interest_amount NUMERIC(19, 2);

UPDATE debt_payments
SET capital_amount = amount,
    interest_amount = 0
WHERE capital_amount IS NULL;

ALTER TABLE debt_payments
    ALTER COLUMN capital_amount SET NOT NULL,
    ALTER COLUMN interest_amount SET NOT NULL,
    ALTER COLUMN interest_amount SET DEFAULT 0;
```

### Entrypoint layer

`debts/entrypoint/rest/dto/RegisterDebtPaymentRequest.java`:

```java
public record RegisterDebtPaymentRequest(
        @NotNull DebtPaymentTypeDto paymentType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal capitalAmount,
        @DecimalMin(value = "0.00") BigDecimal interestAmount,
        @NotNull LocalDate paymentDate,
        @Size(max = 1000) String notes,
        Boolean createExpense,
        Long categoryId,
        Long paymentMethodId,
        @Size(max = 500) String expenseDescription
) {
    public BigDecimal resolvedInterestAmount() {
        return interestAmount == null ? BigDecimal.ZERO : interestAmount;
    }
}
```

`debts/entrypoint/rest/mapper/DebtPaymentRestMapper.java` - build `RegisterDebtPaymentCommand` from
`request.capitalAmount()` and `request.resolvedInterestAmount()` instead of the old single `amount`.

`debts/application/response/DebtPaymentResponse.java` - add `capitalAmount` and `interestAmount` fields (keep
`amount` as the total, unchanged shape/position for existing consumers):

```java
public record DebtPaymentResponse(
        Long id,
        Long accountId,
        Long debtId,
        Long participantId,
        String paymentType,
        BigDecimal amount,
        BigDecimal capitalAmount,
        BigDecimal interestAmount,
        String currency,
        LocalDate paymentDate,
        String notes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
```

`DebtPaymentManagementUseCase.toPaymentResponse` populates the two new fields from
`payment.capitalAmount().amount()` / `payment.interestAmount().amount()`.

### Error codes (new)

| Code | HTTP status | Trigger |
|---|---|---|
| `DEBT_PAYMENT_INTEREST_AMOUNT_INVALID` | 422 | `interestAmount` is negative |
| `DEBT_PAYMENT_CAPITAL_PAYMENT_INTEREST_NOT_ALLOWED` | 422 | `paymentType = CAPITAL_PAYMENT` with `interestAmount > 0` |

`DEBT_PAYMENT_AMOUNT_INVALID` is reused for an invalid/missing `capitalAmount` (same meaning as the old `amount`
check). `DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE` is unchanged but now compares against `capitalAmount` only.

## Frontend Design

`shared/models/debt.models.ts`:
- `RegisterDebtPaymentRequest`: replace `amount: number` with `capitalAmount: number` and
  `interestAmount?: number`.
- `DebtPaymentResponse`: add `capitalAmount: number` and `interestAmount: number` (keep `amount` as the total).

`features/debts/debts-page.component.ts` (`paymentForm`):
- Replace the single `amount` control with `capitalAmount` (`Validators.required`, `Validators.min(0.01)`) and
  `interestAmount` (`Validators.min(0)`, default `0`).
- Show the `interestAmount` input only when `paymentType === 'INSTALLMENT'`; force/reset it to `0` and disable it
  when `paymentType === 'CAPITAL_PAYMENT'`, mirroring how `createExpense`-dependent fields are already
  conditionally shown/reset in this same form.
- Overpayment check (`toCents(raw.amount) > toCents(debt.remainingAmount)`) now compares
  `toCents(raw.capitalAmount)` against `debt.remainingAmount`, since remaining balance only tracks capital.
- Submit payload sends `capitalAmount` + `interestAmount` instead of `amount`.
- Payment history row: keep showing `payment.amount` (total) as today; when `payment.interestAmount > 0`, add a
  small breakdown, e.g. `Capital: {{ payment.capitalAmount | currency }} · Interés: {{ payment.interestAmount | currency }}`.

## Tests To Cover

**Backend**:
- `DebtPaymentTest` (domain): `create` builds correct `amount()` as capital+interest; rejects negative
  `interestAmount`; rejects `interestAmount > 0` when `paymentType = CAPITAL_PAYMENT`; `amount()`/`capitalAmount()`/
  `interestAmount()` accessors.
- `DebtTest`: no changes needed (`applyPayment` behavior/signature is unchanged; only the caller changes what it
  passes in) - confirm existing tests still pass unmodified.
- `DebtPaymentManagementUseCaseTest`: registering an `INSTALLMENT` payment with `capitalAmount=80000,
  interestAmount=20000` reduces `remainingBalance` by `80000` only; `budgetDebtImpactPort` receives `100000`
  (total); associated expense (`createExpense=true`) is created with amount `100000`; `CAPITAL_PAYMENT` with
  `interestAmount>0` rejected with `DEBT_PAYMENT_CAPITAL_PAYMENT_INTEREST_NOT_ALLOWED`.
- `DebtPaymentControllerTest`/equivalent: request validation for `capitalAmount` required/positive, negative
  `interestAmount` rejected.
- `ExpenseImportManagementUseCase` tests: confirm debt-payment rows via Excel still register with 100% capital,
  0 interest, no behavior change.

**Frontend**:
- `debts-page.component.spec.ts`: `interestAmount` field hidden/reset for `CAPITAL_PAYMENT`; payload sent to
  `registerPayment` includes `capitalAmount`/`interestAmount`; overpayment validation uses `capitalAmount`;
  payment history row shows the capital/interest breakdown when `interestAmount > 0`.

## Documentation To Update

Same set of files kept in sync between `easy-finance-backend/frontend-context/` and `easy-finance-frontend/docs/`:

- `frontend-context/api/api-overview.md`: update the debt payment registration request/response shape.
- `frontend-context/models/dto-reference.md`: update `RegisterDebtPaymentRequest`/`DebtPaymentResponse`.
- `frontend-context/business/business-rules.md`: under "Debts And Payments", document that only the capital
  portion reduces `remainingAmount`, interest is recorded for reporting only, and `CAPITAL_PAYMENT` cannot carry
  interest.
- `docs/domain/debt-budget-rules.md` / `docs/database/data-model.md`: document the new columns and the
  capital-only balance reduction rule.
- `frontend-context/frontend-guidance/ui-pages-map.md`: update the "Debt Payments" section to mention the
  capital/interest fields.

## Suggested Implementation Order

1. Migration + domain (`DebtPayment` capital/interest fields, validation) with unit tests.
2. Application layer (`RegisterDebtPaymentCommand`, `DebtPaymentManagementUseCase` wiring) with use-case tests.
3. Entrypoint (request/response DTOs, mapper, controller) with controller tests.
4. Update `ExpenseImportManagementUseCase` call site (capital-only, no behavior change) and confirm its tests
   still pass.
5. Frontend model/form/history changes with their specs.
6. Documentation sync (both repos).
7. Compile + run full backend (`mvn test`) and frontend (`ng test`) suites; confirm 0 failures.

## Open Questions

None. All four blocking product decisions (contract shape, associated-expense amount, optionality, historical
data handling) were confirmed before writing this plan.
