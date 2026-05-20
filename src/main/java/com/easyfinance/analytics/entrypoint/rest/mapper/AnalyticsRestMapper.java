package com.easyfinance.analytics.entrypoint.rest.mapper;

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
import com.easyfinance.analytics.entrypoint.rest.dto.BudgetSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.BudgetVsExpensesByCategoryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.BudgetVsExpensesCategoryItemDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CashflowItemDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CashflowResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CashflowSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CategoryAmountItemDto;
import com.easyfinance.analytics.entrypoint.rest.dto.CategoryBreakdownResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.DebtSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.ExpenseSummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.MonthlySummaryResponseDto;
import com.easyfinance.analytics.entrypoint.rest.dto.PaymentMethodAmountItemDto;
import com.easyfinance.analytics.entrypoint.rest.dto.PaymentMethodBreakdownResponseDto;

public final class AnalyticsRestMapper {

    private AnalyticsRestMapper() {
    }

    public static MonthlySummaryResponseDto toDto(MonthlySummaryResponse response) {
        return new MonthlySummaryResponseDto(
                response.accountId(),
                response.year(),
                response.month(),
                response.totalIncome(),
                response.totalExpenses(),
                response.netBalance(),
                response.totalDebtRemaining(),
                response.totalDebtPaidInMonth(),
                response.activeDebtsCount(),
                response.paidDebtsCount(),
                response.budgetExpected(),
                response.budgetPaid(),
                response.budgetPending(),
                response.generatedAt()
        );
    }

    public static CategoryBreakdownResponseDto toDto(CategoryBreakdownResponse response) {
        return new CategoryBreakdownResponseDto(
                response.accountId(),
                response.from(),
                response.to(),
                response.items().stream().map(AnalyticsRestMapper::toDto).toList()
        );
    }

    public static DebtSummaryResponseDto toDto(DebtSummaryResponse response) {
        return new DebtSummaryResponseDto(
                response.accountId(),
                response.activeDebtsCount(),
                response.paidDebtsCount(),
                response.cancelledDebtsCount(),
                response.totalDebtAmount(),
                response.totalRemainingBalance(),
                response.totalPaidAmount(),
                response.manualDebtsCount(),
                response.installmentExpenseDebtsCount()
        );
    }

    public static BudgetSummaryResponseDto toDto(BudgetSummaryResponse response) {
        return new BudgetSummaryResponseDto(
                response.accountId(),
                response.year(),
                response.month(),
                response.budgetId(),
                response.expectedAmount(),
                response.paidAmount(),
                response.pendingAmount(),
                response.impactsCount(),
                response.paidImpactsCount(),
                response.activeImpactsCount(),
                response.subBudgetsCount()
        );
    }

    public static BudgetVsExpensesByCategoryResponseDto toDto(BudgetVsExpensesByCategoryResponse response) {
        return new BudgetVsExpensesByCategoryResponseDto(
                response.accountId(),
                response.year(),
                response.month(),
                response.from(),
                response.to(),
                response.items().stream().map(AnalyticsRestMapper::toDto).toList()
        );
    }

    public static CashflowSummaryResponseDto toDto(CashflowSummaryResponse response) {
        return new CashflowSummaryResponseDto(
                response.accountId(),
                response.from(),
                response.to(),
                response.totalIncome(),
                response.totalSimpleExpenseOutflow(),
                response.totalDebtPaymentOutflow(),
                response.totalOutflow(),
                response.netCashflow(),
                response.generatedAt()
        );
    }

    public static ExpenseSummaryResponseDto toDto(ExpenseSummaryResponse response) {
        return new ExpenseSummaryResponseDto(
                response.accountId(),
                response.from(),
                response.to(),
                response.totalSimpleExpenses(),
                response.totalInstallmentPurchases(),
                response.totalExpensesConceptual(),
                response.expensesCount(),
                response.generatedAt()
        );
    }

    public static CashflowResponseDto toDto(CashflowResponse response) {
        return new CashflowResponseDto(
                response.accountId(),
                response.from(),
                response.to(),
                response.groupBy().name(),
                response.items().stream().map(AnalyticsRestMapper::toDto).toList()
        );
    }

    public static PaymentMethodBreakdownResponseDto toDto(PaymentMethodBreakdownResponse response) {
        return new PaymentMethodBreakdownResponseDto(
                response.accountId(),
                response.from(),
                response.to(),
                response.items().stream().map(AnalyticsRestMapper::toDto).toList()
        );
    }

    private static CategoryAmountItemDto toDto(CategoryAmountItem item) {
        return new CategoryAmountItemDto(item.categoryId(), item.categoryName(), item.amount(), item.count());
    }

    private static CashflowItemDto toDto(CashflowItem item) {
        return new CashflowItemDto(
                item.period(),
                item.totalIncome(),
                item.simpleExpenseOutflow(),
                item.debtPaymentOutflow(),
                item.totalOutflow(),
                item.netCashflow()
        );
    }

    private static PaymentMethodAmountItemDto toDto(PaymentMethodAmountItem item) {
        return new PaymentMethodAmountItemDto(item.paymentMethodId(), item.paymentMethodName(), item.amount(), item.count());
    }

    private static BudgetVsExpensesCategoryItemDto toDto(BudgetVsExpensesCategoryItem item) {
        return new BudgetVsExpensesCategoryItemDto(
                item.categoryId(),
                item.categoryName(),
                item.budgetedAmount(),
                item.spentAmount(),
                item.remainingAmount(),
                item.executionPercentage()
        );
    }
}
