package com.easyfinance.expenses.infrastructure.persistence;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.application.PageQuery;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class ExpensesSchemaIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExpenseRepositoryPort expenseRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void expenseRequiresPositiveCopAmount() {
        var fixture = createFixture();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                fixture.participantId(),
                "Lunch",
                0,
                "COP",
                "2026-05-09",
                "PAID",
                "ACTIVE",
                "SIMPLE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void expenseRejectsUnsupportedCurrency() {
        var fixture = createFixture();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                fixture.participantId(),
                "Lunch",
                12000,
                "USD",
                "2026-05-09",
                "PAID",
                "ACTIVE",
                "SIMPLE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void validExpenseWithSameAccountCatalogsAndParticipantMembershipWorks() {
        var fixture = createFixture();

        assertThatCode(() -> insertExpense(
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                fixture.participantId()
        )).doesNotThrowAnyException();
    }

    @Test
    void expenseRejectsCategoryFromAnotherAccount() {
        var fixture = createFixture();
        var other = createFixture();

        assertThatThrownBy(() -> insertExpense(
                fixture.accountId(),
                other.categoryId(),
                fixture.paymentMethodId(),
                fixture.participantId()
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void expenseRejectsPaymentMethodFromAnotherAccount() {
        var fixture = createFixture();
        var other = createFixture();

        assertThatThrownBy(() -> insertExpense(
                fixture.accountId(),
                fixture.categoryId(),
                other.paymentMethodId(),
                fixture.participantId()
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void expenseRejectsParticipantWithoutAccountMembership() {
        var fixture = createFixture();
        var otherParticipantId = createParticipant();

        assertThatThrownBy(() -> insertExpense(
                fixture.accountId(),
                fixture.categoryId(),
                fixture.paymentMethodId(),
                otherParticipantId
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void listExpensesSearchMatchesDescriptionCaseInsensitiveAndKeepsAccountScope() {
        var fixture = createFixture();
        var other = createFixture();
        insertExpense(fixture.accountId(), fixture.categoryId(), fixture.paymentMethodId(), fixture.participantId(), "Mercado Junio", "PAID", "ACTIVE");
        insertExpense(fixture.accountId(), fixture.categoryId(), fixture.paymentMethodId(), fixture.participantId(), "Taxi", "PAID", "ACTIVE");
        insertExpense(other.accountId(), other.categoryId(), other.paymentMethodId(), other.participantId(), "Mercado Otra Cuenta", "PAID", "ACTIVE");

        var page = expenseRepository.findAll(new ListExpensesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "mercado",
                PageQuery.of(0, 20),
                "expenseDate,asc"
        ));

        assertThat(page.content()).extracting("description").containsExactly("Mercado Junio");
    }

    @Test
    void listExpensesBlankSearchIsIgnoredAndSearchCombinesWithPaymentState() {
        var fixture = createFixture();
        insertExpense(fixture.accountId(), fixture.categoryId(), fixture.paymentMethodId(), fixture.participantId(), "Mercado Pagado", "PAID", "ACTIVE");
        insertExpense(fixture.accountId(), fixture.categoryId(), fixture.paymentMethodId(), fixture.participantId(), "Mercado Pendiente", "PENDING", "ACTIVE");

        var blankSearchPage = expenseRepository.findAll(new ListExpensesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "   ",
                PageQuery.of(0, 20),
                "expenseDate,asc"
        ));
        var filteredPage = expenseRepository.findAll(new ListExpensesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                ExpensePaymentState.PAID,
                null,
                null,
                "MERcado",
                PageQuery.of(0, 20),
                "expenseDate,asc"
        ));

        assertThat(blankSearchPage.content()).hasSize(2);
        assertThat(filteredPage.content()).extracting("description").containsExactly("Mercado Pagado");
    }

    @Test
    void listExpensesCanFilterByExpenseType() {
        var fixture = createFixture();
        var other = createFixture();
        insertExpense(fixture.accountId(), fixture.categoryId(), fixture.paymentMethodId(), fixture.participantId(), "Simple", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.categoryId(), fixture.paymentMethodId(), fixture.participantId(), "Installment", "PENDING", "ACTIVE", "INSTALLMENT");
        insertExpense(other.accountId(), other.categoryId(), other.paymentMethodId(), other.participantId(), "Other Installment", "PENDING", "ACTIVE", "INSTALLMENT");

        var allTypesPage = expenseRepository.findAll(new ListExpensesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageQuery.of(0, 20),
                "expenseDate,asc"
        ));
        var simplePage = expenseRepository.findAll(new ListExpensesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExpenseType.SIMPLE,
                null,
                PageQuery.of(0, 20),
                "expenseDate,asc"
        ));
        var installmentPage = expenseRepository.findAll(new ListExpensesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ExpenseType.INSTALLMENT,
                null,
                PageQuery.of(0, 20),
                "expenseDate,asc"
        ));

        assertThat(allTypesPage.content()).extracting("description").containsExactlyInAnyOrder("Simple", "Installment");
        assertThat(simplePage.content()).extracting("description").containsExactly("Simple");
        assertThat(installmentPage.content()).extracting("description").containsExactly("Installment");
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Expenses " + System.nanoTime(),
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
                "Food " + System.nanoTime(),
                "food-" + System.nanoTime(),
                "EXPENSE",
                "ACTIVE"
        );
        Long paymentMethodId = jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Cash " + System.nanoTime(),
                "cash-" + System.nanoTime(),
                "CASH",
                "ACTIVE"
        );
        return new Fixture(accountId, categoryId, paymentMethodId, participantId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "expense-" + System.nanoTime() + "@example.com",
                "hash",
                "Expense User",
                "ACTIVE"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Expense User",
                "ACTIVE"
        );
    }

    private void insertExpense(Long accountId, Long categoryId, Long paymentMethodId, Long participantId) {
        insertExpense(accountId, categoryId, paymentMethodId, participantId, "Lunch", "PAID", "ACTIVE");
    }

    private void insertExpense(
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            String paymentState,
            String status
    ) {
        insertExpense(accountId, categoryId, paymentMethodId, participantId, description, paymentState, status, "SIMPLE");
    }

    private void insertExpense(
            Long accountId,
            Long categoryId,
            Long paymentMethodId,
            Long participantId,
            String description,
            String paymentState,
            String status,
            String expenseType
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                description,
                12000,
                "COP",
                "2026-05-09",
                paymentState,
                status,
                expenseType
        );
    }

    private record Fixture(Long accountId, Long categoryId, Long paymentMethodId, Long participantId) {
    }
}
