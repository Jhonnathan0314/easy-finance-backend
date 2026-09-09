package com.easyfinance.analytics.application.response;

import com.easyfinance.analytics.application.query.CashflowGroupBy;
import com.easyfinance.analytics.application.query.DebtAnalyticsState;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DebtAnalyticsResponse(Long accountId, LocalDate from, LocalDate to, CashflowGroupBy groupBy,
                                    DebtAnalyticsState state, DebtAnalyticsSummary summary,
                                    List<DebtAnalyticsPeriod> periods, List<DebtAnalyticsDebt> debts, Instant generatedAt) {}
