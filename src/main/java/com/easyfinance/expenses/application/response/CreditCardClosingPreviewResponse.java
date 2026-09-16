package com.easyfinance.expenses.application.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreditCardClosingPreviewResponse(
        Long paymentMethodId,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long totalCount,
        List<CreditCardClosingCategoryItem> byCategory,
        List<ExpenseResponse> expenses
) {
}
