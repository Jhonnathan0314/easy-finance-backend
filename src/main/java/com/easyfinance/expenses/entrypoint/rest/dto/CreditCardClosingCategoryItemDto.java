package com.easyfinance.expenses.entrypoint.rest.dto;

import java.math.BigDecimal;

public record CreditCardClosingCategoryItemDto(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        long count
) {
}
