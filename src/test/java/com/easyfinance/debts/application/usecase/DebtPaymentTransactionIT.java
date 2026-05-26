package com.easyfinance.debts.application.usecase;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.debts.application.command.RegisterDebtPaymentCommand;
import com.easyfinance.debts.application.port.in.RegisterDebtPaymentPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.application.query.ListDebtsQuery;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.DomainException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {EasyFinanceApplication.class, DebtPaymentTransactionIT.TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
class DebtPaymentTransactionIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private RegisterDebtPaymentPort registerDebtPaymentPort;

    @Autowired
    private TestCurrentUserProvider currentUserProvider;

    @Autowired
    private FailingDebtRepositoryPort debtRepository;

    @Autowired
    private FailingBudgetDebtImpactPort budgetDebtImpactPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @AfterEach
    void resetTestDoubles() {
        debtRepository.failOnSave(false);
        budgetDebtImpactPort.failOnApply(false);
        currentUserProvider.clear();
    }

    @Test
    void debtSaveFailureRollsBackPaymentAndDebtChanges() {
        Fixture fixture = createFixture(new BigDecimal("100.00"));
        currentUserProvider.setParticipantId(fixture.participantId());
        debtRepository.failOnSave(true);

        assertThatThrownBy(() -> registerDebtPaymentPort.registerDebtPayment(command(fixture, new BigDecimal("80.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Forced debt save failure.");

        Long paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM debt_payments WHERE account_id = ? AND debt_id = ?",
                Long.class,
                fixture.accountId(),
                fixture.debtId()
        );
        BigDecimal remainingAmount = jdbcTemplate.queryForObject(
                "SELECT remaining_amount FROM debts WHERE id = ?",
                BigDecimal.class,
                fixture.debtId()
        );
        String state = jdbcTemplate.queryForObject(
                "SELECT state FROM debts WHERE id = ?",
                String.class,
                fixture.debtId()
        );

        assertThat(paymentCount).isZero();
        assertThat(remainingAmount).isEqualByComparingTo("100.00");
        assertThat(state).isEqualTo("ACTIVE");
    }

    @Test
    void concurrentPaymentsCannotOverpayDebt() throws Exception {
        Fixture fixture = createFixture(new BigDecimal("100.00"));
        currentUserProvider.setParticipantId(fixture.participantId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<PaymentAttempt> task = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    registerDebtPaymentPort.registerDebtPayment(command(fixture, new BigDecimal("80.00")));
                    return PaymentAttempt.succeeded();
                } catch (Throwable ex) {
                    return PaymentAttempt.failed(ex);
                }
            };

            Future<PaymentAttempt> first = executor.submit(task);
            Future<PaymentAttempt> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            PaymentAttempt firstResult = first.get(20, TimeUnit.SECONDS);
            PaymentAttempt secondResult = second.get(20, TimeUnit.SECONDS);

            assertThat(successCount(firstResult, secondResult)).isOne();
            assertThat(failure(firstResult, secondResult))
                    .isInstanceOfSatisfying(DomainException.class, ex ->
                            assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE"));

            Long paymentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM debt_payments WHERE account_id = ? AND debt_id = ?",
                    Long.class,
                    fixture.accountId(),
                    fixture.debtId()
            );
            BigDecimal remainingAmount = jdbcTemplate.queryForObject(
                    "SELECT remaining_amount FROM debts WHERE id = ?",
                    BigDecimal.class,
                    fixture.debtId()
            );
            String state = jdbcTemplate.queryForObject(
                    "SELECT state FROM debts WHERE id = ?",
                    String.class,
                    fixture.debtId()
            );

            assertThat(paymentCount).isOne();
            assertThat(remainingAmount).isEqualByComparingTo("20.00");
            assertThat(state).isEqualTo("ACTIVE");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void budgetImpactFailureRollsBackPaymentAndDebtChanges() {
        Fixture fixture = createDerivedFixture(new BigDecimal("100.00"));
        currentUserProvider.setParticipantId(fixture.participantId());
        budgetDebtImpactPort.failOnApply(true);

        assertThatThrownBy(() -> registerDebtPaymentPort.registerDebtPayment(command(fixture, new BigDecimal("80.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Forced budget impact update failure.");

        Long paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM debt_payments WHERE account_id = ? AND debt_id = ?",
                Long.class,
                fixture.accountId(),
                fixture.debtId()
        );
        BigDecimal remainingAmount = jdbcTemplate.queryForObject(
                "SELECT remaining_amount FROM debts WHERE id = ?",
                BigDecimal.class,
                fixture.debtId()
        );
        String state = jdbcTemplate.queryForObject(
                "SELECT state FROM debts WHERE id = ?",
                String.class,
                fixture.debtId()
        );

        assertThat(paymentCount).isZero();
        assertThat(remainingAmount).isEqualByComparingTo("100.00");
        assertThat(state).isEqualTo("ACTIVE");
    }

    private Fixture createFixture(BigDecimal debtAmount) {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Debt payment tx " + System.nanoTime(),
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_MEMBER",
                "ACTIVE"
        );
        Long debtId = jdbcTemplate.queryForObject(
                """
                INSERT INTO debts (account_id, participant_id, source_type, name, total_amount, scheduled_total_amount, total_currency, remaining_amount, remaining_currency, start_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                "MANUAL",
                "Transaction debt " + System.nanoTime(),
                debtAmount,
                debtAmount,
                "COP",
                debtAmount,
                "COP",
                "2026-05-11",
                "ACTIVE"
        );
        return new Fixture(accountId, participantId, debtId);
    }

    private Fixture createDerivedFixture(BigDecimal debtAmount) {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject("INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id", Long.class, "Debt payment derived tx " + System.nanoTime(), "ACTIVE");
        jdbcTemplate.update("INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)", accountId, participantId, "ACCOUNT_MEMBER", "ACTIVE");
        Long categoryId = jdbcTemplate.queryForObject("INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Tx category " + System.nanoTime(), "tx-category-" + System.nanoTime(), "EXPENSE", "ACTIVE");
        Long paymentMethodId = jdbcTemplate.queryForObject("INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id", Long.class, accountId, "Tx card " + System.nanoTime(), "tx-card-" + System.nanoTime(), "CREDIT_CARD", "ACTIVE");
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
                debtAmount,
                "COP",
                "2026-05-11",
                "PENDING",
                "ACTIVE",
                "INSTALLMENT"
        );
        Long debtId = jdbcTemplate.queryForObject(
                """
                INSERT INTO debts (account_id, participant_id, origin_expense_id, source_type, name, total_amount, scheduled_total_amount, total_currency, remaining_amount, remaining_currency, installment_count, installment_amount, installment_currency, start_date, end_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                expenseId,
                "INSTALLMENT_EXPENSE",
                "Laptop",
                debtAmount,
                debtAmount,
                "COP",
                debtAmount,
                "COP",
                1,
                debtAmount,
                "COP",
                "2026-05-11",
                "2026-06-11",
                "ACTIVE"
        );
        return new Fixture(accountId, participantId, debtId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "debt-payment-tx-" + System.nanoTime() + "@example.com",
                "hash",
                "Debt Payment Tx User",
                "ACTIVE"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Debt Payment Tx User",
                "ACTIVE"
        );
    }

    private static RegisterDebtPaymentCommand command(Fixture fixture, BigDecimal amount) {
        return new RegisterDebtPaymentCommand(
                fixture.accountId(),
                fixture.debtId(),
                DebtPaymentType.INSTALLMENT,
                Money.cop(amount),
                LocalDate.of(2026, 5, 11),
                null
        );
    }

    private static int successCount(PaymentAttempt first, PaymentAttempt second) {
        return (first.success() ? 1 : 0) + (second.success() ? 1 : 0);
    }

    private static Throwable failure(PaymentAttempt first, PaymentAttempt second) {
        return first.success() ? second.error() : first.error();
    }

    private record Fixture(Long accountId, Long participantId, Long debtId) {
    }

    private record PaymentAttempt(boolean success, Throwable error) {

        static PaymentAttempt succeeded() {
            return new PaymentAttempt(true, null);
        }

        static PaymentAttempt failed(Throwable error) {
            return new PaymentAttempt(false, error);
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FailingDebtRepositoryPort failingDebtRepositoryPort(@Qualifier("jpaDebtRepositoryAdapter") DebtRepositoryPort delegate) {
            return new FailingDebtRepositoryPort(delegate);
        }

        @Bean
        @Primary
        TestCurrentUserProvider testCurrentUserProvider() {
            return new TestCurrentUserProvider();
        }

        @Bean
        @Primary
        FailingBudgetDebtImpactPort failingBudgetDebtImpactPort() {
            return new FailingBudgetDebtImpactPort();
        }
    }

    static class FailingDebtRepositoryPort implements DebtRepositoryPort {

        private final DebtRepositoryPort delegate;
        private final AtomicBoolean failOnSave = new AtomicBoolean();

        FailingDebtRepositoryPort(DebtRepositoryPort delegate) {
            this.delegate = delegate;
        }

        void failOnSave(boolean value) {
            failOnSave.set(value);
        }

        @Override
        public Debt save(Debt debt) {
            if (failOnSave.get()) {
                throw new IllegalStateException("Forced debt save failure.");
            }
            return delegate.save(debt);
        }

        @Override
        public Optional<Debt> findByAccountIdAndId(Long accountId, Long debtId) {
            return delegate.findByAccountIdAndId(accountId, debtId);
        }

        @Override
        public Optional<Debt> findByAccountIdAndIdForUpdate(Long accountId, Long debtId) {
            return delegate.findByAccountIdAndIdForUpdate(accountId, debtId);
        }

        @Override
        public List<Debt> findActiveByAccountId(Long accountId) {
            return delegate.findActiveByAccountId(accountId);
        }

        @Override
        public PageResponse<Debt> findAll(ListDebtsQuery query) {
            return delegate.findAll(query);
        }
    }

    static class FailingBudgetDebtImpactPort implements BudgetDebtImpactPort {

        private final AtomicBoolean failOnApply = new AtomicBoolean();

        void failOnApply(boolean value) {
            failOnApply.set(value);
        }

        @Override
        public void createImpactsForInstallmentDebt(CreateDebtBudgetImpactsCommand command) {
        }

        @Override
        public void applyDebtPaymentToImpacts(ApplyDebtPaymentImpactCommand command) {
            if (failOnApply.get()) {
                throw new IllegalStateException("Forced budget impact update failure.");
            }
        }

        @Override
        public void cancelActiveImpactsForDebt(Long accountId, Long debtId) {
        }
    }

    static class TestCurrentUserProvider implements CurrentUserProvider {

        private final AtomicReference<Long> participantId = new AtomicReference<>();

        void setParticipantId(Long participantId) {
            this.participantId.set(participantId);
        }

        void clear() {
            participantId.set(null);
        }

        @Override
        public Optional<CurrentUser> currentUser() {
            return Optional.of(new CurrentUser(1L, participantId.get(), "debt-payment-tx@example.com", Set.of("USER"), true));
        }
    }
}
