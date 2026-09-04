package com.easyfinance.imports.application.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DebtImportRowResponse(
        Integer rowNumber,
        String name,
        String description,
        BigDecimal totalAmount,
        BigDecimal remainingBalance,
        Integer installmentCount,
        BigDecimal installmentAmount,
        LocalDate startDate,
        LocalDate dueDate,
        String participantLabel,
        Long participantId,
        String notes,
        boolean valid,
        Long createdDebtId,
        List<String> errors
) {
}
