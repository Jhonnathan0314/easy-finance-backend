package com.easyfinance.budgets.application.usecase;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {EasyFinanceApplication.class, InstallmentExpenseBudgetTransactionIT.TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
class InstallmentExpenseBudgetTransactionIT {

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
    void budgetImpactFailureRollsBackInstallmentExpenseAndDebt() {
        Fixture fixture = createFixture();
        currentUserProvider.setParticipantId(fixture.participantId());

        assertThatThrownBy(() -> createInstallmentExpensePort.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                "Budget rollback laptop",
                Money.cop(new BigDecimal("300000")),
                LocalDate.of(2026, 5, 11),
                3,
                Money.cop(new BigDecimal("100000")),
                LocalDate.of(2026, 6, 1),
                "Budget rollback debt",
                null
        ))).hasMessage("Forced budget impact creation failure.");

        Long expenseCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM expenses WHERE account_id = ? AND description = ?", Long.class, fixture.accountId(), "Budget rollback laptop");
        Long debtCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM debts WHERE account_id = ? AND name = ?", Long.class, fixture.accountId(), "Budget rollback debt");

        assertThat(expenseCount).isZero();
        assertThat(debtCount).isZero();
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject("INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id", Long.class, "Budget rollback " + System.nanoTime(), "ACTIVE");
        jdbcTemplate.update("INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)", accountId, participantId, "ACCOUNT_MEMBER", "ACTIVE");
        Long categoryId = jdbcTemplate.queryForObject("INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Budget rollback category " + System.nanoTime(), "budget-rollback-category-" + System.nanoTime(), "EXPENSE", "ACTIVE");
        Long paymentMethodId = jdbcTemplate.queryForObject("INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Budget rollback card " + System.nanoTime(), "budget-rollback-card-" + System.nanoTime(), "CREDIT_CARD", "ACTIVE");
        return new Fixture(accountId, participantId, categoryId, paymentMethodId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject("INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id", Long.class, "budget-rollback-" + System.nanoTime() + "@example.com", "hash", "Budget Rollback User", "ACTIVE");
        return jdbcTemplate.queryForObject("INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id", Long.class, userId, "Budget Rollback User", "ACTIVE");
    }

    private record Fixture(Long accountId, Long participantId, Long categoryId, Long paymentMethodId) {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        BudgetDebtImpactPort failingBudgetDebtImpactPort() {
            return new BudgetDebtImpactPort() {
                @Override
                public void createImpactsForInstallmentDebt(CreateDebtBudgetImpactsCommand command) {
                    throw new IllegalStateException("Forced budget impact creation failure.");
                }

                @Override
                public void applyDebtPaymentToImpacts(ApplyDebtPaymentImpactCommand command) {
                }

                @Override
                public void cancelActiveImpactsForDebt(Long accountId, Long debtId) {
                }
            };
        }

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
            return Optional.of(new CurrentUser(1L, participantId.get(), "budget-rollback@example.com", Set.of("USER"), true));
        }
    }
}
