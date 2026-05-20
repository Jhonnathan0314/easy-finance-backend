package com.easyfinance.debts.infrastructure.persistence;

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
class DebtsSchemaIT {

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
    void validManualDebtWorks() {
        Fixture fixture = createFixture();

        assertThatCode(() -> insertManualDebt(fixture.accountId(), fixture.participantId()))
                .doesNotThrowAnyException();
    }

    @Test
    void derivedDebtRequiresOriginExpenseFromSameAccount() {
        Fixture fixture = createFixture();
        Fixture other = createFixture();

        assertThatThrownBy(() -> insertDerivedDebt(fixture.accountId(), fixture.participantId(), other.expenseId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void debtRejectsParticipantWithoutAccountMembership() {
        Fixture fixture = createFixture();
        Long otherParticipantId = createParticipant();

        assertThatThrownBy(() -> insertManualDebt(fixture.accountId(), otherParticipantId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void originExpenseCanHaveOnlyOneDebt() {
        Fixture fixture = createFixture();
        insertDerivedDebt(fixture.accountId(), fixture.participantId(), fixture.expenseId());

        assertThatThrownBy(() -> insertDerivedDebt(fixture.accountId(), fixture.participantId(), fixture.expenseId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void installmentSourceRequiresInstallmentFields() {
        Fixture fixture = createFixture();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO debts (account_id, participant_id, origin_expense_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, start_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fixture.accountId(),
                fixture.participantId(),
                fixture.expenseId(),
                "INSTALLMENT_EXPENSE",
                "Bad debt",
                1200000,
                "COP",
                1200000,
                "COP",
                "2026-06-01",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void validDebtPaymentWorks() {
        Fixture fixture = createFixture();
        Long debtId = insertManualDebtReturningId(fixture.accountId(), fixture.participantId());

        assertThatCode(() -> insertDebtPayment(fixture.accountId(), debtId, fixture.participantId()))
                .doesNotThrowAnyException();
    }

    @Test
    void paymentRejectsDebtFromAnotherAccount() {
        Fixture fixture = createFixture();
        Fixture other = createFixture();
        Long otherDebtId = insertManualDebtReturningId(other.accountId(), other.participantId());

        assertThatThrownBy(() -> insertDebtPayment(fixture.accountId(), otherDebtId, fixture.participantId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void paymentRejectsParticipantWithoutAccountMembership() {
        Fixture fixture = createFixture();
        Long debtId = insertManualDebtReturningId(fixture.accountId(), fixture.participantId());
        Long otherParticipantId = createParticipant();

        assertThatThrownBy(() -> insertDebtPayment(fixture.accountId(), debtId, otherParticipantId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void paymentRejectsInvalidAmount() {
        Fixture fixture = createFixture();
        Long debtId = insertManualDebtReturningId(fixture.accountId(), fixture.participantId());

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO debt_payments (account_id, debt_id, participant_id, payment_type, amount, currency, payment_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fixture.accountId(),
                debtId,
                fixture.participantId(),
                "INSTALLMENT",
                0,
                "COP",
                "2026-05-11",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    private void insertManualDebt(Long accountId, Long participantId) {
        jdbcTemplate.update(
                """
                INSERT INTO debts (account_id, participant_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, start_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId,
                participantId,
                "MANUAL",
                "Manual debt",
                100000,
                "COP",
                100000,
                "COP",
                "2026-05-11",
                "ACTIVE"
        );
    }

    private Long insertManualDebtReturningId(Long accountId, Long participantId) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO debts (account_id, participant_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, start_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                accountId,
                participantId,
                "MANUAL",
                "Manual debt",
                100000,
                "COP",
                100000,
                "COP",
                "2026-05-11",
                "ACTIVE"
        );
    }

    private void insertDebtPayment(Long accountId, Long debtId, Long participantId) {
        jdbcTemplate.update(
                """
                INSERT INTO debt_payments (account_id, debt_id, participant_id, payment_type, amount, currency, payment_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId,
                debtId,
                participantId,
                "INSTALLMENT",
                50000,
                "COP",
                "2026-05-11",
                "ACTIVE"
        );
    }

    private void insertDerivedDebt(Long accountId, Long participantId, Long expenseId) {
        jdbcTemplate.update(
                """
                INSERT INTO debts (account_id, participant_id, origin_expense_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, installment_count, installment_amount, installment_currency, start_date, end_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId,
                participantId,
                expenseId,
                "INSTALLMENT_EXPENSE",
                "Laptop",
                1200000,
                "COP",
                1200000,
                "COP",
                6,
                200000,
                "COP",
                "2026-06-01",
                "2026-12-01",
                "ACTIVE"
        );
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Debts " + System.nanoTime(),
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
                "Installments " + System.nanoTime(),
                "installments-" + System.nanoTime(),
                "EXPENSE",
                "ACTIVE"
        );
        Long paymentMethodId = jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Card " + System.nanoTime(),
                "card-" + System.nanoTime(),
                "CREDIT_CARD",
                "ACTIVE"
        );
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
                1200000,
                "COP",
                "2026-05-11",
                "PENDING",
                "ACTIVE",
                "INSTALLMENT"
        );
        return new Fixture(accountId, participantId, expenseId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "debt-" + System.nanoTime() + "@example.com",
                "hash",
                "Debt User",
                "ACTIVE"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Debt User",
                "ACTIVE"
        );
    }

    private record Fixture(Long accountId, Long participantId, Long expenseId) {
    }
}
