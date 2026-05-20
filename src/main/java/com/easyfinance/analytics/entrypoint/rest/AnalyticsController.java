package com.easyfinance.analytics.entrypoint.rest;

import com.easyfinance.analytics.application.port.in.GetCashflowPort;
import com.easyfinance.analytics.application.port.in.GetCashflowSummaryPort;
import com.easyfinance.analytics.application.port.in.GetBudgetSummaryPort;
import com.easyfinance.analytics.application.port.in.GetBudgetVsExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetDebtSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpenseSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodPort;
import com.easyfinance.analytics.application.port.in.GetIncomesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetMonthlySummaryPort;
import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.query.CashflowQuery;
import com.easyfinance.analytics.application.query.CashflowSummaryQuery;
import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.analytics.application.query.MonthlyAnalyticsQuery;
import com.easyfinance.analytics.entrypoint.rest.dto.BudgetSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.BudgetVsExpensesByCategoryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CashflowResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CashflowSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CategoryBreakdownResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.DebtSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.ExpenseSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.MonthlySummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.PaymentMethodBreakdownResponseDto;
import com.easyfinance.analytics.entrypoint.rest.mapper.AnalyticsRestMapper;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.income.domain.model.IncomeStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/analytics")
public class AnalyticsController {

    private final GetMonthlySummaryPort getMonthlySummaryPort;
    private final GetCashflowSummaryPort getCashflowSummaryPort;
    private final GetExpenseSummaryPort getExpenseSummaryPort;
    private final GetCashflowPort getCashflowPort;
    private final GetExpensesByCategoryPort getExpensesByCategoryPort;
    private final GetExpensesByPaymentMethodPort getExpensesByPaymentMethodPort;
    private final GetIncomesByCategoryPort getIncomesByCategoryPort;
    private final GetDebtSummaryPort getDebtSummaryPort;
    private final GetBudgetSummaryPort getBudgetSummaryPort;
    private final GetBudgetVsExpensesByCategoryPort getBudgetVsExpensesByCategoryPort;

    public AnalyticsController(
            GetMonthlySummaryPort getMonthlySummaryPort,
            GetCashflowSummaryPort getCashflowSummaryPort,
            GetExpenseSummaryPort getExpenseSummaryPort,
            GetCashflowPort getCashflowPort,
            GetExpensesByCategoryPort getExpensesByCategoryPort,
            GetExpensesByPaymentMethodPort getExpensesByPaymentMethodPort,
            GetIncomesByCategoryPort getIncomesByCategoryPort,
            GetDebtSummaryPort getDebtSummaryPort,
            GetBudgetSummaryPort getBudgetSummaryPort,
            GetBudgetVsExpensesByCategoryPort getBudgetVsExpensesByCategoryPort
    ) {
        this.getMonthlySummaryPort = getMonthlySummaryPort;
        this.getCashflowSummaryPort = getCashflowSummaryPort;
        this.getExpenseSummaryPort = getExpenseSummaryPort;
        this.getCashflowPort = getCashflowPort;
        this.getExpensesByCategoryPort = getExpensesByCategoryPort;
        this.getExpensesByPaymentMethodPort = getExpensesByPaymentMethodPort;
        this.getIncomesByCategoryPort = getIncomesByCategoryPort;
        this.getDebtSummaryPort = getDebtSummaryPort;
        this.getBudgetSummaryPort = getBudgetSummaryPort;
        this.getBudgetVsExpensesByCategoryPort = getBudgetVsExpensesByCategoryPort;
    }

    @GetMapping("/cashflow-summary")
    public CashflowSummaryResponseDto cashflowSummary(
            @PathVariable Long accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId
    ) {
        return AnalyticsRestMapper.toDto(getCashflowSummaryPort.getCashflowSummary(new CashflowSummaryQuery(
                accountId,
                from,
                to,
                participantId,
                categoryId,
                paymentMethodId
        )));
    }

    @GetMapping("/expense-summary")
    public ExpenseSummaryResponseDto expenseSummary(
            @PathVariable Long accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) ExpenseType expenseType,
            @RequestParam(required = false) ExpensePaymentState paymentState,
            @RequestParam(required = false) ExpenseStatus status
    ) {
        return AnalyticsRestMapper.toDto(getExpenseSummaryPort.getExpenseSummary(new ExpenseSummaryQuery(
                accountId,
                from,
                to,
                categoryId,
                paymentMethodId,
                participantId,
                expenseType,
                paymentState,
                status
        )));
    }

    @GetMapping("/cashflow")
    public CashflowResponseDto cashflow(
            @PathVariable Long accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam CashflowGroupBy groupBy,
            @RequestParam(required = false) Long participantId
    ) {
        return AnalyticsRestMapper.toDto(getCashflowPort.getCashflow(new CashflowQuery(accountId, from, to, groupBy, participantId)));
    }

    @GetMapping("/monthly-summary")
    public MonthlySummaryResponseDto monthlySummary(
            @PathVariable Long accountId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return AnalyticsRestMapper.toDto(getMonthlySummaryPort.getMonthlySummary(new MonthlyAnalyticsQuery(accountId, year, month)));
    }

    @GetMapping("/expenses-by-category")
    public CategoryBreakdownResponseDto expensesByCategory(
            @PathVariable Long accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) ExpensePaymentState paymentState,
            @RequestParam(required = false) ExpenseType expenseType
    ) {
        return AnalyticsRestMapper.toDto(getExpensesByCategoryPort.getExpensesByCategory(new ExpenseBreakdownQuery(
                accountId,
                from,
                to,
                categoryId,
                paymentMethodId,
                participantId,
                status,
                paymentState,
                expenseType
        )));
    }

    @GetMapping("/expenses-by-payment-method")
    public PaymentMethodBreakdownResponseDto expensesByPaymentMethod(
            @PathVariable Long accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long paymentMethodId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) ExpensePaymentState paymentState,
            @RequestParam(required = false) ExpenseType expenseType
    ) {
        return AnalyticsRestMapper.toDto(getExpensesByPaymentMethodPort.getExpensesByPaymentMethod(new ExpenseBreakdownQuery(
                accountId,
                from,
                to,
                categoryId,
                paymentMethodId,
                participantId,
                status,
                paymentState,
                expenseType
        )));
    }

    @GetMapping("/incomes-by-category")
    public CategoryBreakdownResponseDto incomesByCategory(
            @PathVariable Long accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) IncomeStatus status
    ) {
        return AnalyticsRestMapper.toDto(getIncomesByCategoryPort.getIncomesByCategory(new IncomeBreakdownQuery(
                accountId,
                from,
                to,
                categoryId,
                participantId,
                status
        )));
    }

    @GetMapping("/debt-summary")
    public DebtSummaryResponseDto debtSummary(@PathVariable Long accountId) {
        return AnalyticsRestMapper.toDto(getDebtSummaryPort.getDebtSummary(accountId));
    }

    @GetMapping("/budget-summary")
    public BudgetSummaryResponseDto budgetSummary(
            @PathVariable Long accountId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return AnalyticsRestMapper.toDto(getBudgetSummaryPort.getBudgetSummary(new MonthlyAnalyticsQuery(accountId, year, month)));
    }

    @GetMapping("/budget-vs-expenses-by-category")
    public BudgetVsExpensesByCategoryResponseDto budgetVsExpensesByCategory(
            @PathVariable Long accountId,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return AnalyticsRestMapper.toDto(getBudgetVsExpensesByCategoryPort.getBudgetVsExpensesByCategory(
                new MonthlyAnalyticsQuery(accountId, year, month)
        ));
    }
}
