package com.easyfinance.analytics.application.query;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;

import java.time.LocalDate;

public record ExpenseBreakdownQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        Long categoryId,
        Long paymentMethodId,
        Long participantId,
        ExpenseStatus status,
        ExpensePaymentState paymentState,
        ExpenseType expenseType
) {
    public ExpenseBreakdownQuery {
        AnalyticsRangeValidator.validate(from, to);
    }
}
