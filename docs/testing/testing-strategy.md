# Testing Strategy

## Goals

Testing must protect financial correctness, account isolation, authorization, persistence constraints, and audit behavior.

The test suite should be fast at the unit level and realistic at the integration level.

## Test Stack

- JUnit 5.
- AssertJ.
- Mockito.
- Spring Boot Test.
- MockMvc.
- Testcontainers PostgreSQL.
- Flyway migrations.

## Domain Unit Tests

Domain tests must not start Spring.

Focus areas:

- `Money` validation.
- Debt creation.
- Debt end date calculation.
- Debt payment application.
- Debt state transitions.
- Remaining balance invariants.
- Budget period validation.
- Budget impact paid amount rules.

Example test classes:

```text
MoneyTest
DebtTest
DebtPaymentTest
BudgetImpactTest
InstallmentScheduleTest
```

## Application Use Case Tests

Use mocks for output ports.

Focus areas:

- Use case orchestration.
- Authorization checks.
- Transactional workflow expectations.
- Domain exceptions.
- Audit event creation.
- Calls to repository ports.

Example test classes:

```text
CreateInstallmentExpenseUseCaseTest
RegisterDebtPaymentUseCaseTest
CreateMonthlyBudgetUseCaseTest
ConfirmExpenseImportUseCaseTest
```

## Persistence Integration Tests

Use Testcontainers with PostgreSQL.

Focus areas:

- Flyway migration execution.
- JPA mappings.
- Constraints.
- Index-supported query paths where relevant.
- Repository adapter behavior.

Example test classes:

```text
JpaExpenseRepositoryAdapterIT
JpaDebtRepositoryAdapterIT
JpaBudgetRepositoryAdapterIT
AuditEventRepositoryIT
```

Rules:

- Do not use H2 for persistence tests.
- Test against PostgreSQL because constraints and SQL behavior matter.

## REST Tests

Use MockMvc.

Focus areas:

- Request validation.
- Response status codes.
- Error response format.
- Serialization.
- Pagination and filtering.
- Controller-to-use-case wiring.

Example test classes:

```text
ExpenseControllerTest
DebtPaymentControllerTest
BudgetControllerTest
RestExceptionHandlerTest
```

## Security Tests

Focus areas:

- Missing JWT returns `401`.
- Invalid JWT returns `401`.
- Valid JWT without account membership returns `403` or `404` according to endpoint policy.
- Insufficient account role returns `403`.
- Account-scoped queries do not leak cross-account data.

Example test classes:

```text
AccountAuthorizationServiceTest
ExpenseSecurityIT
BudgetSecurityIT
```

## End-to-End Integration Tests

Use Spring Boot Test, PostgreSQL Testcontainers, and MockMvc.

Critical MVP flows:

1. Create account.
2. Add participant.
3. Create category and payment method.
4. Create installment expense.
5. Verify derived debt.
6. Verify monthly budgets created.
7. Verify budget impacts created.
8. Register debt payment.
9. Verify remaining balance.
10. Verify audit event.

Recommended test class:

```text
InstallmentExpenseDebtBudgetFlowIT
```

## Excel Import Tests

Focus areas:

- Valid template parsing.
- Missing required fields.
- Unknown category.
- Unknown responsible participant.
- Unknown payment method.
- Preview does not persist financial records.
- Confirmation persists in one transaction.
- Existing simple-import files remain compatible.
- Rows marked as debt payments create the imported expense and register the debt payment in one transaction.
- Debt-payment import rows are rolled back completely if confirmation fails.

Recommended test classes:

```text
ExpenseImportParserTest
PreviewExpenseImportUseCaseTest
ConfirmExpenseImportUseCaseIT
```

## Test Data Builders

Use builders or object mothers for readable tests.

Examples:

```text
AccountTestBuilder
ParticipantTestBuilder
ExpenseTestBuilder
DebtTestBuilder
MoneyMother
```

## Coverage Priorities

Highest priority:

- debt payment correctness
- installment debt creation
- budget impact creation
- account authorization
- audit event persistence

Medium priority:

- CRUD validation
- reporting filters
- imports

Lower priority:

- simple getters/setters
- framework-generated code

## Pending Decisions

- Minimum coverage threshold.
- Whether architecture tests will use ArchUnit.
- Whether API contract tests are required for frontend integration.
