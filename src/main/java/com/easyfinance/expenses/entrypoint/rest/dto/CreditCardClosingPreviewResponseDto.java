package com.easyfinance.expenses.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingPreviewResponseDto(
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long totalCount,
        List<CreditCardClosingCategoryItemDto> byCategory,
        List<ExpenseResponseDto> expenses
) {
}
