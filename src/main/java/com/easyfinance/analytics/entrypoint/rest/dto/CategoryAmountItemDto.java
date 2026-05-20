package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record CategoryAmountItemDto(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        Long count
) {
}
