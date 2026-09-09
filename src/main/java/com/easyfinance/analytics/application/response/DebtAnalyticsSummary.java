package com.easyfinance.analytics.application.response;
import java.math.BigDecimal;
public record DebtAnalyticsSummary(BigDecimal originalAmount, BigDecimal remainingAmount, BigDecimal capitalPaid, BigDecimal interestPaid, BigDecimal totalPaid, Long activeDebtsCount, Long paidDebtsCount, Long cancelledDebtsCount, Long debtsCount) {}
