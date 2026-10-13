package com.easyfinance.expenses.application.response;

import com.easyfinance.expenses.domain.model.ExpensePaymentState;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditCardClosingResultResponse(
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long updatedCount,
        ExpensePaymentState targetState
) {
    public CreditCardClosingResultResponse(Long paymentMethodId, LocalDate from, LocalDate to, BigDecimal totalAmount, long updatedCount) {
        this(paymentMethodId, from, to, totalAmount, updatedCount, ExpensePaymentState.PAID);
    }
}
