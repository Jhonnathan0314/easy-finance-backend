package com.easyfinance.analytics.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.analytics.application.port.in.GetBudgetVsExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetCashflowPort;
import com.easyfinance.analytics.application.port.in.GetCashflowSummaryPort;
import com.easyfinance.analytics.application.port.in.GetBudgetSummaryPort;
import com.easyfinance.analytics.application.port.in.GetDebtSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpenseSummaryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodPort;
import com.easyfinance.analytics.application.port.in.GetExpensesByPaymentMethodTypePort;
import com.easyfinance.analytics.application.port.in.GetIncomesByCategoryPort;
import com.easyfinance.analytics.application.port.in.GetMonthlySummaryPort;
import com.easyfinance.analytics.application.port.out.AnalyticsQueryPort;
import com.easyfinance.analytics.application.query.CashflowQuery;
import com.easyfinance.analytics.application.query.CashflowSummaryQuery;
import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.analytics.application.query.MonthlyAnalyticsQuery;
import com.easyfinance.analytics.application.response.BudgetSummaryResponse;
import com.easyfinance.analytics.application.response.BudgetVsExpensesByCategoryResponse;
import com.easyfinance.analytics.application.response.CashflowResponse;
import com.easyfinance.analytics.application.response.CashflowSummaryResponse;
import com.easyfinance.analytics.application.response.CategoryBreakdownResponse;
import com.easyfinance.analytics.application.response.DebtSummaryResponse;
import com.easyfinance.analytics.application.response.ExpenseSummaryResponse;
import com.easyfinance.analytics.application.response.MonthlySummaryResponse;
import com.easyfinance.analytics.application.response.PaymentMethodBreakdownResponse;
import com.easyfinance.analytics.application.response.PaymentMethodTypeBreakdownResponse;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
public class AnalyticsQueryUseCase implements
        GetMonthlySummaryPort,
        GetCashflowSummaryPort,
        GetExpenseSummaryPort,
        GetCashflowPort,
        GetExpensesByCategoryPort,
        GetExpensesByPaymentMethodPort,
        GetExpensesByPaymentMethodTypePort,
        GetIncomesByCategoryPort,
        GetDebtSummaryPort,
        GetBudgetSummaryPort,
        GetBudgetVsExpensesByCategoryPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AnalyticsQueryPort analyticsQueryPort;

    public AnalyticsQueryUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            AnalyticsQueryPort analyticsQueryPort
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.analyticsQueryPort = analyticsQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(MonthlyAnalyticsQuery query) {
        requireActiveMember(query.accountId());
        YearMonth period = YearMonth.of(query.year(), query.month());
        return analyticsQueryPort.getMonthlySummary(query.accountId(), query.year(), query.month(), period.atDay(1), period.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public CashflowSummaryResponse getCashflowSummary(CashflowSummaryQuery query) {
        requireActiveMember(query.accountId());
        return analyticsQueryPort.getCashflowSummary(query);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(ExpenseSummaryQuery query) {
        requireActiveMember(query.accountId());
        return analyticsQueryPort.getExpenseSummary(query);
    }

    @Override
    @Transactional(readOnly = true)
    public CashflowResponse getCashflow(CashflowQuery query) {
        requireActiveMember(query.accountId());
        return new CashflowResponse(
                query.accountId(),
                query.from(),
                query.to(),
                query.groupBy(),
                analyticsQueryPort.getCashflow(query.accountId(), query.from(), query.to(), query.groupBy(), query.participantId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryBreakdownResponse getExpensesByCategory(ExpenseBreakdownQuery query) {
        requireActiveMember(query.accountId());
        return new CategoryBreakdownResponse(
                query.accountId(),
                query.from(),
                query.to(),
                analyticsQueryPort.getExpensesByCategory(query)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodBreakdownResponse getExpensesByPaymentMethod(ExpenseBreakdownQuery query) {
        requireActiveMember(query.accountId());
        return new PaymentMethodBreakdownResponse(
                query.accountId(),
                query.from(),
                query.to(),
                analyticsQueryPort.getExpensesByPaymentMethod(query)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodTypeBreakdownResponse getExpensesByPaymentMethodType(ExpenseBreakdownQuery query) {
        requireActiveMember(query.accountId());
        return new PaymentMethodTypeBreakdownResponse(
                query.accountId(),
                query.from(),
                query.to(),
                analyticsQueryPort.getExpensesByPaymentMethodType(query)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryBreakdownResponse getIncomesByCategory(IncomeBreakdownQuery query) {
        requireActiveMember(query.accountId());
        return new CategoryBreakdownResponse(
                query.accountId(),
                query.from(),
                query.to(),
                analyticsQueryPort.getIncomesByCategory(query)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DebtSummaryResponse getDebtSummary(Long accountId) {
        requireActiveMember(accountId);
        return analyticsQueryPort.getDebtSummary(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetSummaryResponse getBudgetSummary(MonthlyAnalyticsQuery query) {
        requireActiveMember(query.accountId());
        return analyticsQueryPort.getBudgetSummary(query.accountId(), query.year(), query.month());
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetVsExpensesByCategoryResponse getBudgetVsExpensesByCategory(MonthlyAnalyticsQuery query) {
        requireActiveMember(query.accountId());
        YearMonth period = YearMonth.of(query.year(), query.month());
        return new BudgetVsExpensesByCategoryResponse(
                query.accountId(),
                query.year(),
                query.month(),
                period.atDay(1),
                period.atEndOfMonth(),
                analyticsQueryPort.getBudgetVsExpensesByCategory(
                        query.accountId(),
                        query.year(),
                        query.month(),
                        period.atDay(1),
                        period.atEndOfMonth()
                )
        );
    }

    private void requireActiveMember(Long accountId) {
        CurrentUser currentUser = currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
        accountAuthorizationService.requireActiveMember(accountId, currentUser.participantId());
    }
}
