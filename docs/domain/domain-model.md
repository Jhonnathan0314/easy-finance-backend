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
- `ExpensePaymentType`
- `ExpensePaymentState`

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
- `IncomePeriodicity`

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

- `DashboardSummary`
- `ExpenseByCategoryReport`
- `DebtStatusReport`
- `BudgetExecutionReport`

### Audit

Owns functional audit events.

Main concepts:

- `AuditEvent`
- `AuditEventType`

## Main Entities

### User

Authentication identity.

Fields:

- `id`
- `email`
- `passwordHash`
- `state`
- `lastLogin`

### Participant

Financial actor linked 1:1 to a user.

Fields:

- `id`
- `userId`
- `name`
- `phone`
- `state`

### Account

Isolated financial context.

Fields:

- `id`
- `name`
- `description`
- `state`

### AccountParticipant

Membership between account and participant.

Fields:

- `id`
- `accountId`
- `participantId`
- `accountRole`
- `joinedAt`
- `state`

### Category

Account-scoped expense category.

Fields:

- `id`
- `accountId`
- `name`
- `description`
- `state`

### PaymentMethod

Account-scoped payment method.

Fields:

- `id`
- `accountId`
- `responsibleParticipantId`
- `name`
- `type`
- `state`

### Expense

Financial expense.

Fields:

- `id`
- `accountId`
- `categoryId`
- `responsibleParticipantId`
- `paymentMethodId`
- `name`
- `description`
- `totalAmount`
- `date`
- `paymentType`
- `paymentState`

### Debt

Financial obligation, manual or derived from an installment expense.

Fields:

- `id`
- `accountId`
- `responsibleParticipantId`
- `paymentMethodId`
- `originExpenseId`
- `name`
- `totalAmount`
- `installments`
- `installmentAmount`
- `remainingBalance`
- `startDate`
- `endDate`
- `state`

### DebtPayment

Payment against a debt.

Fields:

- `id`
- `debtId`
- `responsibleParticipantId`
- `date`
- `amount`
- `paymentType`

### MonthlyBudget

Budget container for one account and one calendar month.

Fields:

- `id`
- `accountId`
- `year`
- `month`
- `name`
- `description`
- `state`

### SubBudget

Budget line for category, participant, or debt-derived allocation.

Fields:

- `id`
- `budgetId`
- `categoryId`
- `responsibleParticipantId`
- `debtId`
- `name`
- `amount`
- `paymentDate`
- `sourceType`
- `state`

### BudgetImpact

Traceable financial impact against a monthly budget.

Fields:

- `id`
- `accountId`
- `budgetId`
- `subBudgetId`
- `expenseId`
- `debtId`
- `periodYear`
- `periodMonth`
- `amount`
- `paidAmount`
- `sourceType`

### Income

Financial income.

Fields:

- `id`
- `accountId`
- `responsibleParticipantId`
- `name`
- `amount`
- `periodicity`
- `startDate`
- `endDate`
- `state`

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

- Whether budget impact and sub-budget should both exist for debt-derived entries, or if `BudgetImpact` becomes the main traceability table while `SubBudget` remains the planning line.
- Whether manual debts also create monthly budget impacts or only debts derived from installment expenses do.
- Whether deleting financial entities is allowed or all removals are state transitions.

