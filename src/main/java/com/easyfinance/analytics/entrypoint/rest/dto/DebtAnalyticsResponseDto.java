package com.easyfinance.analytics.entrypoint.rest.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public record DebtAnalyticsResponseDto(Long accountId, String from, String to, String groupBy, String state,
                                       Summary summary, List<Period> periods, List<Debt> debts, Instant generatedAt) {
    public record Summary(BigDecimal originalAmount, BigDecimal remainingAmount, BigDecimal capitalPaid, BigDecimal interestPaid,
                          BigDecimal totalPaid, Long activeDebtsCount, Long paidDebtsCount, Long cancelledDebtsCount, Long debtsCount) {}
    public record Period(String period, BigDecimal capitalPaid, BigDecimal interestPaid, BigDecimal totalPaid) {}
    public record Debt(Long debtId, String name, String state, BigDecimal originalAmount, BigDecimal capitalPaid, BigDecimal interestPaid,
                       BigDecimal totalPaid, BigDecimal remainingAmount, BigDecimal paidPercentage, Long paymentsCount) {}
}
