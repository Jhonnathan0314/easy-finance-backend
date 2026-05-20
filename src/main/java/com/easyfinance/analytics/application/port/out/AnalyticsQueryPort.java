package com.easyfinance.analytics.application.port.out;

import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.query.CashflowSummaryQuery;
import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.analytics.application.response.BudgetSummaryResponse;
import com.easyfinance.analytics.application.response.BudgetVsExpensesCategoryItem;
import com.easyfinance.analytics.application.response.CashflowItem;
import com.easyfinance.analytics.application.response.CashflowSummaryResponse;
import com.easyfinance.analytics.application.response.CategoryAmountItem;
import com.easyfinance.analytics.application.response.DebtSummaryResponse;
import com.easyfinance.analytics.application.response.ExpenseSummaryResponse;
import com.easyfinance.analytics.application.response.MonthlySummaryResponse;
import com.easyfinance.analytics.application.response.PaymentMethodAmountItem;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsQueryPort {
    MonthlySummaryResponse getMonthlySummary(Long accountId, Integer year, Integer month, LocalDate startInclusive, LocalDate endInclusive);

    CashflowSummaryResponse getCashflowSummary(CashflowSummaryQuery query);

    ExpenseSummaryResponse getExpenseSummary(ExpenseSummaryQuery query);

    List<CashflowItem> getCashflow(Long accountId, LocalDate from, LocalDate to, CashflowGroupBy groupBy, Long participantId);

    List<CategoryAmountItem> getExpensesByCategory(ExpenseBreakdownQuery query);

    List<CategoryAmountItem> getIncomesByCategory(IncomeBreakdownQuery query);

    List<PaymentMethodAmountItem> getExpensesByPaymentMethod(ExpenseBreakdownQuery query);

    DebtSummaryResponse getDebtSummary(Long accountId);

    BudgetSummaryResponse getBudgetSummary(Long accountId, Integer year, Integer month);

    List<BudgetVsExpensesCategoryItem> getBudgetVsExpensesByCategory(Long accountId, Integer year, Integer month, LocalDate from, LocalDate to);
}
