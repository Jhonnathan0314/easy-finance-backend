package com.easyfinance.expenses.application.command;

import java.time.LocalDate;

public record CreditCardClosingCommand(
        Long accountId,
        Long paymentMethodId,
        LocalDate from,
        LocalDate to
) {
}
