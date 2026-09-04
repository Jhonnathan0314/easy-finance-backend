# Debt Bulk Import - Implementation Plan

## Objective

Every other entity (expenses, income, categories, payment methods, annual budgets) can be bulk-imported from
Excel; debts cannot (`docs/implementation/roadmap.md:174` explicitly says "Debt import: not implemented", also
flagged in `release-candidate-notes.md:28` and `qa-checklist.md:283`). This plan adds bulk import for **manual**
debts, following the "direct" pattern already used by income/categories/payment-methods/annual-budget (preview
validates without persisting anything; a plain `POST` validates and creates in the same call) - not the "batch"
pattern used by expenses, since debts don't need the expense-import's two-step confirm workflow (there's no
"AplicaPagoDeuda"-style cross-entity creation required here... except the one new capability below).

## Decisions Already Made

Confirmed with the product owner before drafting this plan:

- **Direct import pattern** (like income), not batch (like expenses).
- **`ACCOUNT_MEMBER` can import**, matching who can already create a manual debt via `POST /debts` today (not
  restricted to `ACCOUNT_ADMIN` like catalogs/budgets).
- **Support an optional "already has an existing balance" column.** The primary real-world use case for bulk
  debt import is migrating debts the user already has elsewhere (another tracker, a spreadsheet) - not creating
  brand-new debts from scratch. Those debts often already have some amount paid down, so the import must let the
  user specify a starting `remainingBalance` different from the full capital, instead of always behaving like a
  freshly-created debt.

## Out Of Scope

- Importing `INSTALLMENT_EXPENSE`-sourced debts (they can only be derived from an installment expense, never
  created/imported directly - this is an existing, unrelated rule, not something this feature changes).
- Importing a debt's payment history (only the *current* remaining balance is captured, not a full ledger of past
  payments). If the user needs the historical payments themselves recorded (not just the net balance), they still
  register those manually afterward, same as today.
- Exposing "initial remaining balance" in the single manual-debt-creation form/UI - this plan only wires it
  through the backend command/domain (see below) and the import flow. Adding it to the manual-creation form is a
  natural, cheap follow-up enabled by this change, but not required by it.

## Backend Design

Mirrors the `income` import module 1:1 (`imports/domain/model`, `imports/application/{command,port,response,
usecase,validation}`, `imports/infrastructure/excel`, `imports/entrypoint/rest`).

### Domain change: `Debt.createManual` gains an optional starting balance

`debts/domain/model/Debt.java` - `createManual` currently always sets `remainingBalance = totalAmount`
(confirmed: no existing factory lets a manual debt start with a different remaining balance). Add a new optional
trailing parameter:

```java
public static Debt createManual(
        Long accountId, Long participantId, String name, String description,
        Money totalAmount, Integer installmentCount, Money installmentAmount,
        LocalDate startDate, LocalDate dueDate, String notes,
        Money initialRemainingBalance   // NEW, nullable
) {
    Money resolvedRemainingBalance = initialRemainingBalance == null ? totalAmount : initialRemainingBalance;
    ...
}
```

Validation (new, only runs when non-null): `initialRemainingBalance` must be `> 0` and `<= totalAmount`, else
`DEBT_INITIAL_REMAINING_BALANCE_INVALID`. Backward compatible: every existing caller passes `null` and gets
today's exact behavior (`remainingBalance = totalAmount`).

`CreateManualDebtCommand` and `CreateManualDebtRequest` each get the matching optional field
(`initialRemainingBalance` / `remainingBalance`), threaded through `DebtManagementUseCase.createManualDebt`
unchanged otherwise.

### New import module files

- `imports/application/validation/DebtImportParsedRow.java` (record, mirrors `IncomeImportParsedRow`): `rowNumber,
  name, description, totalAmount, remainingBalance, installmentCount, installmentAmount, startDate, dueDate,
  participantLabel, notes, errors: List<String>`, `valid()`.
- `imports/infrastructure/excel/ApachePoiDebtImportParser.java`: sheet `"Deudas"`, headers `Nombre, Descripcion,
  Capital, SaldoPendiente, NumeroCuotas, ValorCuota, FechaInicio, FechaVencimiento, Participante, Notas`.
  `Descripcion`, `SaldoPendiente`, `NumeroCuotas`, `ValorCuota`, `FechaVencimiento`, `Participante`, `Notas` are
  optional cells (mirrors income's `optionalCell`/`optionalText` helpers); `Nombre`, `Capital`, `FechaInicio` are
  required. Row-level pairing check: if only one of `NumeroCuotas`/`ValorCuota` is present, add error
  `IMPORT_DEBT_INSTALLMENT_PAIR_REQUIRED` (mirrors the frontend's existing `installmentPairValidator` in
  `debts-page.component.ts` - the domain itself doesn't enforce this pairing for `MANUAL` debts today, so the
  import row validation must, to avoid silently creating a debt with a nonsensical partial installment plan).
- `imports/application/usecase/DebtImportUseCase.java` implementing `GenerateDebtImportTemplatePort`,
  `PreviewDebtImportPort`, `ImportDebtPort` - structure mirrors `IncomeImportUseCase`:
  - Shared `validateImport`: resolves participant (`AssignedParticipantValidator`, same fallback-to-current-user
    behavior as income), validates `Capital > 0`, validates the installment pair, validates `SaldoPendiente` per
    the new domain rule (`> 0`, `<= Capital`) surfaced as `IMPORT_DEBT_REMAINING_BALANCE_INVALID` at the row
    level so the Excel error table is specific instead of a generic domain rejection.
  - `previewDebts`: validates only, never calls `CreateManualDebtPort`.
  - `importDebts`: validates, then for each valid row calls the existing debt-creation port (extended with
    `initialRemainingBalance`), same "all valid rows created independently" semantics as income (not
    all-or-nothing like expenses' batch, and also not "must be 100% valid or nothing created" like categories/
    payment-methods/annual-budget - match whichever behavior income actually has, since income is the direct
    template being followed).
- `imports/infrastructure/excel/ApachePoiDebtImportTemplateGenerator.java`: two sheets (`Deudas` + hidden
  `Valores`), `Participante` as a dropdown named range sourced from active account members (identical mechanism
  to income's `ParticipantesIngreso` named range). Cell validation: `Capital`/`ValorCuota` > 0, `NumeroCuotas` a
  positive integer, dates in a sane range, `SaldoPendiente` numeric.
- `imports/entrypoint/rest/DebtImportsController.java` + DTOs, endpoints:
  ```
  GET  /api/v1/accounts/{accountId}/imports/debts/template
  POST /api/v1/accounts/{accountId}/imports/debts/preview
  POST /api/v1/accounts/{accountId}/imports/debts
  ```
- `imports/application/response/DebtImportResponse.java` (`createdCount`, `rows`) and
  `DebtImportRowResponse.java` (mirrors `IncomeImportRowResponse`: parsed fields + `valid`, `createdDebtId`,
  `errors: List<String>`).

### Error codes (new)

| Code | Trigger |
|---|---|
| `DEBT_INITIAL_REMAINING_BALANCE_INVALID` (domain, 422) | `initialRemainingBalance` is `<= 0` or `> totalAmount` |
| `IMPORT_DEBT_INSTALLMENT_PAIR_REQUIRED` (row-level) | Only one of `NumeroCuotas`/`ValorCuota` present |
| `IMPORT_DEBT_REMAINING_BALANCE_INVALID` (row-level) | `SaldoPendiente` present but invalid relative to `Capital` |

Reuses existing domain codes where possible (`DEBT_NAME_REQUIRED`, `DEBT_AMOUNT_INVALID`, etc.) rather than
duplicating them under new import-specific names.

## Frontend Design

Mirrors the existing Income Import tab inside `features/imports/imports-page.component.ts` (or its own tab if
that component is organized as one-tab-per-entity - follow whatever structural convention that file already uses
for income/categories/payment-methods/annual-budget).

- New tab "Importar deudas": download template action, upload dropzone for preview, preview summary + row error
  table, upload again to confirm/create - identical interaction shape to the other direct-import tabs.
- `shared/models/import.models.ts` (or wherever `IncomeImportRowResponseDto` lives): add matching
  `DebtImportRowResponseDto`/`DebtImportResponseDto` types.
- New methods on the imports API service mirroring the income ones (`getDebtImportTemplate`,
  `previewDebtImport`, `importDebts`).
- Optional, cheap follow-up enabled by the same backend change: add an optional "Saldo pendiente inicial" field
  to the manual-debt-creation form in `debts-page.component.ts` (`manualDebtForm`), for the case where a user adds
  one pre-existing debt by hand instead of importing a batch. Not required for this plan to be complete.

## Tests To Cover

**Backend**:
- `DebtTest`: `createManual` with `initialRemainingBalance` null behaves exactly as today; with a valid override
  sets `remainingBalance` accordingly; rejects `<= 0` and `> totalAmount` with
  `DEBT_INITIAL_REMAINING_BALANCE_INVALID`.
- `DebtManagementUseCaseTest`: `createManualDebt` threads the new optional field through unchanged otherwise.
- `ApachePoiDebtImportParserTest`: required/optional cells, installment-pair row error, malformed dates/amounts.
- `ApachePoiDebtImportTemplateGeneratorTest`: sheet/header shape, participant dropdown population.
- `DebtImportUseCaseTest`: preview never creates; import creates only valid rows; participant fallback to current
  user when blank; remaining-balance validation row error; installment-pair row error.
- `DebtImportsControllerTest` / `DebtImportsControllerSecurityTest`: endpoint wiring, `ACCOUNT_MEMBER` allowed
  (unlike catalogs/budgets imports which require admin).

**Frontend**:
- New imports-page spec coverage for the debts tab: template download action, preview rendering, row error
  display, confirm creates and refreshes the debts list.

## Documentation To Update

- `docs/implementation/roadmap.md`, `docs/release/release-candidate-notes.md`, `docs/release/qa-checklist.md`:
  remove debt import from the "not implemented" lists.
- `frontend-context/api/api-overview.md` / mirrored `docs/api/api-overview.md`: add the 3 new endpoints.
- `frontend-context/models/dto-reference.md`: add the new DTOs.
- `frontend-context/business/business-rules.md`: document the new `initialRemainingBalance` concept and that
  debt import follows the direct (not batch) pattern, `ACCOUNT_MEMBER`-accessible.
- `frontend-context/frontend-guidance/ui-pages-map.md`: add the "Debt Import" entry under Imports.
- `docs/domain/debt-budget-rules.md` / `docs/database/data-model.md`: note the new optional starting-balance
  concept on manual debt creation.

## Suggested Implementation Order

1. Domain change (`Debt.createManual` optional balance) + command/request field, with unit tests.
2. Import module (parser, template generator, use case, controller) mirroring income import, with tests.
3. Frontend imports tab + models + API methods, with specs.
4. Documentation sync (both repos).
5. Optional: expose "Saldo pendiente inicial" on the manual single-debt form.

## Open Questions

None blocking. The row-level installment-pair check mirrors an already-established frontend rule rather than
introducing a new product decision.
