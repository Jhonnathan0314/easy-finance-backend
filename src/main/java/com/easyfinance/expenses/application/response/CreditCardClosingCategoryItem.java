package com.easyfinance.expenses.application.response;

import java.math.BigDecimal;

public record CreditCardClosingCategoryItem(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        long count
) {
}
