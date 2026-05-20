package com.easyfinance.debts.application.query;

import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

public record ListDebtsQuery(
        Long accountId,
        DebtState state,
        DebtSourceType sourceType,
        Long participantId,
        LocalDate from,
        LocalDate to,
        PageQuery pageQuery,
        String sort
) {
    public ListDebtsQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("DEBT_DATE_INVALID", "Debt date range is invalid.");
        }
    }
}
