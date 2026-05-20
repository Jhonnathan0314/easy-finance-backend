# Backend Architecture

## Architecture Style

Easy Finance uses a modular monolith with hexagonal architecture / clean architecture.

The backend is organized around business modules. Each module contains its own domain model, application use cases, ports, and infrastructure adapters.

## Layers

### Domain Layer

Contains the core business model.

Responsibilities:

- Entities.
- Value objects.
- Domain services.
- Domain events.
- Business exceptions.
- Invariants.

Restrictions:

- No Spring annotations.
- No JPA annotations.
- No REST DTOs.
- No database classes.
- No framework dependencies.

Example classes:

```text
Expense
Debt
DebtPayment
MonthlyBudget
Money
DebtCreatedFromExpenseEvent
DebtAlreadyPaidException
```

### Application Layer

Coordinates use cases.

Responsibilities:

- Input ports.
- Output ports.
- Commands and queries.
- Transaction boundaries.
- Authorization orchestration.
- Domain event publication.
- Audit event creation.

Example classes:

```text
CreateExpenseUseCase
RegisterDebtPaymentUseCase
CreateInstallmentExpenseCommand
DebtRepositoryPort
AccountAuthorizationPort
FunctionalAuditPort
```

### Infrastructure Layer

Implements technical details.

Responsibilities:

- Spring Data JPA repositories.
- JPA entities.
- Persistence mappers.
- JWT services.
- Excel readers.
- Audit persistence.
- Metrics, logging, and configuration.

Example classes:

```text
JpaDebtRepositoryAdapter
DebtJpaEntity
DebtJpaRepository
JwtTokenService
ApachePoiExpenseImportReader
AuditEventJpaAdapter
```

### Entrypoint Layer

Receives external requests.

Responsibilities:

- REST controllers.
- Request and response DTOs.
- Request validation.
- Error response mapping through centralized handlers.

Example classes:

```text
ExpenseController
CreateExpenseRequest
ExpenseResponse
DebtPaymentController
RestExceptionHandler
```

## Dependency Rules

Allowed dependencies:

```text
entrypoint -> application
infrastructure -> application
application -> domain
infrastructure -> domain
```

Forbidden dependencies:

```text
domain -> application
domain -> infrastructure
domain -> entrypoint
application -> infrastructure
application -> entrypoint
```

The application layer depends on output port interfaces, not adapter implementations.

## Recommended Package Structure

```text
com.easyfinance
  bootstrap
    EasyFinanceApplication

  shared
    domain
      Money
      DomainEvent
      BusinessException
    application
      CurrentUserProvider
      PageQuery
    infrastructure
      error
      audit
      observability

  expenses
    domain
      model
      valueobject
      event
      exception
      service
    application
      port.in
      port.out
      command
      query
      usecase
    infrastructure
      persistence.jpa
      mapper
    entrypoint
      rest
      dto
      mapper
```

Use the same internal structure for `debts`, `budgets`, `accounts`, `catalogs`, `income`, `imports`, `analytics`, and `audit` when the module needs all layers.

## Module Boundaries

Each module owns its business concepts.

Examples:

- `expenses` owns expense creation rules.
- `debts` owns debt balance and payment rules.
- `budgets` owns monthly budget creation and impact rules.
- `accounts` owns membership and account-role rules.

When one module needs another module's capability, it should use an application-level port or a public application service, not internal infrastructure classes.

## Naming Conventions

Use English in code and Spanish in product/user-facing documentation when needed.

Class naming:

- Use cases: `CreateExpenseUseCase`, `RegisterDebtPaymentUseCase`.
- Commands: `CreateExpenseCommand`, `PayDebtCommand`.
- Queries: `FindExpensesQuery`.
- Input ports: `CreateExpensePort`, `PayDebtPort`.
- Output ports: `ExpenseRepositoryPort`, `BudgetImpactPort`.
- JPA entities: `ExpenseJpaEntity`.
- JPA repositories: `SpringDataExpenseRepository`.
- Adapters: `JpaExpenseRepositoryAdapter`.
- REST controllers: `ExpenseController`.
- DTOs: `CreateExpenseRequest`, `ExpenseResponse`.
- Mappers: `ExpenseDomainMapper`, `ExpenseRestMapper`.

Database naming:

- Tables: plural snake case, for example `expenses`, `debt_payments`.
- Columns: snake case, for example `created_at`, `account_id`.
- Primary keys: `id`.
- Foreign keys: `<entity>_id`.
- Indexes: `idx_<table>_<columns>`.
- Unique constraints: `uq_<table>_<columns>`.
- Foreign keys: `fk_<table>_<referenced_table>`.

## Transaction Rules

Transactions are application-layer concerns.

Use `@Transactional` on application use case implementations when the use case changes state.

Examples requiring one transaction:

- Creating an installment expense and its derived debt.
- Paying a debt and updating budget impact.
- Confirming an Excel import.
- Changing account membership roles.

## Error Handling

Use centralized REST error handling.

Domain exceptions should be explicit:

```text
AccountMembershipRequiredException
DebtAlreadyPaidException
InvalidMoneyAmountException
BudgetPeriodNotFoundException
```

REST errors must be mapped to a stable API error format.

## Outside the MVP

Mutual debts are outside the MVP. Keep package names, tables, endpoints, and use cases out of the codebase until the rules are approved.

