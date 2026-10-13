package com.easyfinance.expenses.application.command;

import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingCommand(
        Long accountId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        List<Long> categoryIds
) {
    public CreditCardClosingCommand(Long accountId, Long paymentMethodId, LocalDate from, LocalDate to) {
        this(accountId, paymentMethodId, from, to, null);
    }
}
