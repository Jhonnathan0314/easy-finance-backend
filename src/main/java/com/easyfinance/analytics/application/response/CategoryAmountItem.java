package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record CategoryAmountItem(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        Long count
) {
}
