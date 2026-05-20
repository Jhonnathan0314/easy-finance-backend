package com.easyfinance.analytics.entrypoint.rest.dto;

import java.math.BigDecimal;

public record CashflowItemDto(
        String period,
        BigDecimal totalIncome,
        BigDecimal simpleExpenseOutflow,
        BigDecimal debtPaymentOutflow,
        BigDecimal totalOutflow,
        BigDecimal netCashflow
) {
}
