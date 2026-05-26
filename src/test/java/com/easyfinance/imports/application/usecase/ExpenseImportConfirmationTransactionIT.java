package com.easyfinance.imports.application.usecase;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.expenses.application.command.CreateImportedExpenseCommand;
import com.easyfinance.expenses.application.port.in.CreateImportedExpensePort;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.imports.application.port.in.ConfirmExpenseImportPort;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {EasyFinanceApplication.class, ExpenseImportConfirmationTransactionIT.TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
class ExpenseImportConfirmationTransactionIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ConfirmExpenseImportPort confirmExpenseImportPort;

    @Autowired
    private TestCurrentUserProvider currentUserProvider;

    @Autowired
    private FailingCreateImportedExpensePort createImportedExpensePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @AfterEach
    void reset() {
        createImportedExpensePort.reset();
        currentUserProvider.clear();
    }

    @Test
    void expenseCreationFailureRollsBackBatchConfirmationRowsAndExpenses() {
        Fixture fixture = createFixture(2);
        currentUserProvider.setParticipantId(fixture.participantId());
        createImportedExpensePort.failAfterInvocation(2);

        assertThatThrownBy(() -> confirmExpenseImportPort.confirm(fixture.accountId(), fixture.batchId()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        ex -> assertThat(ex.code()).isEqualTo("IMPORT_CONFIRMATION_FAILED"));

        Long expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expenses WHERE account_id = ? AND description LIKE 'Imported rollback%'",
                Long.class,
                fixture.accountId()
        );
        Long createdRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense_import_rows WHERE account_id = ? AND batch_id = ? AND created_expense_id IS NOT NULL",
                Long.class,
                fixture.accountId(),
                fixture.batchId()
        );
        String batchStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM expense_import_batches WHERE account_id = ? AND id = ?",
                String.class,
                fixture.accountId(),
                fixture.batchId()
        );

        assertThat(expenseCount).isZero();
        assertThat(createdRows).isZero();
        assertThat(batchStatus).isEqualTo("PREVIEW");
    }

    @Test
    void concurrentConfirmationCreatesExpensesOnlyOnce() throws Exception {
        Fixture fixture = createFixture(1);
        currentUserProvider.setParticipantId(fixture.participantId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<ConfirmAttempt> task = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                try {
                    confirmExpenseImportPort.confirm(fixture.accountId(), fixture.batchId());
                    return ConfirmAttempt.succeeded();
                } catch (Throwable ex) {
                    return ConfirmAttempt.failed(ex);
                }
            };

            Future<ConfirmAttempt> first = executor.submit(task);
            Future<ConfirmAttempt> second = executor.submit(task);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            ConfirmAttempt firstResult = first.get(20, TimeUnit.SECONDS);
            ConfirmAttempt secondResult = second.get(20, TimeUnit.SECONDS);

            assertThat(successCount(firstResult, secondResult)).isOne();
            assertThat(failure(firstResult, secondResult))
                    .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                            ex -> assertThat(ex.code()).isEqualTo("IMPORT_ALREADY_CONFIRMED"));

            Long expenseCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM expenses WHERE account_id = ? AND description LIKE 'Imported rollback%'",
                    Long.class,
                    fixture.accountId()
            );
            Long createdRows = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM expense_import_rows WHERE account_id = ? AND batch_id = ? AND created_expense_id IS NOT NULL",
                    Long.class,
                    fixture.accountId(),
                    fixture.batchId()
            );
            String batchStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM expense_import_batches WHERE account_id = ? AND id = ?",
                    String.class,
                    fixture.accountId(),
                    fixture.batchId()
            );

            assertThat(expenseCount).isOne();
            assertThat(createdRows).isOne();
            assertThat(batchStatus).isEqualTo("CONFIRMED");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void debtImportConfirmationCreatesExpenseDebtPaymentAndUpdatesDebt() {
        Fixture fixture = createDebtPaymentFixture(new BigDecimal("500.00"), "ACTIVE");
        currentUserProvider.setParticipantId(fixture.participantId());

        var response = confirmExpenseImportPort.confirm(fixture.accountId(), fixture.batchId());

        Long expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expenses WHERE account_id = ? AND description = ?",
                Long.class,
                fixture.accountId(),
                "Imported debt payment"
        );
        Long paymentId = jdbcTemplate.queryForObject(
                "SELECT created_debt_payment_id FROM expense_import_rows WHERE account_id = ? AND batch_id = ?",
                Long.class,
                fixture.accountId(),
                fixture.batchId()
        );
        BigDecimal remainingAmount = jdbcTemplate.queryForObject(
                "SELECT remaining_amount FROM debts WHERE account_id = ? AND id = ?",
                BigDecimal.class,
                fixture.accountId(),
                fixture.debtId()
        );
        Long paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM debt_payments WHERE account_id = ? AND debt_id = ? AND id = ?",
                Long.class,
                fixture.accountId(),
                fixture.debtId(),
                paymentId
        );

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(expenseCount).isOne();
        assertThat(paymentId).isNotNull();
        assertThat(paymentCount).isOne();
        assertThat(remainingAmount).isEqualByComparingTo(new BigDecimal("400.00"));
    }

    @Test
    void debtPaymentOverpaymentDuringConfirmRollsBackBatchExpenseAndPayment() {
        Fixture fixture = createDebtPaymentFixture(new BigDecimal("50.00"), "ACTIVE");
        currentUserProvider.setParticipantId(fixture.participantId());

        assertThatThrownBy(() -> confirmExpenseImportPort.confirm(fixture.accountId(), fixture.batchId()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        ex -> assertThat(ex.code()).isEqualTo("IMPORT_CONFIRMATION_FAILED"));

        assertDebtImportRollback(fixture, new BigDecimal("50.00"));
    }

    @Test
    void inactiveDebtDuringConfirmRollsBackBatchExpenseAndPayment() {
        Fixture fixture = createDebtPaymentFixture(new BigDecimal("0.00"), "PAID");
        currentUserProvider.setParticipantId(fixture.participantId());

        assertThatThrownBy(() -> confirmExpenseImportPort.confirm(fixture.accountId(), fixture.batchId()))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        ex -> assertThat(ex.code()).isEqualTo("IMPORT_CONFIRMATION_FAILED"));

        assertDebtImportRollback(fixture, new BigDecimal("0.00"));
    }

    private Fixture createFixture(int rows) {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Import tx " + System.nanoTime(),
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
                "Import Tx Category " + System.nanoTime(),
                "import-tx-category-" + System.nanoTime(),
                "EXPENSE",
                "ACTIVE"
        );
        Long paymentMethodId = jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Import Tx Cash " + System.nanoTime(),
                "import-tx-cash-" + System.nanoTime(),
                "CASH",
                "ACTIVE"
        );
        Long batchId = jdbcTemplate.queryForObject(
                """
                INSERT INTO expense_import_batches (account_id, participant_id, original_filename, status, total_rows, valid_rows, invalid_rows)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                "expenses.xlsx",
                "PREVIEW",
                rows,
                rows,
                0
        );
        for (int i = 1; i <= rows; i++) {
            jdbcTemplate.update(
                    """
                    INSERT INTO expense_import_rows
                    (account_id, batch_id, row_number, expense_date, description, amount, currency, category_name, category_id, payment_method_name, payment_method_id, payment_state, valid, errors_json)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                    """,
                    accountId,
                    batchId,
                    i + 1,
                    "2026-05-12",
                    "Imported rollback " + i,
                    new BigDecimal("100.00"),
                    "COP",
                    "Food",
                    categoryId,
                    "Cash",
                    paymentMethodId,
                    "PAID",
                    true,
                    "[]"
            );
        }
        return new Fixture(accountId, participantId, batchId, null);
    }

    private Fixture createDebtPaymentFixture(BigDecimal remainingAmount, String debtState) {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Import debt tx " + System.nanoTime(),
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
                "Import Debt Category " + System.nanoTime(),
                "import-debt-category-" + System.nanoTime(),
                "EXPENSE",
                "ACTIVE"
        );
        Long paymentMethodId = jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Import Debt Cash " + System.nanoTime(),
                "import-debt-cash-" + System.nanoTime(),
                "CASH",
                "ACTIVE"
        );
        Long debtId = jdbcTemplate.queryForObject(
                """
                INSERT INTO debts
                (account_id, participant_id, source_type, name, total_amount, scheduled_total_amount, total_currency, remaining_amount, remaining_currency, start_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                "MANUAL",
                "Import Debt",
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                "COP",
                remainingAmount,
                "COP",
                "2026-05-01",
                debtState
        );
        Long batchId = jdbcTemplate.queryForObject(
                """
                INSERT INTO expense_import_batches (account_id, participant_id, original_filename, status, total_rows, valid_rows, invalid_rows)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                "expenses.xlsx",
                "PREVIEW",
                1,
                1,
                0
        );
        jdbcTemplate.update(
                """
                INSERT INTO expense_import_rows
                (account_id, batch_id, row_number, expense_date, description, amount, currency, category_name, category_id, payment_method_name, payment_method_id, payment_state, applies_debt_payment, debt_id, debt_label, debt_payment_type, debt_payment_notes, valid, errors_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """,
                accountId,
                batchId,
                2,
                "2026-05-12",
                "Imported debt payment",
                new BigDecimal("100.00"),
                "COP",
                "Food",
                categoryId,
                "Cash",
                paymentMethodId,
                "PAID",
                true,
                debtId,
                "Import Debt | Saldo: 500.00 | Inicio: 2026-05-01 | MANUAL",
                "INSTALLMENT",
                "Imported debt note",
                true,
                "[]"
        );
        return new Fixture(accountId, participantId, batchId, debtId);
    }

    private void assertDebtImportRollback(Fixture fixture, BigDecimal expectedRemainingAmount) {
        Long expenseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expenses WHERE account_id = ? AND description = ?",
                Long.class,
                fixture.accountId(),
                "Imported debt payment"
        );
        Long paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM debt_payments WHERE account_id = ? AND debt_id = ?",
                Long.class,
                fixture.accountId(),
                fixture.debtId()
        );
        Long linkedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expense_import_rows WHERE account_id = ? AND batch_id = ? AND (created_expense_id IS NOT NULL OR created_debt_payment_id IS NOT NULL)",
                Long.class,
                fixture.accountId(),
                fixture.batchId()
        );
        String batchStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM expense_import_batches WHERE account_id = ? AND id = ?",
                String.class,
                fixture.accountId(),
                fixture.batchId()
        );
        BigDecimal remainingAmount = jdbcTemplate.queryForObject(
                "SELECT remaining_amount FROM debts WHERE account_id = ? AND id = ?",
                BigDecimal.class,
                fixture.accountId(),
                fixture.debtId()
        );

        assertThat(expenseCount).isZero();
        assertThat(paymentCount).isZero();
        assertThat(linkedRows).isZero();
        assertThat(batchStatus).isEqualTo("PREVIEW");
        assertThat(remainingAmount).isEqualByComparingTo(expectedRemainingAmount);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "import-tx-" + System.nanoTime() + "@example.com",
                "hash",
                "Import Tx User",
                "ACTIVE"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Import Tx User",
                "ACTIVE"
        );
    }

    private static int successCount(ConfirmAttempt first, ConfirmAttempt second) {
        return (first.success() ? 1 : 0) + (second.success() ? 1 : 0);
    }

    private static Throwable failure(ConfirmAttempt first, ConfirmAttempt second) {
        return first.success() ? second.error() : first.error();
    }

    private record Fixture(Long accountId, Long participantId, Long batchId, Long debtId) {
    }

    private record ConfirmAttempt(boolean success, Throwable error) {

        static ConfirmAttempt succeeded() {
            return new ConfirmAttempt(true, null);
        }

        static ConfirmAttempt failed(Throwable error) {
            return new ConfirmAttempt(false, error);
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FailingCreateImportedExpensePort failingCreateImportedExpensePort(@Qualifier("expenseManagementUseCase") CreateImportedExpensePort delegate) {
            return new FailingCreateImportedExpensePort(delegate);
        }

        @Bean
        @Primary
        TestCurrentUserProvider testCurrentUserProvider() {
            return new TestCurrentUserProvider();
        }
    }

    static class FailingCreateImportedExpensePort implements CreateImportedExpensePort {

        private final CreateImportedExpensePort delegate;
        private final AtomicInteger invocation = new AtomicInteger();
        private final AtomicInteger failAfterInvocation = new AtomicInteger(-1);

        FailingCreateImportedExpensePort(CreateImportedExpensePort delegate) {
            this.delegate = delegate;
        }

        void failAfterInvocation(int invocationNumber) {
            failAfterInvocation.set(invocationNumber);
        }

        void reset() {
            invocation.set(0);
            failAfterInvocation.set(-1);
        }

        @Override
        public ExpenseResponse createImportedExpense(CreateImportedExpenseCommand command) {
            ExpenseResponse response = delegate.createImportedExpense(command);
            if (invocation.incrementAndGet() == failAfterInvocation.get()) {
                throw new BusinessRuleViolationException("FORCED_IMPORT_FAILURE", "Forced imported expense creation failure.");
            }
            return response;
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
            return Optional.of(new CurrentUser(1L, participantId.get(), "import-tx@example.com", Set.of("USER"), true));
        }
    }
}
