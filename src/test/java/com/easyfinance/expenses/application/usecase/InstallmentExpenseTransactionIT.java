package com.easyfinance.expenses.application.usecase;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.debts.application.command.CreateInstallmentExpenseDebtCommand;
import com.easyfinance.debts.application.port.in.CreateInstallmentExpenseDebtPort;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.expenses.application.command.CreateInstallmentExpenseCommand;
import com.easyfinance.expenses.application.port.in.CreateInstallmentExpensePort;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
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

@SpringBootTest(classes = {EasyFinanceApplication.class, InstallmentExpenseTransactionIT.TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
class InstallmentExpenseTransactionIT {

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
    void debtFailureRollsBackInstallmentExpense() {
        Fixture fixture = createFixture();
        currentUserProvider.setParticipantId(fixture.participantId());

        assertThatThrownBy(() -> createInstallmentExpensePort.createInstallmentExpense(new CreateInstallmentExpenseCommand(
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                "Rollback laptop",
                Money.cop(new BigDecimal("1200000")),
                LocalDate.of(2026, 5, 11),
                6,
                Money.cop(new BigDecimal("200000")),
                LocalDate.of(2026, 6, 1),
                "Rollback laptop debt",
                null
        ))).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("INSTALLMENT_EXPENSE_DEBT_CREATION_FAILED"));

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expenses WHERE account_id = ? AND description = ?",
                Long.class,
                fixture.accountId(),
                "Rollback laptop"
        );
        assertThat(count).isZero();
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Rollback " + System.nanoTime(),
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_MEMBER",
                "ACTIVE"
        );
        Long categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Installment rollback " + System.nanoTime(),
                "installment-rollback-" + System.nanoTime(),
                "EXPENSE",
                "ACTIVE"
        );
        Long paymentMethodId = jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Rollback card " + System.nanoTime(),
                "rollback-card-" + System.nanoTime(),
                "CREDIT_CARD",
                "ACTIVE"
        );
        return new Fixture(accountId, participantId, categoryId, paymentMethodId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "rollback-" + System.nanoTime() + "@example.com",
                "hash",
                "Rollback User",
                "ACTIVE"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Rollback User",
                "ACTIVE"
        );
    }

    private record Fixture(Long accountId, Long participantId, Long categoryId, Long paymentMethodId) {
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        CreateInstallmentExpenseDebtPort failingDebtPort() {
            return new CreateInstallmentExpenseDebtPort() {
                @Override
                public DebtResponse createInstallmentExpenseDebt(CreateInstallmentExpenseDebtCommand command) {
                    throw new BusinessRuleViolationException("DEBT_TEST_FAILURE", "Forced debt failure.");
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
            return Optional.of(new CurrentUser(1L, participantId.get(), "rollback@example.com", Set.of("USER"), true));
        }
    }
}
