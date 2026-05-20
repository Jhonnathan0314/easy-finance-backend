package com.easyfinance.imports.infrastructure.persistence;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class ExpenseImportsSchemaIT {

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
    void importTablesEnforceScopedBatchRowsAndJsonErrors() {
        Fixture fixture = createFixture();
        Long batchId = jdbcTemplate.queryForObject(
                "INSERT INTO expense_import_batches (account_id, participant_id, original_filename, status, total_rows, valid_rows, invalid_rows) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                fixture.accountId(),
                fixture.participantId(),
                "expenses.xlsx",
                "PREVIEW",
                1,
                0,
                1
        );

        jdbcTemplate.update(
                "INSERT INTO expense_import_rows (account_id, batch_id, row_number, category_name, payment_method_name, valid, errors_json) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)",
                fixture.accountId(),
                batchId,
                2,
                "Missing",
                "Cash",
                false,
                "[{\"column\":\"Categoría\",\"code\":\"CATEGORY_NOT_FOUND\",\"message\":\"Missing\"}]"
        );

        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM expense_import_rows WHERE account_id = ? AND batch_id = ?", Integer.class, fixture.accountId(), batchId);
        assertThat(rows).isEqualTo(1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO expense_import_rows (account_id, batch_id, row_number, valid) VALUES (?, ?, ?, ?)",
                fixture.otherAccountId(),
                batchId,
                2,
                true
        )).isInstanceOf(Exception.class);
    }

    @Test
    void importRowsCanReferenceDebtPaymentContractFields() {
        Fixture fixture = createFixture();
        Long debtId = jdbcTemplate.queryForObject(
                """
                INSERT INTO debts (account_id, participant_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, start_date, state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                fixture.accountId(),
                fixture.participantId(),
                "MANUAL",
                "Loan",
                "1000.00",
                "COP",
                "1000.00",
                "COP",
                "2026-05-01",
                "ACTIVE"
        );
        Long paymentId = jdbcTemplate.queryForObject(
                """
                INSERT INTO debt_payments (account_id, debt_id, participant_id, payment_type, amount, currency, payment_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """,
                Long.class,
                fixture.accountId(),
                debtId,
                fixture.participantId(),
                "INSTALLMENT",
                "100.00",
                "COP",
                "2026-05-10",
                "ACTIVE"
        );
        Long batchId = jdbcTemplate.queryForObject(
                "INSERT INTO expense_import_batches (account_id, participant_id, original_filename, status, total_rows, valid_rows, invalid_rows) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                fixture.accountId(),
                fixture.participantId(),
                "expenses.xlsx",
                "PREVIEW",
                1,
                1,
                0
        );

        jdbcTemplate.update(
                """
                INSERT INTO expense_import_rows
                (account_id, batch_id, row_number, applies_debt_payment, debt_id, debt_label, debt_payment_type, debt_payment_notes, valid, errors_json, created_debt_payment_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                """,
                fixture.accountId(),
                batchId,
                2,
                true,
                debtId,
                "Loan | Saldo: 1000.00 | Inicio: 2026-05-01 | MANUAL",
                "INSTALLMENT",
                "Imported payment",
                true,
                "[]",
                paymentId
        );

        Boolean appliesDebtPayment = jdbcTemplate.queryForObject(
                "SELECT applies_debt_payment FROM expense_import_rows WHERE account_id = ? AND batch_id = ?",
                Boolean.class,
                fixture.accountId(),
                batchId
        );

        assertThat(appliesDebtPayment).isTrue();
    }

    private Fixture createFixture() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "imports-schema-" + System.nanoTime() + "@example.com",
                "hash",
                "Import User",
                "ACTIVE"
        );
        Long participantId = jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Import User",
                "ACTIVE"
        );
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Imports " + System.nanoTime(),
                "ACTIVE"
        );
        Long otherAccountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Imports Other " + System.nanoTime(),
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_MEMBER",
                "ACTIVE"
        );
        return new Fixture(accountId, otherAccountId, participantId);
    }

    private record Fixture(Long accountId, Long otherAccountId, Long participantId) {
    }
}
