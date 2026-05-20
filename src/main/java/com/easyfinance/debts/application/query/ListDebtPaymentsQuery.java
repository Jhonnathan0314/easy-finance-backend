package com.easyfinance.debts.application.query;

import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.BusinessRuleViolationException;

import java.time.LocalDate;

public record ListDebtPaymentsQuery(
        Long accountId,
        Long debtId,
        LocalDate from,
        LocalDate to,
        DebtPaymentType paymentType,
        DebtPaymentStatus status,
        PageQuery pageQuery,
        String sort
) {
    public ListDebtPaymentsQuery {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleViolationException("DEBT_PAYMENT_DATE_INVALID", "Debt payment date range is invalid.");
        }
    }
}
