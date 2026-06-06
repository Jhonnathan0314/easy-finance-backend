package com.easyfinance.imports.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseImportRowResponseDto(
        Long id,
        Integer rowNumber,
        LocalDate expenseDate,
        String description,
        BigDecimal amount,
        String currency,
        String categoryName,
        Long categoryId,
        String paymentMethodName,
        Long paymentMethodId,
        String paymentState,
        String participantLabel,
        Long participantId,
        boolean appliesDebtPayment,
        Long debtId,
        String debtLabel,
        String debtPaymentType,
        String debtPaymentNotes,
        boolean valid,
        List<ImportRowErrorDto> errors,
        Long createdExpenseId,
        Long createdDebtPaymentId
) {
}
