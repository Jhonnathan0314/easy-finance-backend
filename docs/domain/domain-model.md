# Domain Model

## Domain Summary

Easy Finance manages collaborative financial accounts. Each account is an isolated financial context such as a home, business, or project.

All financial records belong to an account. Participants operate inside accounts through account roles.

## Bounded Contexts

### Identity Access

Owns authentication and technical authorization.

Main concepts:

- `User`
- `GlobalRole`
- `UserGlobalRole`
- JWT authentication

### Accounts

Owns account collaboration.

Main concepts:

- `Participant`
- `Account`
- `AccountParticipant`
- `AccountRole`

### Catalogs

Owns account-scoped catalogs.

Main concepts:

- `Category`
- `PaymentMethod`

### Expenses

Owns expenses.

Main concepts:

- `Expense`
- `ExpenseType`
- `ExpensePaymentState`
- `ExpenseSourceType`

### Debts

Owns debts and payments against debt.

Main concepts:

- `Debt`
- `DebtPayment`
- `DebtState`
- `DebtPaymentType`

### Budgets

Owns monthly budgets, sub-budgets, and budget impacts.

Main concepts:

- `MonthlyBudget`
- `SubBudget`
- `BudgetImpact`
- `BudgetPeriod`

### Income

Owns income records.

Main concepts:

- `Income`

### Imports

Owns Excel import validation and confirmation.

Main concepts:

- `ExpenseImportBatch`
- `ExpenseImportRow`
- `ExpenseImportPreview`
- `ExpenseImportValidationError`

### Analytics

Owns read models and reporting queries.

Main concepts:

- `MonthlySummary`
- `CashflowSummary`
- `ExpenseSummary`
- `ExpenseByCategory`
- `IncomeByCategory`
- `DebtSummary`
- `BudgetSummary`
- `BudgetVsExpensesByCategory`

### Audit

Planned bounded context for functional audit events. Not implemented today: there is no `AuditEvent` domain
class in the codebase, and `audit_events` is currently an unused table (schema only). See
`docs/audit/audit-strategy.md` for the target design.

Main concepts (planned):

- `AuditEvent`
- `AuditEventType`

## Main Entities

### User

Authentication identity.

Fields:

- `id`
- `email`
- `passwordHash`
- `fullName`
- `status`

### Participant

Financial actor linked 1:1 to a user.

Fields:

- `id`
- `userId`
- `displayName`
- `status`

### Account

Isolated financial context.

Fields:

- `id`
- `name`
- `description`
- `status`

### AccountParticipant

Membership between account and participant.

Fields:

- `id`
- `accountId`
- `participantId`
- `role`
- `joinedAt`
- `status`

### Category

Account-scoped expense category.

Fields:

- `id`
- `accountId`
- `name`
- `description`
- `type`
- `status`

### PaymentMethod

Account-scoped payment method.

Fields:

- `id`
- `accountId`
- `name`
- `description`
- `type`
- `status`

### Expense

Financial expense.

Fields:

- `id`
- `accountId`
- `categoryId`
- `participantId`
- `paymentMethodId`
- `description`
- `amount`
- `currency`
- `expenseDate`
- `expenseType`
- `paymentState`
- `status`
- `sourceType`
- `sourceDebtPaymentId`

Notes:

- `sourceType` is `MANUAL`, `IMPORT`, or `DEBT_PAYMENT`.
- The responsible `participantId` can be assigned explicitly by an account admin. Members can only assign themselves.
- Installment-derived debt inherits the assigned participant from the origin installment expense.
- Expenses with `sourceType = DEBT_PAYMENT` are conceptual records associated with a debt payment. They remain visible in conceptual expense analytics, but cashflow excludes them to avoid counting the same real payment twice.

### Debt

Financial obligation, manual or derived from an installment expense.

Fields:

- `id`
- `accountId`
- `participantId`
- `originExpenseId`
- `sourceType`
- `name`
- `description`
- `totalAmount`
- `scheduledTotalAmount`
- `installmentCount`
- `installmentAmount`
- `remainingBalance`
- `startDate`
- `endDate`
- `state`

Notes:

- Manual debt `participantId` can be assigned explicitly by an account admin. Members can only assign themselves.
- Installment-derived debt inherits the assigned participant from the origin expense.
- `totalAmount` is principal/original debt amount.
- `scheduledTotalAmount` is the programmed total to pay and can include implicit financing costs.
- `remainingBalance` represents pending principal.

### DebtPayment

Payment against a debt.

Fields:

- `id`
- `accountId`
- `debtId`
- `participantId`
- `paymentDate`
- `amount`
- `currency`
- `paymentType`
- `notes`
- `status`

Notes:

- A manual debt payment may optionally create an associated expense when the caller explicitly requests it.
- The debt payment remains the source of real cashflow.

### MonthlyBudget

Budget container for one account and one calendar month.

Fields:

- `id`
- `accountId`
- `year`
- `month`
- `name`
- `status`

### SubBudget

Budget line for a manual category allocation or a debt-derived installment allocation.

Fields:

- `id`
- `accountId`
- `budgetId`
- `categoryId`
- `participantId`
- `debtId`
- `name`
- `plannedAmount`
- `plannedCurrency`
- `spentAmount`
- `spentCurrency`
- `sourceType`
- `status`

Notes:

- Manual sub-budget execution is calculated at read time from active simple expenses in the budget month and same category.
- Manual sub-budget `participantId` is optional. When it is null, execution is global by category. When it is present, execution is scoped to that participant.
- Manual execution includes expenses with `sourceType = MANUAL` or `IMPORT`.
- Manual execution excludes `sourceType = DEBT_PAYMENT` to avoid duplicate counting with debt payments.
- Debt-derived sub-budgets inherit the participant from the debt and continue to use budget impacts and debt payments.

### BudgetImpact

Traceable financial impact against a monthly budget.

Fields:

- `id`
- `accountId`
- `budgetId`
- `subBudgetId`
- `debtId`
- `debtPaymentId`
- `periodYear`
- `periodMonth`
- `expectedAmount`
- `paidAmount`
- `status`
- `sourceType`

### Income

Financial income.

Fields:

- `id`
- `accountId`
- `categoryId`
- `participantId`
- `description`
- `amount`
- `currency`
- `incomeDate`
- `status`

Notes:

- Income is event-based in the MVP.
- The responsible `participantId` can be assigned explicitly by an account admin. Members can only assign themselves.
- Recurring income templates are not implemented yet; the current UX helper is duplication to a new date.

## Value Objects

### Money

Represents monetary amounts.

Fields:

- `amount`
- `currency`

Rules:

- Amount must be greater than or equal to zero unless a specific use case allows otherwise.
- Currency is always `COP` in the MVP.
- Use `BigDecimal` internally.
- Avoid floating point types.

### Email

Represents a user email.

Rules:

- Required.
- Unique.
- Normalized to lowercase.

### BudgetPeriod

Represents a calendar month.

Fields:

- `year`
- `month`

Rules:

- Month must be 1 through 12.
- Used for monthly budget lookup and creation.

### AccountRole

Allowed values:

- `ACCOUNT_ADMIN`
- `ACCOUNT_MEMBER`

### GlobalRole

Allowed values:

- `SUPER_ADMIN`
- `USER`

## Suggested Aggregates

### Account Aggregate

Root:

- `Account`

Related entities:

- `AccountParticipant`

Key invariants:

- An account must have at least one active participant.
- One participant can appear only once per account.
- Account role is required for each active membership.

### Expense Aggregate

Root:

- `Expense`

Key invariants:

- Expense belongs to one account.
- Category, responsible participant, and payment method must belong to the same account.
- Installment expense must trigger debt creation through the application use case.
- Associated debt-payment expenses must reference the created debt payment and must not duplicate cashflow.

### Debt Aggregate

Root:

- `Debt`

Related entities:

- `DebtPayment`

Key invariants:

- Remaining balance cannot be negative.
- Paid debts cannot receive new payments.
- Debt state is derived from remaining balance.

### MonthlyBudget Aggregate

Root:

- `MonthlyBudget`

Related entities:

- `SubBudget`
- `BudgetImpact`

Key invariants:

- One budget per account and calendar month.
- Debt-derived impacts must reference the originating debt.
- Debt-derived sub-budgets must be traceable.
- Manual sub-budget execution should not be persisted from simple expenses; it is calculated for read models.

## Candidate Domain Events

- `AccountCreatedEvent`
- `ParticipantAddedToAccountEvent`
- `ExpenseCreatedEvent`
- `InstallmentExpenseCreatedEvent`
- `DebtCreatedFromExpenseEvent`
- `DebtPaymentRegisteredEvent`
- `DebtPaidEvent`
- `MonthlyBudgetCreatedEvent`
- `DebtBudgetImpactCreatedEvent`
- `BudgetImpactUpdatedEvent`
- `IncomeCreatedEvent`
- `ExpenseImportConfirmedEvent`

Domain events are candidates for internal coordination and functional audit. They do not imply asynchronous processing in the MVP.

## Outside the MVP

Mutual debts are outside the MVP. They should not be modeled as aggregates, entities, events, tables, or endpoints yet.

## Pending Decisions

- Whether manual debts also create monthly budget impacts or only debts derived from installment expenses do.
- Whether deleting financial entities is allowed or all removals are state transitions.
- Whether full recurring income templates are needed beyond the current income duplication workflow.
