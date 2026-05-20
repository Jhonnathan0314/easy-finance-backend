package com.easyfinance.catalogs.infrastructure.persistence;

import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class CatalogsSchemaIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CategoryRepositoryPort categoryRepository;

    @Autowired
    private PaymentMethodRepositoryPort paymentMethodRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void activeCategoriesAreUniqueByAccountTypeAndName() {
        Long accountId = createAccount("Catalog Account");
        jdbcTemplate.update(
                "INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?)",
                accountId,
                "Food",
                "food",
                "EXPENSE",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?)",
                accountId,
                "FOOD",
                "food",
                "EXPENSE",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void inactiveCategoryDoesNotBlockRecreation() {
        Long accountId = createAccount("Catalog Soft Delete");
        jdbcTemplate.update(
                "INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?)",
                accountId,
                "Food",
                "food",
                "EXPENSE",
                "INACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?)",
                accountId,
                "Food",
                "food",
                "EXPENSE",
                "ACTIVE"
        );
    }

    @Test
    void activePaymentMethodsAreUniqueByAccountAndName() {
        Long accountId = createAccount("Payment Account");
        jdbcTemplate.update(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?)",
                accountId,
                "Cash",
                "cash",
                "CASH",
                "ACTIVE"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?)",
                accountId,
                "CASH",
                "cash",
                "CASH",
                "ACTIVE"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void categoriesSearchMatchesNameAndDescriptionWithFiltersAndAccountIsolation() {
        Long accountId = createAccount("Category Search");
        Long otherAccountId = createAccount("Other Category Search");
        insertCategory(accountId, "Mercado Uno", "mercado uno", "Compra semanal", "EXPENSE", "ACTIVE");
        insertCategory(accountId, "Groceries", "groceries", "Pago en mercado local", "EXPENSE", "ACTIVE");
        insertCategory(accountId, "Mercado Inactivo", "mercado inactivo", "Hidden", "EXPENSE", "INACTIVE");
        insertCategory(accountId, "Mercado Income", "mercado income", "Hidden", "INCOME", "ACTIVE");
        insertCategory(otherAccountId, "Mercado Otro", "mercado otro", "Hidden", "EXPENSE", "ACTIVE");

        var page = categoryRepository.findAll(new ListCategoriesQuery(
                accountId,
                CategoryType.EXPENSE,
                CatalogStatus.ACTIVE,
                "MERCADO",
                PageQuery.of(0, 20),
                "name,asc"
        ));

        assertThat(page.content()).extracting("name").containsExactly("Groceries", "Mercado Uno");
    }

    @Test
    void paymentMethodsSearchIsCaseInsensitiveAndBlankSearchIsIgnored() {
        Long accountId = createAccount("Payment Search");
        Long otherAccountId = createAccount("Other Payment Search");
        insertPaymentMethod(accountId, "Visa Gold", "visa gold", "Tarjeta principal", "CREDIT_CARD", "ACTIVE");
        insertPaymentMethod(accountId, "Wallet", "wallet", "Pago con VISA virtual", "DIGITAL_WALLET", "ACTIVE");
        insertPaymentMethod(accountId, "Visa Inactive", "visa inactive", "Hidden", "CREDIT_CARD", "INACTIVE");
        insertPaymentMethod(otherAccountId, "Visa Other", "visa other", "Hidden", "CREDIT_CARD", "ACTIVE");

        var searchPage = paymentMethodRepository.findAll(new ListPaymentMethodsQuery(
                accountId,
                null,
                CatalogStatus.ACTIVE,
                "vIsA",
                PageQuery.of(0, 20),
                "name,asc"
        ));
        var blankSearchPage = paymentMethodRepository.findAll(new ListPaymentMethodsQuery(
                accountId,
                null,
                CatalogStatus.ACTIVE,
                "   ",
                PageQuery.of(0, 20),
                "name,asc"
        ));

        assertThat(searchPage.content()).extracting("name").containsExactly("Visa Gold", "Wallet");
        assertThat(blankSearchPage.content()).extracting("name").containsExactly("Visa Gold", "Wallet");
    }

    @Test
    void templateCatalogQueriesReturnOnlyActiveExpenseCategoriesAndActivePaymentMethods() {
        Long accountId = createAccount("Template Catalog");
        Long otherAccountId = createAccount("Other Template Catalog");
        insertCategory(accountId, "Food", "food", null, "EXPENSE", "ACTIVE");
        insertCategory(accountId, "Salary", "salary", null, "INCOME", "ACTIVE");
        insertCategory(accountId, "Old Food", "old food", null, "EXPENSE", "INACTIVE");
        insertCategory(otherAccountId, "Other Food", "other food", null, "EXPENSE", "ACTIVE");
        insertPaymentMethod(accountId, "Cash", "cash", null, "CASH", "ACTIVE");
        insertPaymentMethod(accountId, "Old Card", "old card", null, "CREDIT_CARD", "INACTIVE");
        insertPaymentMethod(otherAccountId, "Other Cash", "other cash", null, "CASH", "ACTIVE");

        var categories = categoryRepository.findActiveExpenseByAccountId(accountId);
        var paymentMethods = paymentMethodRepository.findActiveByAccountId(accountId);

        assertThat(categories).extracting("name").containsExactly("Food");
        assertThat(paymentMethods).extracting("name").containsExactly("Cash");
    }

    private Long createAccount(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                name,
                "ACTIVE"
        );
    }

    private void insertCategory(Long accountId, String name, String normalizedName, String description, String type, String status) {
        jdbcTemplate.update(
                "INSERT INTO categories (account_id, name, normalized_name, description, type, status) VALUES (?, ?, ?, ?, ?, ?)",
                accountId,
                name,
                normalizedName,
                description,
                type,
                status
        );
    }

    private void insertPaymentMethod(Long accountId, String name, String normalizedName, String description, String type, String status) {
        jdbcTemplate.update(
                "INSERT INTO payment_methods (account_id, name, normalized_name, description, type, status) VALUES (?, ?, ?, ?, ?, ?)",
                accountId,
                name,
                normalizedName,
                description,
                type,
                status
        );
    }
}
