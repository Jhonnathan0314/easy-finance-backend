package com.easyfinance.analytics.entrypoint.rest;

import com.easyfinance.analytics.application.port.in.GetBudgetSummaryPort;
import com.easyfinance.analytics.application.port.in.GetBudgetVsExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetCashflowPort;
import com.easyfinance.analytics.application.port.in.GetCashflowSummaryPort;
import com.easyfinance.analytics.application.port.in.GetDebtSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpenseSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodTypePort;
import com.easyfinance.analytics.application.port.in.GetIncomesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetMonthlySummaryPort;
import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.response.BudgetSummaryResponse;
import com.easyfinance.analytics.application.response.BudgetVsExpensesByCategoryResponse;
import com.easyfinance.analytics.application.response.BudgetVsExpensesCategoryItem;
import com.easyfinance.analytics.application.response.CashflowItem;
import com.easyfinance.analytics.application.response.CashflowResponse;
import com.easyfinance.analytics.application.response.CashflowSummaryResponse;
import com.easyfinance.analytics.application.response.CategoryAmountItem;
import com.easyfinance.analytics.application.response.CategoryBreakdownResponse;
import com.easyfinance.analytics.application.response.DebtSummaryResponse;
import com.easyfinance.analytics.application.response.ExpenseSummaryResponse;
import com.easyfinance.analytics.application.response.MonthlySummaryResponse;
import com.easyfinance.analytics.application.response.PaymentMethodAmountItem;
import com.easyfinance.analytics.application.response.PaymentMethodBreakdownResponse;
import com.easyfinance.analytics.application.response.PaymentMethodTypeAmountItem;
import com.easyfinance.analytics.application.response.PaymentMethodTypeBreakdownResponse;
import com.easyfinance.shared.infrastructure.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsControllerTest {

    private final GetMonthlySummaryPort getMonthlySummaryPort = mock(GetMonthlySummaryPort.class);
    private final GetCashflowSummaryPort getCashflowSummaryPort = mock(GetCashflowSummaryPort.class);
    private final GetExpenseSummaryPort getExpenseSummaryPort = mock(GetExpenseSummaryPort.class);
    private final GetCashflowPort getCashflowPort = mock(GetCashflowPort.class);
    private final GetExpensesByCategoryPort getExpensesByCategoryPort = mock(GetExpensesByCategoryPort.class);
    private final GetExpensesByPaymentMethodPort getExpensesByPaymentMethodPort = mock(GetExpensesByPaymentMethodPort.class);
    private final GetExpensesByPaymentMethodTypePort getExpensesByPaymentMethodTypePort = mock(GetExpensesByPaymentMethodTypePort.class);
    private final GetIncomesByCategoryPort getIncomesByCategoryPort = mock(GetIncomesByCategoryPort.class);
    private final GetDebtSummaryPort getDebtSummaryPort = mock(GetDebtSummaryPort.class);
    private final GetBudgetSummaryPort getBudgetSummaryPort = mock(GetBudgetSummaryPort.class);
    private final GetBudgetVsExpensesByCategoryPort getBudgetVsExpensesByCategoryPort = mock(GetBudgetVsExpensesByCategoryPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AnalyticsController(getMonthlySummaryPort, getCashflowSummaryPort,
                        getExpenseSummaryPort, getCashflowPort, getExpensesByCategoryPort,
                        getExpensesByPaymentMethodPort, getExpensesByPaymentMethodTypePort,
                        getIncomesByCategoryPort, getDebtSummaryPort,
                        getBudgetSummaryPort, getBudgetVsExpensesByCategoryPort))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void monthlySummaryEndpointDelegates() throws Exception {
        when(getMonthlySummaryPort.getMonthlySummary(any())).thenReturn(new MonthlySummaryResponse(
                1L,
                2026,
                5,
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("800.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/monthly-summary?year=2026&month=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netBalance").value(800.00));
    }

    @Test
    void categoryBreakdownEndpointsDelegate() throws Exception {
        when(getExpensesByCategoryPort.getExpensesByCategory(any())).thenReturn(new CategoryBreakdownResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(new CategoryAmountItem(2L, "Food", new BigDecimal("120.00"), 2L))
        ));
        when(getIncomesByCategoryPort.getIncomesByCategory(any())).thenReturn(new CategoryBreakdownResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(new CategoryAmountItem(3L, "Salary", new BigDecimal("1000.00"), 1L))
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/expenses-by-category?from=2026-05-01&to=2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].categoryName").value("Food"));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/incomes-by-category?from=2026-05-01&to=2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].categoryName").value("Salary"));
    }

    @Test
    void debtAndBudgetSummaryEndpointsDelegate() throws Exception {
        when(getDebtSummaryPort.getDebtSummary(1L)).thenReturn(new DebtSummaryResponse(1L, 1L, 1L, 1L, new BigDecimal("500.00"), new BigDecimal("200.00"), new BigDecimal("300.00"), 1L, 1L));
        when(getBudgetSummaryPort.getBudgetSummary(any())).thenReturn(new BudgetSummaryResponse(1L, 2026, 5, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L, 0L));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/debt-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledDebtsCount").value(1));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/budget-summary?year=2026&month=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").doesNotExist());
    }

    @Test
    void budgetVsExpensesByCategoryEndpointDelegates() throws Exception {
        when(getBudgetVsExpensesByCategoryPort.getBudgetVsExpensesByCategory(any())).thenReturn(new BudgetVsExpensesByCategoryResponse(
                1L,
                2026,
                5,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(new BudgetVsExpensesCategoryItem(
                        2L,
                        "Food",
                        new BigDecimal("500.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("40.00")
                ))
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/budget-vs-expenses-by-category?year=2026&month=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.from").value("2026-05-01"))
                .andExpect(jsonPath("$.to").value("2026-05-31"))
                .andExpect(jsonPath("$.items[0].categoryId").value(2))
                .andExpect(jsonPath("$.items[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.items[0].budgetedAmount").value(500.00))
                .andExpect(jsonPath("$.items[0].spentAmount").value(200.00))
                .andExpect(jsonPath("$.items[0].remainingAmount").value(300.00))
                .andExpect(jsonPath("$.items[0].executionPercentage").value(40.00));
    }

    @Test
    void invalidPeriodReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/analytics/monthly-summary?year=2026&month=13"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ANALYTICS_PERIOD_INVALID"));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/budget-vs-expenses-by-category?year=2026&month=13"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ANALYTICS_PERIOD_INVALID"));
    }

    @Test
    void invalidDateRangeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/analytics/expenses-by-category?from=2026-06-01&to=2026-05-01"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ANALYTICS_DATE_RANGE_INVALID"));
    }

    @Test
    void rangeGreaterThanTwentyFourMonthsReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/analytics/expenses-by-category?from=2026-01-01&to=2028-01-02"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ANALYTICS_DATE_RANGE_TOO_LARGE"));
    }

    @Test
    void cashflowSummaryEndpointDelegates() throws Exception {
        when(getCashflowSummaryPort.getCashflowSummary(any())).thenReturn(new CashflowSummaryResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("150.00"),
                new BigDecimal("350.00"),
                new BigDecimal("650.00"),
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/cashflow-summary?from=2026-05-01&to=2026-05-31&participantId=7&categoryId=2&paymentMethodId=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalSimpleExpenseOutflow").value(200.00))
                .andExpect(jsonPath("$.totalDebtPaymentOutflow").value(150.00))
                .andExpect(jsonPath("$.netCashflow").value(650.00));
    }

    @Test
    void expenseSummaryEndpointDelegates() throws Exception {
        when(getExpenseSummaryPort.getExpenseSummary(any())).thenReturn(new ExpenseSummaryResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                new BigDecimal("300.00"),
                new BigDecimal("700.00"),
                new BigDecimal("1000.00"),
                4L,
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/expense-summary?from=2026-05-01&to=2026-05-31&categoryId=2&paymentMethodId=3&participantId=7&expenseType=INSTALLMENT&paymentState=PARTIAL&status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSimpleExpenses").value(300.00))
                .andExpect(jsonPath("$.totalInstallmentPurchases").value(700.00))
                .andExpect(jsonPath("$.totalExpensesConceptual").value(1000.00))
                .andExpect(jsonPath("$.expensesCount").value(4));
    }

    @Test
    void cashflowEndpointDelegates() throws Exception {
        when(getCashflowPort.getCashflow(any())).thenReturn(new CashflowResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                CashflowGroupBy.MONTH,
                List.of(new CashflowItem("2026-05", new BigDecimal("1000.00"),
                        new BigDecimal("200.00"), new BigDecimal("150.00"),
                        new BigDecimal("350.00"), new BigDecimal("650.00")))
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/cashflow?from=2026-05-01&to=2026-05-31&groupBy=MONTH&participantId=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("MONTH"))
                .andExpect(jsonPath("$.items[0].period").value("2026-05"))
                .andExpect(jsonPath("$.items[0].netCashflow").value(650.00));
    }

    @Test
    void expensesByPaymentMethodEndpointDelegates() throws Exception {
        when(getExpensesByPaymentMethodPort.getExpensesByPaymentMethod(any())).thenReturn(new PaymentMethodBreakdownResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(new PaymentMethodAmountItem(3L, "Credit Card", new BigDecimal("420.00"), 2L))
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/expenses-by-payment-method?from=2026-05-01&to=2026-05-31&categoryId=2&paymentMethodId=3&participantId=7&expenseType=SIMPLE&paymentState=PAID&status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].paymentMethodId").value(3))
                .andExpect(jsonPath("$.items[0].paymentMethodName").value("Credit Card"))
                .andExpect(jsonPath("$.items[0].amount").value(420.00))
                .andExpect(jsonPath("$.items[0].count").value(2));
    }

    @Test
    void expensesByPaymentMethodTypeEndpointDelegates() throws Exception {
        when(getExpensesByPaymentMethodTypePort.getExpensesByPaymentMethodType(any())).thenReturn(new PaymentMethodTypeBreakdownResponse(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(new PaymentMethodTypeAmountItem("CREDIT_CARD", new BigDecimal("420.00"), 2L))
        ));

        mockMvc.perform(get("/api/v1/accounts/1/analytics/expenses-by-payment-method-type?from=2026-05-01&to=2026-05-31&categoryId=2&participantId=7&expenseType=SIMPLE&paymentState=PAID&status=ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].paymentMethodType").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.items[0].amount").value(420.00))
                .andExpect(jsonPath("$.items[0].count").value(2));
    }
}
