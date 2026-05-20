package com.easyfinance.analytics.application.query;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;

import java.time.LocalDate;

public record ExpenseSummaryQuery(
        Long accountId,
        LocalDate from,
        LocalDate to,
        Long categoryId,
        Long paymentMethodId,
        Long participantId,
        ExpenseType expenseType,
        ExpensePaymentState paymentState,
        ExpenseStatus status
) {
    public ExpenseSummaryQuery {
        AnalyticsRangeValidator.validate(from, to);
    }
}
