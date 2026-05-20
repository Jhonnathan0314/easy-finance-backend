package com.easyfinance.income.infrastructure.persistence;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.income.application.port.out.IncomeRepositoryPort;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.domain.model.IncomeStatus;
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
class IncomesSchemaIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IncomeRepositoryPort incomeRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void validIncomeWithSameAccountCategoryAndParticipantMembershipWorks() {
        var fixture = createFixture();

        assertThatCode(() -> insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), 2500000, "COP", "ACTIVE"))
                .doesNotThrowAnyException();
    }

    @Test
    void incomeRequiresPositiveCopAmount() {
        var fixture = createFixture();

        assertThatThrownBy(() -> insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), 0, "COP", "ACTIVE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void incomeRejectsUnsupportedCurrency() {
        var fixture = createFixture();

        assertThatThrownBy(() -> insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), 2500000, "USD", "ACTIVE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void incomeRejectsInvalidStatus() {
        var fixture = createFixture();

        assertThatThrownBy(() -> insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), 2500000, "COP", "DELETED"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void incomeRejectsCategoryFromAnotherAccount() {
        var fixture = createFixture();
        var other = createFixture();

        assertThatThrownBy(() -> insertIncome(fixture.accountId(), other.categoryId(), fixture.participantId(), 2500000, "COP", "ACTIVE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void incomeRejectsParticipantWithoutAccountMembership() {
        var fixture = createFixture();
        var otherParticipantId = createParticipant();

        assertThatThrownBy(() -> insertIncome(fixture.accountId(), fixture.categoryId(), otherParticipantId, 2500000, "COP", "ACTIVE"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void listIncomesSearchMatchesDescriptionCaseInsensitiveAndKeepsAccountScope() {
        var fixture = createFixture();
        var other = createFixture();
        insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), "Nomina Mayo", 2500000, "COP", "ACTIVE");
        insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), "Freelance", 1000000, "COP", "ACTIVE");
        insertIncome(other.accountId(), other.categoryId(), other.participantId(), "Nomina Otra Cuenta", 2500000, "COP", "ACTIVE");

        var page = incomeRepository.findAll(new ListIncomesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                "nomina",
                PageQuery.of(0, 20),
                "incomeDate,asc"
        ));

        assertThat(page.content()).extracting("description").containsExactly("Nomina Mayo");
    }

    @Test
    void listIncomesBlankSearchIsIgnoredAndSearchCombinesWithStatus() {
        var fixture = createFixture();
        insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), "Nomina Activa", 2500000, "COP", "ACTIVE");
        insertIncome(fixture.accountId(), fixture.categoryId(), fixture.participantId(), "Nomina Cancelada", 2500000, "COP", "CANCELLED");

        var blankSearchPage = incomeRepository.findAll(new ListIncomesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                null,
                "   ",
                PageQuery.of(0, 20),
                "incomeDate,asc"
        ));
        var filteredPage = incomeRepository.findAll(new ListIncomesQuery(
                fixture.accountId(),
                null,
                null,
                null,
                null,
                IncomeStatus.CANCELLED,
                "NOMina",
                PageQuery.of(0, 20),
                "incomeDate,asc"
        ));

        assertThat(blankSearchPage.content()).hasSize(1);
        assertThat(filteredPage.content()).extracting("description").containsExactly("Nomina Cancelada");
    }

    private Fixture createFixture() {
        Long participantId = createParticipant();
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Income " + System.nanoTime(),
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
                "Salary " + System.nanoTime(),
                "salary-" + System.nanoTime(),
                "INCOME",
                "ACTIVE"
        );
        return new Fixture(accountId, categoryId, participantId);
    }

    private Long createParticipant() {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "income-" + System.nanoTime() + "@example.com",
                "hash",
                "Income User",
                "ACTIVE"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Income User",
                "ACTIVE"
        );
    }

    private void insertIncome(Long accountId, Long categoryId, Long participantId, int amount, String currency, String status) {
        insertIncome(accountId, categoryId, participantId, "Salary", amount, currency, status);
    }

    private void insertIncome(
            Long accountId,
            Long categoryId,
            Long participantId,
            String description,
            int amount,
            String currency,
            String status
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO incomes (account_id, category_id, participant_id, description, amount, currency, income_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                accountId,
                categoryId,
                participantId,
                description,
                amount,
                currency,
                "2026-05-10",
                status
        );
    }

    private record Fixture(Long accountId, Long categoryId, Long participantId) {
    }
}
