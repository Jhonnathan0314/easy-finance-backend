package com.easyfinance.debts.entrypoint.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDebtPaymentRequest(
        @NotNull DebtPaymentTypeDto paymentType,
        @NotNull @DecimalMin(value = "0.00") BigDecimal capitalAmount,
        @DecimalMin(value = "0.00") BigDecimal interestAmount,
        @NotNull LocalDate paymentDate,
        @Size(max = 1000) String notes
) {
    public BigDecimal resolvedInterestAmount() {
        return interestAmount == null ? BigDecimal.ZERO : interestAmount;
    }
}
