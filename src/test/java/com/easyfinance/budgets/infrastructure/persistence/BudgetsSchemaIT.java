package com.easyfinance.budgets.infrastructure.persistence;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class BudgetsSchemaIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void validBudgetSubBudgetAndImpactWork() {
        Fixture fixture = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long subBudgetId = insertSubBudget(fixture.accountId(), budgetId, fixture.categoryId());
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), fixture.expenseId());

        assertThatCode(() -> insertImpact(fixture.accountId(), budgetId, subBudgetId, debtId, fixture.expenseId(), 2026, 5))
                .doesNotThrowAnyException();
    }

    @Test
    void budgetPeriodIsUniquePerAccount() {
        Fixture fixture = createFixture();
        insertBudget(fixture.accountId(), 2026, 5);

        assertThatThrownBy(() -> insertBudget(fixture.accountId(), 2026, 5))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void subBudgetRejectsCategoryFromAnotherAccount() {
        Fixture fixture = createFixture();
        Fixture other = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);

        assertThatThrownBy(() -> insertSubBudget(fixture.accountId(), budgetId, other.categoryId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void impactRejectsDebtFromAnotherAccount() {
        Fixture fixture = createFixture();
        Fixture other = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long subBudgetId = insertSubBudget(fixture.accountId(), budgetId, fixture.categoryId());
        Long otherDebtId = insertDebt(other.accountId(), other.participantId(), other.expenseId());

        assertThatThrownBy(() -> insertImpact(fixture.accountId(), budgetId, subBudgetId, otherDebtId, fixture.expenseId(), 2026, 5))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void impactIsUniqueByDebtAndPeriod() {
        Fixture fixture = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long subBudgetId = insertSubBudget(fixture.accountId(), budgetId, fixture.categoryId());
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), fixture.expenseId());
        insertImpact(fixture.accountId(), budgetId, subBudgetId, debtId, fixture.expenseId(), 2026, 5);

        assertThatThrownBy(() -> insertImpact(fixture.accountId(), budgetId, subBudgetId, debtId, fixture.expenseId(), 2026, 5))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void manualSubBudgetRejectsDebtId() {
        Fixture fixture = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), fixture.expenseId());

        assertThatThrownBy(() -> insertDerivedSubBudget(fixture.accountId(), budgetId, fixture.categoryId(), debtId, "MANUAL"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void debtDerivedSubBudgetRequiresDebtId() {
        Fixture fixture = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);

        assertThatThrownBy(() -> insertDerivedSubBudget(fixture.accountId(), budgetId, fixture.categoryId(), null, "DEBT_DERIVED"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void debtDerivedSubBudgetIsUniqueByBudgetAndDebt() {
        Fixture fixture = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), fixture.expenseId());
        insertDerivedSubBudget(fixture.accountId(), budgetId, fixture.categoryId(), debtId, "DEBT_DERIVED");

        assertThatThrownBy(() -> insertDerivedSubBudget(fixture.accountId(), budgetId, fixture.categoryId(), debtId, "DEBT_DERIVED"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void debtDerivedSubBudgetRejectsDebtFromAnotherAccount() {
        Fixture fixture = createFixture();
        Fixture other = createFixture();
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long otherDebtId = insertDebt(other.accountId(), other.participantId(), other.expenseId());

        assertThatThrownBy(() -> insertDerivedSubBudget(fixture.accountId(), budgetId, fixture.categoryId(), otherDebtId, "DEBT_DERIVED"))
                .isInstanceOf(DataAccessException.class);
    }

    private Long insertBudget(Long accountId, int year, int month) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO budgets (account_id, year, month, name, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                year,
                month,
                "Budget",
                "ACTIVE"
        );
    }

    private Long insertSubBudget(Long accountId, Long budgetId, Long categoryId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO sub_budgets (account_id, budget_id, category_id, name, planned_amount, planned_currency, spent_amount, spent_currency, status, source_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                budgetId,
                categoryId,
                "Food",
                100000,
                "COP",
                0,
                "COP",
                "ACTIVE",
                "MANUAL"
        );
    }

    private Long insertDerivedSubBudget(Long accountId, Long budgetId, Long categoryId, Long debtId, String sourceType) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO sub_budgets (account_id, budget_id, category_id, debt_id, name, planned_amount, planned_currency, spent_amount, spent_currency, status, source_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                budgetId,
                categoryId,
                debtId,
                "Debt",
                100000,
                "COP",
                0,
                "COP",
                "ACTIVE",
                sourceType
        );
    }

    private Long insertDebt(Long accountId, Long participantId, Long expenseId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO debts (account_id, participant_id, origin_expense_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, installment_count, installment_amount, installment_currency, start_date, end_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                expenseId,
                "INSTALLMENT_EXPENSE",
                "Laptop",
                300000,
                "COP",
                300000,
                "COP",
                3,
                100000,
                "COP",
                "2026-05-01",
                "2026-08-01",
                "ACTIVE"
        );
    }

    private void insertImpact(Long accountId, Long budgetId, Long subBudgetId, Long debtId, Long expenseId, int year, int month) {
        jdbcTemplate.update(
                """
                INSERT INTO budget_impacts (account_id, budget_id, sub_budget_id, debt_id, expense_id, period_year, period_month, expected_amount, expected_currency, paid_amount, paid_currency, status, source_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId,
                budgetId,
                subBudgetId,
                debtId,
                expenseId,
                year,
                month,
                100000,
                "COP",
                0,
                "COP",
                "ACTIVE",
                "DEBT_INSTALLMENT"
        );
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject("INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id", Long.class, "Budget account " + System.nanoTime(), "ACTIVE");
        jdbcTemplate.update("INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)", accountId, participantId, "ACCOUNT_MEMBER", "ACTIVE");
        Long categoryId = jdbcTemplate.queryForObject("INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Budget category " + System.nanoTime(), "budget-category-" + System.nanoTime(), "EXPENSE", "ACTIVE");
        Long paymentMethodId = jdbcTemplate.queryForObject("INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Budget card " + System.nanoTime(), "budget-card-" + System.nanoTime(), "CREDIT_CARD", "ACTIVE");
        Long expenseId = jdbcTemplate.queryForObject(
                """
                INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                "Laptop",
                300000,
                "COP",
                "2026-05-01",
                "PENDING",
                "ACTIVE",
                "INSTALLMENT"
        );
        return new Fixture(accountId, participantId, categoryId, expenseId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject("INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id", Long.class, "budget-" + System.nanoTime() + "@example.com", "hash", "Budget User", "ACTIVE");
        return jdbcTemplate.queryForObject("INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id", Long.class, userId, "Budget User", "ACTIVE");
    }

    private record Fixture(Long accountId, Long participantId, Long categoryId, Long expenseId) {
    }
}
