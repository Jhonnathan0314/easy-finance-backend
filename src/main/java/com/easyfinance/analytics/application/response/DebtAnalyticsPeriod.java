package com.easyfinance.analytics.application.response;
import java.math.BigDecimal;
public record DebtAnalyticsPeriod(String period, BigDecimal capitalPaid, BigDecimal interestPaid, BigDecimal totalPaid) {}
