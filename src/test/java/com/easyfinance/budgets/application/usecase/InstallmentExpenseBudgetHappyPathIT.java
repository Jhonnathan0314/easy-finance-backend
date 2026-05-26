package com.easyfinance.budgets.application.usecase;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.expenses.application.command.CreateInstallmentExpenseCommand;
import com.easyfinance.expenses.application.port.in.CreateInstallmentExpensePort;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {EasyFinanceApplication.class, InstallmentExpenseBudgetHappyPathIT.TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
class InstallmentExpenseBudgetHappyPathIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private CreateInstallmentExpensePort createInstallmentExpensePort;

    @Autowired
    private TestCurrentUserProvider currentUserProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void creatingInstallmentExpenseCreatesDebtBudgetsSubBudgetsAndImpacts() {
        Fixture fixture = createFixture();
        currentUserProvider.setParticipantId(fixture.participantId());

        createInstallmentExpensePort.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                "Budget happy path laptop",
                Money.cop(new BigDecimal("250000")),
                LocalDate.of(2026, 5, 11),
                3,
                Money.cop(new BigDecimal("100000")),
                LocalDate.of(2026, 6, 1),
                "Budget happy path debt",
                null
        ));

        Long expenseId = jdbcTemplate.queryForObject("SELECT id FROM expenses WHERE account_id = ? AND description = ?", Long.class, fixture.accountId(), "Budget happy path laptop");
        BigDecimal expenseAmount = jdbcTemplate.queryForObject("SELECT amount FROM expenses WHERE account_id = ? AND id = ?", BigDecimal.class, fixture.accountId(), expenseId);
        Long debtId = jdbcTemplate.queryForObject("SELECT id FROM debts WHERE account_id = ? AND name = ?", Long.class, fixture.accountId(), "Budget happy path debt");
        BigDecimal debtTotal = jdbcTemplate.queryForObject("SELECT total_amount FROM debts WHERE account_id = ? AND id = ?", BigDecimal.class, fixture.accountId(), debtId);
        BigDecimal remainingBalance = jdbcTemplate.queryForObject("SELECT remaining_amount FROM debts WHERE account_id = ? AND id = ?", BigDecimal.class, fixture.accountId(), debtId);
        Long budgetCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM budgets WHERE account_id = ?", Long.class, fixture.accountId());
        Long subBudgetCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sub_budgets WHERE account_id = ? AND source_type = 'DEBT_DERIVED'", Long.class, fixture.accountId());
        Long impactCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM budget_impacts WHERE account_id = ? AND debt_id = ?", Long.class, fixture.accountId(), debtId);
        List<Integer> months = jdbcTemplate.queryForList("SELECT period_month FROM budget_impacts WHERE account_id = ? AND debt_id = ? ORDER BY period_month", Integer.class, fixture.accountId(), debtId);
        Long invalidAmounts = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM budget_impacts
                WHERE account_id = ?
                  AND debt_id = ?
                  AND (expected_amount <> 100000.00 OR paid_amount <> 0.00 OR status <> 'ACTIVE' OR expense_id <> ?)
                """,
                Long.class,
                fixture.accountId(),
                debtId,
                expenseId
        );
        Long mismatchedSubBudgets = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM budget_impacts bi
                JOIN sub_budgets sb ON sb.account_id = bi.account_id AND sb.id = bi.sub_budget_id
                WHERE bi.account_id = ?
                  AND bi.debt_id = ?
                  AND (sb.debt_id <> bi.debt_id OR sb.source_type <> 'DEBT_DERIVED')
                """,
                Long.class,
                fixture.accountId(),
                debtId
        );

        assertThat(expenseId).isPositive();
        assertThat(expenseAmount).isEqualByComparingTo("250000.00");
        assertThat(debtId).isPositive();
        assertThat(debtTotal).isEqualByComparingTo("300000.00");
        assertThat(remainingBalance).isEqualByComparingTo("300000.00");
        assertThat(budgetCount).isEqualTo(3);
        assertThat(subBudgetCount).isEqualTo(3);
        assertThat(impactCount).isEqualTo(3);
        assertThat(months).containsExactly(6, 7, 8);
        assertThat(invalidAmounts).isZero();
        assertThat(mismatchedSubBudgets).isZero();
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject("INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id", Long.class, "Budget happy " + System.nanoTime(), "ACTIVE");
        jdbcTemplate.update("INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)", accountId, participantId, "ACCOUNT_MEMBER", "ACTIVE");
        Long categoryId = jdbcTemplate.queryForObject("INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Budget happy category " + System.nanoTime(), "budget-happy-category-" + System.nanoTime(), "EXPENSE", "ACTIVE");
        Long paymentMethodId = jdbcTemplate.queryForObject("INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Budget happy card " + System.nanoTime(), "budget-happy-card-" + System.nanoTime(), "CREDIT_CARD", "ACTIVE");
        return new Fixture(accountId, participantId, categoryId, paymentMethodId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject("INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id", Long.class, "budget-happy-" + System.nanoTime() + "@example.com", "hash", "Budget Happy User", "ACTIVE");
        return jdbcTemplate.queryForObject("INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id", Long.class, userId, "Budget Happy User", "ACTIVE");
    }

    private record Fixture(Long accountId, Long participantId, Long categoryId, Long paymentMethodId) {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        TestCurrentUserProvider testCurrentUserProvider() {
            return new TestCurrentUserProvider();
        }
    }

    static class TestCurrentUserProvider implements CurrentUserProvider {

        private final AtomicReference<Long> participantId = new AtomicReference<>();

        void setParticipantId(Long participantId) {
            this.participantId.set(participantId);
        }

        @Override
        public Optional<CurrentUser> currentUser() {
            return Optional.of(new CurrentUser(1L, participantId.get(), "budget-happy@example.com", Set.of("USER"), true));
        }
    }
}
