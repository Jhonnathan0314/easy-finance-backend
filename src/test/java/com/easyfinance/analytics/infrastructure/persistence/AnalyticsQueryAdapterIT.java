package com.easyfinance.analytics.infrastructure.persistence;

import com.easyfinance.analytics.application.port.out.AnalyticsQueryPort;
import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.query.CashflowSummaryQuery;
import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.bootstrap.EasyFinanceApplication;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.income.domain.model.IncomeStatus;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EasyFinanceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class AnalyticsQueryAdapterIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnalyticsQueryPort analyticsQueryPort;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void monthlySummaryAggregatesOnlyRequestedAccount() {
        Fixture fixture = createFixture("main");
        Fixture other = createFixture("other");
        seedMonthlyData(fixture, 1000, 200, 100, 50);
        seedMonthlyData(other, 9000, 8000, 7000, 6000);
        Long cancelledDebtId = insertDebt(fixture.accountId(), fixture.participantId(), "CANCELLED", "MANUAL", 999, 999);
        insertDebtPayment(fixture.accountId(), cancelledDebtId, fixture.participantId(), 999, "2026-05-15");

        var summary = analyticsQueryPort.getMonthlySummary(
                fixture.accountId(),
                2026,
                5,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(summary.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(summary.totalExpenses()).isEqualByComparingTo("200.00");
        assertThat(summary.netBalance()).isEqualByComparingTo("800.00");
        assertThat(summary.totalDebtRemaining()).isEqualByComparingTo("100.00");
        assertThat(summary.totalDebtPaidInMonth()).isEqualByComparingTo("50.00");
        assertThat(summary.budgetExpected()).isEqualByComparingTo("100.00");
        assertThat(summary.budgetPaid()).isEqualByComparingTo("50.00");
        assertThat(summary.budgetPending()).isEqualByComparingTo("50.00");
    }

    @Test
    void categoryBreakdownsAreGroupedAndSortedByAmount() {
        Fixture fixture = createFixture("breakdown");
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), 1000, "2026-05-10");
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), 500, "2026-05-11");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 200, "2026-05-12");

        var incomes = analyticsQueryPort.getIncomesByCategory(new IncomeBreakdownQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null));
        var expenses = analyticsQueryPort.getExpensesByCategory(new ExpenseBreakdownQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null, null, null, null));

        assertThat(incomes).singleElement().satisfies(item -> {
            assertThat(item.categoryName()).startsWith("Income");
            assertThat(item.amount()).isEqualByComparingTo("1500.00");
            assertThat(item.count()).isEqualTo(2L);
        });
        assertThat(expenses).singleElement().satisfies(item -> {
            assertThat(item.categoryName()).startsWith("Expense");
            assertThat(item.amount()).isEqualByComparingTo("200.00");
        });
    }

    @Test
    void incomesByCategoryWithoutStatusDefaultsToActiveAndExplicitStatusWorks() {
        Fixture fixture = createFixture("income-status");
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), 1000, "2026-05-10", "ACTIVE");
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), 700, "2026-05-11", "CANCELLED");

        var activeByDefault = analyticsQueryPort.getIncomesByCategory(new IncomeBreakdownQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null));
        var cancelledExplicit = analyticsQueryPort.getIncomesByCategory(new IncomeBreakdownQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, IncomeStatus.CANCELLED));

        assertThat(activeByDefault).singleElement().satisfies(item -> {
            assertThat(item.amount()).isEqualByComparingTo("1000.00");
            assertThat(item.count()).isEqualTo(1L);
        });
        assertThat(cancelledExplicit).singleElement().satisfies(item -> {
            assertThat(item.amount()).isEqualByComparingTo("700.00");
            assertThat(item.count()).isEqualTo(1L);
        });
    }

    @Test
    void debtSummaryExcludesCancelledFromFinancialTotalsButCountsThem() {
        Fixture fixture = createFixture("debts");
        Long activeDebtId = insertDebt(fixture.accountId(), fixture.participantId(), "ACTIVE", "MANUAL", 500, 200);
        insertInstallmentDebt(fixture, "PAID", 300, 0);
        Long cancelledDebtId = insertDebt(fixture.accountId(), fixture.participantId(), "CANCELLED", "MANUAL", 900, 900);
        insertDebtPayment(fixture.accountId(), activeDebtId, fixture.participantId(), 125, "2026-05-12");
        insertDebtPayment(fixture.accountId(), cancelledDebtId, fixture.participantId(), 900, "2026-05-13");

        var summary = analyticsQueryPort.getDebtSummary(fixture.accountId());

        assertThat(summary.activeDebtsCount()).isEqualTo(1L);
        assertThat(summary.paidDebtsCount()).isEqualTo(1L);
        assertThat(summary.cancelledDebtsCount()).isEqualTo(1L);
        assertThat(summary.totalDebtAmount()).isEqualByComparingTo("800.00");
        assertThat(summary.totalRemainingBalance()).isEqualByComparingTo("200.00");
        assertThat(summary.totalPaidAmount()).isEqualByComparingTo("125.00");
    }

    @Test
    void budgetSummaryWithoutBudgetReturnsZeros() {
        Fixture fixture = createFixture("empty-budget");

        var summary = analyticsQueryPort.getBudgetSummary(fixture.accountId(), 2026, 5);

        assertThat(summary.budgetId()).isNull();
        assertThat(summary.expectedAmount()).isZero();
        assertThat(summary.impactsCount()).isZero();
    }

    @Test
    void budgetVsExpensesByCategoryReturnsSpentCategoryWhenMonthlyBudgetDoesNotExist() {
        Fixture fixture = createFixture("budget-vs-no-budget");
        Fixture other = createFixture("budget-vs-no-budget-other");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 250, "2026-05-12", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 999, "2026-05-13", "PAID", "CANCELLED", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 999, "2026-06-01", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(other.accountId(), other.expenseCategoryId(), other.paymentMethodId(), other.participantId(), 999, "2026-05-12", "PAID", "ACTIVE", "SIMPLE");

        var items = analyticsQueryPort.getBudgetVsExpensesByCategory(
                fixture.accountId(),
                2026,
                5,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.categoryId()).isEqualTo(fixture.expenseCategoryId());
            assertThat(item.budgetedAmount()).isZero();
            assertThat(item.spentAmount()).isEqualByComparingTo("250.00");
            assertThat(item.remainingAmount()).isEqualByComparingTo("-250.00");
            assertThat(item.executionPercentage()).isNull();
        });
    }

    @Test
    void budgetVsExpensesByCategoryCombinesBudgetedAndSpentWithoutCrossAccountLeak() {
        Fixture fixture = createFixture("budget-vs-expenses");
        Fixture other = createFixture("budget-vs-expenses-other");
        Long transportCategoryId = insertCategory(fixture.accountId(), "Transport " + System.nanoTime(), "EXPENSE");
        Long healthCategoryId = insertCategory(fixture.accountId(), "Health " + System.nanoTime(), "EXPENSE");
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long otherBudgetId = insertBudget(other.accountId(), 2026, 5);

        insertManualSubBudget(fixture.accountId(), budgetId, fixture.expenseCategoryId(), "Food", 500, "ACTIVE", "MANUAL");
        insertManualSubBudget(fixture.accountId(), budgetId, transportCategoryId, "Transport", 300, "ACTIVE", "MANUAL");
        insertManualSubBudget(fixture.accountId(), budgetId, healthCategoryId, "Inactive health", 999, "INACTIVE", "MANUAL");
        insertManualSubBudget(fixture.accountId(), budgetId, healthCategoryId, "Debt derived health", 999, "ACTIVE", "DEBT_DERIVED");
        insertManualSubBudget(fixture.accountId(), budgetId, null, "No category", 999, "ACTIVE", "MANUAL");
        insertManualSubBudget(other.accountId(), otherBudgetId, other.expenseCategoryId(), "Other", 999, "ACTIVE", "MANUAL");

        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 200, "2026-05-12", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), healthCategoryId, fixture.paymentMethodId(), fixture.participantId(), 150, "2026-05-13", "PENDING", "ACTIVE", "INSTALLMENT");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 999, "2026-05-14", "PAID", "CANCELLED", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 999, "2026-06-01", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(other.accountId(), other.expenseCategoryId(), other.paymentMethodId(), other.participantId(), 999, "2026-05-12", "PAID", "ACTIVE", "SIMPLE");

        var items = analyticsQueryPort.getBudgetVsExpensesByCategory(
                fixture.accountId(),
                2026,
                5,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(items).hasSize(3);
        assertThat(items).filteredOn(item -> item.categoryId().equals(fixture.expenseCategoryId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.budgetedAmount()).isEqualByComparingTo("500.00");
                    assertThat(item.spentAmount()).isEqualByComparingTo("200.00");
                    assertThat(item.remainingAmount()).isEqualByComparingTo("300.00");
                    assertThat(item.executionPercentage()).isEqualByComparingTo("40.00");
                });
        assertThat(items).filteredOn(item -> item.categoryId().equals(transportCategoryId))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.budgetedAmount()).isEqualByComparingTo("300.00");
                    assertThat(item.spentAmount()).isZero();
                    assertThat(item.remainingAmount()).isEqualByComparingTo("300.00");
                    assertThat(item.executionPercentage()).isZero();
                });
        assertThat(items).filteredOn(item -> item.categoryId().equals(healthCategoryId))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.budgetedAmount()).isZero();
                    assertThat(item.spentAmount()).isEqualByComparingTo("150.00");
                    assertThat(item.remainingAmount()).isEqualByComparingTo("-150.00");
                    assertThat(item.executionPercentage()).isNull();
                });
    }

    @Test
    void cashflowSummaryUsesOnlyRealMoneyMovements() {
        Fixture fixture = createFixture("cashflow-summary");
        Fixture other = createFixture("cashflow-other");
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), 1000, "2026-05-10");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 200, "2026-05-11", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 300, "2026-05-12", "PENDING", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 400, "2026-05-13", "PARTIAL", "ACTIVE", "SIMPLE");
        Long activeInstallmentDebt = insertInstallmentDebt(fixture, "ACTIVE", 900, 750);
        Long debtPaymentId = insertDebtPaymentReturningId(fixture.accountId(), activeInstallmentDebt, fixture.participantId(), 150, "2026-05-14");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 150, "2026-05-14", "PAID", "ACTIVE", "SIMPLE", "DEBT_PAYMENT", debtPaymentId);
        Long cancelledDebt = insertDebt(fixture.accountId(), fixture.participantId(), "CANCELLED", "MANUAL", 500, 500);
        insertDebtPayment(fixture.accountId(), cancelledDebt, fixture.participantId(), 500, "2026-05-15");
        seedMonthlyData(other, 9000, 8000, 7000, 6000);

        var summary = analyticsQueryPort.getCashflowSummary(new CashflowSummaryQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null));

        assertThat(summary.totalIncome()).isEqualByComparingTo("1000.00");
        assertThat(summary.totalSimpleExpenseOutflow()).isEqualByComparingTo("200.00");
        assertThat(summary.totalDebtPaymentOutflow()).isEqualByComparingTo("150.00");
        assertThat(summary.totalOutflow()).isEqualByComparingTo("350.00");
        assertThat(summary.netCashflow()).isEqualByComparingTo("650.00");
    }

    @Test
    void expenseSummaryIsConceptualAndIncludesInstallmentPurchases() {
        Fixture fixture = createFixture("expense-summary");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 200, "2026-05-11", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 300, "2026-05-12", "PENDING", "ACTIVE", "SIMPLE");
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), "ACTIVE", "MANUAL", 500, 350);
        Long debtPaymentId = insertDebtPaymentReturningId(fixture.accountId(), debtId, fixture.participantId(), 150, "2026-05-13");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 150, "2026-05-13", "PAID", "ACTIVE", "SIMPLE", "DEBT_PAYMENT", debtPaymentId);
        insertInstallmentDebt(fixture, "ACTIVE", 900, 900);

        var summary = analyticsQueryPort.getExpenseSummary(new ExpenseSummaryQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null, null, null, null));
        var installmentOnly = analyticsQueryPort.getExpenseSummary(new ExpenseSummaryQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null, ExpenseType.INSTALLMENT, null, null));
        var paidOnly = analyticsQueryPort.getExpenseSummary(new ExpenseSummaryQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null, null, ExpensePaymentState.PAID, null));

        assertThat(summary.totalSimpleExpenses()).isEqualByComparingTo("650.00");
        assertThat(summary.totalInstallmentPurchases()).isEqualByComparingTo("900.00");
        assertThat(summary.totalExpensesConceptual()).isEqualByComparingTo("1550.00");
        assertThat(summary.expensesCount()).isEqualTo(4L);
        assertThat(installmentOnly.totalInstallmentPurchases()).isEqualByComparingTo("900.00");
        assertThat(installmentOnly.expensesCount()).isEqualTo(1L);
        assertThat(paidOnly.totalExpensesConceptual()).isEqualByComparingTo("350.00");
    }

    @Test
    void expenseBreakdownsSupportFilters() {
        Fixture fixture = createFixture("payment-methods");
        Long otherPaymentMethodId = insertPaymentMethod(fixture.accountId(), "Debit " + System.nanoTime());
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 200, "2026-05-11", "PAID", "ACTIVE", "SIMPLE");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), otherPaymentMethodId, fixture.participantId(), 300, "2026-05-12", "PENDING", "ACTIVE", "SIMPLE");
        insertInstallmentDebt(fixture, "ACTIVE", 900, 900);

        var paymentMethodItems = analyticsQueryPort.getExpensesByPaymentMethod(new ExpenseBreakdownQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(),
                ExpenseStatus.ACTIVE, ExpensePaymentState.PAID, ExpenseType.SIMPLE));
        var categoryItems = analyticsQueryPort.getExpensesByCategory(new ExpenseBreakdownQuery(
                fixture.accountId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(),
                ExpenseStatus.ACTIVE, ExpensePaymentState.PAID, ExpenseType.SIMPLE));

        assertThat(paymentMethodItems).singleElement().satisfies(item -> {
            assertThat(item.paymentMethodId()).isEqualTo(fixture.paymentMethodId());
            assertThat(item.amount()).isEqualByComparingTo("200.00");
            assertThat(item.count()).isEqualTo(1L);
        });
        assertThat(categoryItems).singleElement().satisfies(item -> {
            assertThat(item.categoryId()).isEqualTo(fixture.expenseCategoryId());
            assertThat(item.amount()).isEqualByComparingTo("200.00");
        });
    }

    @Test
    void cashflowCanBeGroupedByDayAndMonth() {
        Fixture fixture = createFixture("cashflow-series");
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), 1000, "2026-05-10");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), 200, "2026-05-10", "PAID", "ACTIVE", "SIMPLE");
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), "ACTIVE", "MANUAL", 500, 400);
        insertDebtPayment(fixture.accountId(), debtId, fixture.participantId(), 100, "2026-05-11");

        var byDay = analyticsQueryPort.getCashflow(fixture.accountId(), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31), CashflowGroupBy.DAY, null);
        var byMonth = analyticsQueryPort.getCashflow(fixture.accountId(), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31), CashflowGroupBy.MONTH, null);

        assertThat(byDay).extracting("period").containsExactly("2026-05-10", "2026-05-11");
        assertThat(byDay.get(0).netCashflow()).isEqualByComparingTo("800.00");
        assertThat(byDay.get(1).netCashflow()).isEqualByComparingTo("-100.00");
        assertThat(byMonth).singleElement().satisfies(item -> {
            assertThat(item.period()).isEqualTo("2026-05");
            assertThat(item.netCashflow()).isEqualByComparingTo("700.00");
        });
    }

    private void seedMonthlyData(Fixture fixture, int income, int expense, int debtRemaining, int paidAmount) {
        insertIncome(fixture.accountId(), fixture.incomeCategoryId(), fixture.participantId(), income, "2026-05-10");
        insertExpense(fixture.accountId(), fixture.expenseCategoryId(), fixture.paymentMethodId(), fixture.participantId(), expense, "2026-05-11");
        Long debtId = insertDebt(fixture.accountId(), fixture.participantId(), "ACTIVE", "MANUAL", debtRemaining + paidAmount, debtRemaining);
        insertDebtPayment(fixture.accountId(), debtId, fixture.participantId(), paidAmount, "2026-05-12");
        Long budgetId = insertBudget(fixture.accountId(), 2026, 5);
        Long subBudgetId = insertDerivedSubBudget(fixture.accountId(), budgetId, debtId, 100);
        insertBudgetImpact(fixture.accountId(), budgetId, subBudgetId, debtId, 2026, 5, 100, paidAmount);
    }

    private Fixture createFixture(String suffix) {
        Long userId = jdbcTemplate.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                "analytics-" + suffix + "-" + System.nanoTime() + "@example.com",
                "hash",
                "Analytics User",
                "ACTIVE"
        );
        Long participantId = jdbcTemplate.queryForObject(
                "INSERT INTO participants (user_id, display_name, status) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                userId,
                "Analytics User",
                "ACTIVE"
        );
        Long accountId = jdbcTemplate.queryForObject(
                "INSERT INTO accounts (name, status) VALUES (?, ?) RETURNING id",
                Long.class,
                "Analytics " + suffix + " " + System.nanoTime(),
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO account_participants (account_id, participant_id, role, status) VALUES (?, ?, ?, ?)",
                accountId,
                participantId,
                "ACCOUNT_MEMBER",
                "ACTIVE"
        );
        Long incomeCategoryId = insertCategory(accountId, "Income " + suffix, "INCOME");
        Long expenseCategoryId = insertCategory(accountId, "Expense " + suffix, "EXPENSE");
        Long paymentMethodId = jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                "Cash " + suffix,
                "cash-" + suffix + "-" + System.nanoTime(),
                "CASH",
                "ACTIVE"
        );
        return new Fixture(accountId, participantId, incomeCategoryId, expenseCategoryId, paymentMethodId);
    }

    private Long insertCategory(Long accountId, String name, String type) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO categories (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                name,
                name.toLowerCase().replace(" ", "-") + "-" + System.nanoTime(),
                type,
                "ACTIVE"
        );
    }

    private void insertIncome(Long accountId, Long categoryId, Long participantId, int amount, String date) {
        insertIncome(accountId, categoryId, participantId, amount, date, "ACTIVE");
    }

    private void insertIncome(Long accountId, Long categoryId, Long participantId, int amount, String date, String status) {
        jdbcTemplate.update(
                "INSERT INTO incomes (account_id, category_id, participant_id, description, amount, currency, income_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                accountId,
                categoryId,
                participantId,
                "Income",
                amount,
                "COP",
                date,
                status
        );
    }

    private Long insertExpense(Long accountId, Long categoryId, Long paymentMethodId, Long participantId, int amount, String date) {
        return insertExpense(accountId, categoryId, paymentMethodId, participantId, amount, date, "PAID", "ACTIVE", "SIMPLE");
    }

    private Long insertExpense(Long accountId, Long categoryId, Long paymentMethodId, Long participantId, int amount, String date,
                               String paymentState, String status, String expenseType) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                "Expense",
                amount,
                "COP",
                date,
                paymentState,
                status,
                expenseType
        );
    }

    private Long insertExpense(Long accountId, Long categoryId, Long paymentMethodId, Long participantId, int amount, String date,
                               String paymentState, String status, String expenseType, String sourceType, Long sourceDebtPaymentId) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type, source_type, source_debt_payment_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                categoryId,
                paymentMethodId,
                participantId,
                "Expense",
                amount,
                "COP",
                date,
                paymentState,
                status,
                expenseType,
                sourceType,
                sourceDebtPaymentId
        );
    }

    private Long insertPaymentMethod(Long accountId, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO payment_methods (account_id, name, normalized_name, type, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                name,
                name.toLowerCase().replace(" ", "-") + "-" + System.nanoTime(),
                "DEBIT_CARD",
                "ACTIVE"
        );
    }

    private Long insertDebt(Long accountId, Long participantId, String state, String sourceType, int totalAmount, int remainingAmount) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO debts (account_id, participant_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, start_date, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                participantId,
                sourceType,
                "Debt",
                totalAmount,
                "COP",
                remainingAmount,
                "COP",
                "2026-05-01",
                state
        );
    }

    private Long insertInstallmentDebt(Fixture fixture, String state, int totalAmount, int remainingAmount) {
        Long originExpenseId = jdbcTemplate.queryForObject(
                "INSERT INTO expenses (account_id, category_id, payment_method_id, participant_id, description, amount, currency, expense_date, payment_state, status, expense_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                fixture.accountId(),
                fixture.expenseCategoryId(),
                fixture.paymentMethodId(),
                fixture.participantId(),
                "Installment origin",
                totalAmount,
                "COP",
                "2026-05-01",
                "PENDING",
                "ACTIVE",
                "INSTALLMENT"
        );
        return jdbcTemplate.queryForObject(
                "INSERT INTO debts (account_id, participant_id, origin_expense_id, source_type, name, total_amount, total_currency, remaining_amount, remaining_currency, installment_count, installment_amount, installment_currency, start_date, state) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                fixture.accountId(),
                fixture.participantId(),
                originExpenseId,
                "INSTALLMENT_EXPENSE",
                "Installment debt",
                totalAmount,
                "COP",
                remainingAmount,
                "COP",
                3,
                totalAmount / 3,
                "COP",
                "2026-05-01",
                state
        );
    }

    private void insertDebtPayment(Long accountId, Long debtId, Long participantId, int amount, String date) {
        jdbcTemplate.update(
                "INSERT INTO debt_payments (account_id, debt_id, participant_id, payment_type, amount, currency, payment_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                accountId,
                debtId,
                participantId,
                "INSTALLMENT",
                amount,
                "COP",
                date,
                "ACTIVE"
        );
    }

    private Long insertDebtPaymentReturningId(Long accountId, Long debtId, Long participantId, int amount, String date) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO debt_payments (account_id, debt_id, participant_id, payment_type, amount, currency, payment_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                debtId,
                participantId,
                "INSTALLMENT",
                amount,
                "COP",
                date,
                "ACTIVE"
        );
    }

    private Long insertBudget(Long accountId, int year, int month) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO budgets (account_id, year, month, status) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                year,
                month,
                "ACTIVE"
        );
    }

    private Long insertDerivedSubBudget(Long accountId, Long budgetId, Long debtId, int plannedAmount) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO sub_budgets (account_id, budget_id, debt_id, name, planned_amount, planned_currency, spent_amount, spent_currency, status, source_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                budgetId,
                debtId,
                "Debt derived",
                plannedAmount,
                "COP",
                0,
                "COP",
                "ACTIVE",
                "DEBT_DERIVED"
        );
    }

    private Long insertManualSubBudget(Long accountId, Long budgetId, Long categoryId, String name, int plannedAmount, String status, String sourceType) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO sub_budgets (account_id, budget_id, category_id, name, planned_amount, planned_currency, spent_amount, spent_currency, status, source_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                accountId,
                budgetId,
                categoryId,
                name,
                plannedAmount,
                "COP",
                0,
                "COP",
                status,
                sourceType
        );
    }

    private void insertBudgetImpact(Long accountId, Long budgetId, Long subBudgetId, Long debtId, int year, int month, int expected, int paid) {
        jdbcTemplate.update(
                "INSERT INTO budget_impacts (account_id, budget_id, sub_budget_id, debt_id, period_year, period_month, expected_amount, expected_currency, paid_amount, paid_currency, status, source_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                accountId,
                budgetId,
                subBudgetId,
                debtId,
                year,
                month,
                expected,
                "COP",
                paid,
                "COP",
                paid == expected ? "PAID" : "ACTIVE",
                "DEBT_INSTALLMENT"
        );
    }

    private record Fixture(Long accountId, Long participantId, Long incomeCategoryId, Long expenseCategoryId, Long paymentMethodId) {
    }
}
