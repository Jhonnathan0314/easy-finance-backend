package com.easyfinance.analytics.application.response;
import java.math.BigDecimal;
public record DebtAnalyticsDebt(Long debtId, String name, String state, BigDecimal originalAmount, BigDecimal capitalPaid, BigDecimal interestPaid, BigDecimal totalPaid, BigDecimal remainingAmount, BigDecimal paidPercentage, Long paymentsCount) {}
