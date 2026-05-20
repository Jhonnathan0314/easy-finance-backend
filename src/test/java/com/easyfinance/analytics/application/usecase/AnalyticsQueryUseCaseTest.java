package com.easyfinance.analytics.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.AccountRepositoryPort;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.analytics.application.port.out.AnalyticsQueryPort;
import com.easyfinance.analytics.application.query.AnalyticsDateRangeQuery;
import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.query.CashflowQuery;
import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.analytics.application.query.MonthlyAnalyticsQuery;
import com.easyfinance.analytics.application.response.BudgetSummaryResponse;
import com.easyfinance.analytics.application.response.BudgetVsExpensesCategoryItem;
import com.easyfinance.analytics.application.response.CategoryAmountItem;
import com.easyfinance.analytics.application.response.DebtSummaryResponse;
import com.easyfinance.analytics.application.response.MonthlySummaryResponse;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsQueryUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountRepositoryPort accountRepository = mock(AccountRepositoryPort.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final AnalyticsQueryPort analyticsQueryPort = mock(AnalyticsQueryPort.class);
    private final AccountAuthorizationService accountAuthorizationService = new AccountAuthorizationService(accountRepository, accountParticipantRepository);
    private final AnalyticsQueryUseCase useCase = new AnalyticsQueryUseCase(currentUserProvider, accountAuthorizationService, analyticsQueryPort);

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    @Test
    void monthlySummaryRequiresActiveMembershipAndDelegates() {
        givenMemberAccess();
        when(analyticsQueryPort.getMonthlySummary(any(), any(), any(), any(), any())).thenReturn(monthlySummary());

        var response = useCase.getMonthlySummary(new MonthlyAnalyticsQuery(1L, 2026, 5));

        assertThat(response.netBalance()).isEqualByComparingTo("800.00");
    }

    @Test
    void expensesByCategoryReturnsItems() {
        givenMemberAccess();
        when(analyticsQueryPort.getExpensesByCategory(any(ExpenseBreakdownQuery.class)))
                .thenReturn(List.of(new CategoryAmountItem(2L, "Food", new BigDecimal("120.00"), 2L)));

        var response = useCase.getExpensesByCategory(new ExpenseBreakdownQuery(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null, null, null, null));

        assertThat(response.items()).hasSize(1);
    }

    @Test
    void incomesByCategoryReturnsItems() {
        givenMemberAccess();
        when(analyticsQueryPort.getIncomesByCategory(any(IncomeBreakdownQuery.class)))
                .thenReturn(List.of(new CategoryAmountItem(3L, "Salary", new BigDecimal("1000.00"), 1L)));

        var response = useCase.getIncomesByCategory(new IncomeBreakdownQuery(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31),
                null, null, null));

        assertThat(response.items().getFirst().amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void debtSummaryDelegates() {
        givenMemberAccess();
        when(analyticsQueryPort.getDebtSummary(1L)).thenReturn(new DebtSummaryResponse(1L, 1L, 1L, 1L, new BigDecimal("500.00"), new BigDecimal("200.00"), new BigDecimal("300.00"), 1L, 1L));

        var response = useCase.getDebtSummary(1L);

        assertThat(response.cancelledDebtsCount()).isEqualTo(1L);
    }

    @Test
    void budgetSummaryWithoutBudgetCanReturnZeros() {
        givenMemberAccess();
        when(analyticsQueryPort.getBudgetSummary(1L, 2026, 5)).thenReturn(new BudgetSummaryResponse(1L, 2026, 5, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L, 0L));

        var response = useCase.getBudgetSummary(new MonthlyAnalyticsQuery(1L, 2026, 5));

        assertThat(response.budgetId()).isNull();
        assertThat(response.expectedAmount()).isZero();
    }

    @Test
    void budgetVsExpensesByCategoryCombinesMonthlyBudgetAndConceptualExpenses() {
        givenMemberAccess();
        when(analyticsQueryPort.getBudgetVsExpensesByCategory(
                1L,
                2026,
                5,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        )).thenReturn(List.of(
                new BudgetVsExpensesCategoryItem(2L, "Food", new BigDecimal("500.00"), new BigDecimal("200.00"), new BigDecimal("300.00"), new BigDecimal("40.00")),
                new BudgetVsExpensesCategoryItem(3L, "Transport", new BigDecimal("300.00"), BigDecimal.ZERO.setScale(2), new BigDecimal("300.00"), BigDecimal.ZERO.setScale(2)),
                new BudgetVsExpensesCategoryItem(4L, "Health", BigDecimal.ZERO.setScale(2), new BigDecimal("150.00"), new BigDecimal("-150.00"), null)
        ));

        var response = useCase.getBudgetVsExpensesByCategory(new MonthlyAnalyticsQuery(1L, 2026, 5));

        assertThat(response.from()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.to()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(response.items()).hasSize(3);
        assertThat(response.items().get(0).remainingAmount()).isEqualByComparingTo("300.00");
        assertThat(response.items().get(0).executionPercentage()).isEqualByComparingTo("40.00");
        assertThat(response.items().get(2).budgetedAmount()).isZero();
        assertThat(response.items().get(2).executionPercentage()).isNull();
    }

    @Test
    void invalidDateRangeFails() {
        assertThatThrownBy(() -> new AnalyticsDateRangeQuery(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ANALYTICS_DATE_RANGE_INVALID"));
    }

    @Test
    void rangeGreaterThanTwentyFourMonthsFails() {
        assertThatThrownBy(() -> new ExpenseSummaryQuery(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 2),
                null, null, null, null, null, null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ANALYTICS_DATE_RANGE_TOO_LARGE"));
    }

    @Test
    void archivedAccountAllowsReadAnalytics() {
        givenArchivedMemberAccess();
        when(analyticsQueryPort.getCashflow(any(), any(), any(), any(), any())).thenReturn(List.of());

        var response = useCase.getCashflow(new CashflowQuery(1L, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31), CashflowGroupBy.MONTH, null));

        assertThat(response.items()).isEmpty();
    }

    @Test
    void invalidPeriodFails() {
        assertThatThrownBy(() -> new MonthlyAnalyticsQuery(1L, 2026, 13))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ANALYTICS_PERIOD_INVALID"));
    }

    @Test
    void invalidYearFails() {
        assertThatThrownBy(() -> new MonthlyAnalyticsQuery(1L, 1999, 12))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ANALYTICS_PERIOD_INVALID"));
        assertThatThrownBy(() -> new MonthlyAnalyticsQuery(1L, 2101, 1))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ANALYTICS_PERIOD_INVALID"));
    }

    @Test
    void noMemberFailsAsAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getDebtSummary(1L))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void budgetVsExpensesByCategoryRequiresMembership() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getBudgetVsExpensesByCategory(new MonthlyAnalyticsQuery(1L, 2026, 5)))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_FOUND"));
    }

    private void givenMemberAccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private void givenArchivedMemberAccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(Account.restore(1L, "Home", null, AccountStatus.ARCHIVED, Instant.now(), Instant.now())));
        when(accountParticipantRepository.findByAccountIdAndParticipantId(1L, 10L))
                .thenReturn(Optional.of(AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), null, null)));
    }

    private static MonthlySummaryResponse monthlySummary() {
        return new MonthlySummaryResponse(
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
        );
    }
}
