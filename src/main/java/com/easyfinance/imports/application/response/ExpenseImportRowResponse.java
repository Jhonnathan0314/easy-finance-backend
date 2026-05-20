package com.easyfinance.imports.application.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseImportRowResponse(
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
        boolean valid,
        List<ImportRowErrorResponse> errors,
        Long createdExpenseId
) {
}
