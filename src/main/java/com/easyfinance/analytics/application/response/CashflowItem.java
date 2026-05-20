package com.easyfinance.analytics.application.response;

import java.math.BigDecimal;

public record CashflowItem(
        String period,
        BigDecimal totalIncome,
        BigDecimal simpleExpenseOutflow,
        BigDecimal debtPaymentOutflow,
        BigDecimal totalOutflow,
        BigDecimal netCashflow
) {
}
