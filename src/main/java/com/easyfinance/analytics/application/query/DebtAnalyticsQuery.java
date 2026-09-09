package com.easyfinance.analytics.application.query;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import java.time.LocalDate;

public record DebtAnalyticsQuery(Long accountId, LocalDate from, LocalDate to, CashflowGroupBy groupBy,
                                 DebtAnalyticsState state, Long participantId, Long categoryId, Long paymentMethodId) {
    public DebtAnalyticsQuery {
        AnalyticsRangeValidator.validate(from, to);
        if (groupBy == null) throw new BusinessRuleViolationException("ANALYTICS_GROUP_BY_INVALID", "Analytics groupBy is invalid.");
        if (state == null) throw new BusinessRuleViolationException("ANALYTICS_DEBT_STATE_INVALID", "Debt state is invalid.");
    }
}
