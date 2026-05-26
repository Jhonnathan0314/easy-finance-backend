package com.easyfinance.debts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtTest {

    @Test
    void createManualDebtWorks() {
        Debt debt = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.of(2026, 5, 11), null, null);

        assertThat(debt.sourceType()).isEqualTo(DebtSourceType.MANUAL);
        assertThat(debt.remainingBalance()).isEqualTo(debt.totalAmount());
        assertThat(debt.scheduledTotalAmount()).isEqualTo(debt.totalAmount());
        assertThat(debt.endDate()).isNull();
        assertThat(debt.state()).isEqualTo(DebtState.ACTIVE);
    }

    @Test
    void createInstallmentDebtCalculatesEndDate() {
        Debt debt = Debt.createFromInstallmentExpense(1L, 10L, 99L, "Laptop", null, Money.cop(new BigDecimal("1000000")), 6, Money.cop(new BigDecimal("200000")), LocalDate.of(2026, 6, 1), null);

        assertThat(debt.sourceType()).isEqualTo(DebtSourceType.INSTALLMENT_EXPENSE);
        assertThat(debt.endDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(debt.totalAmount().amount()).isEqualByComparingTo("1000000.00");
        assertThat(debt.scheduledTotalAmount().amount()).isEqualByComparingTo("1200000.00");
    }

    @Test
    void calculateEndDateFromEndOfJanuaryUsesJavaCalendarMonths() {
        assertThat(Debt.calculateEndDate(LocalDate.of(2026, 1, 31), 1))
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void calculateEndDateFromLeapDayUsesJavaCalendarMonths() {
        assertThat(Debt.calculateEndDate(LocalDate.of(2024, 2, 29), 12))
                .isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @Test
    void calculateEndDateAcrossYearBoundaryUsesJavaCalendarMonths() {
        assertThat(Debt.calculateEndDate(LocalDate.of(2026, 12, 31), 2))
                .isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void rejectInvalidAmount() {
        assertThatThrownBy(() -> Debt.createManual(1L, 10L, "Loan", null, Money.zeroCop(), null, null, LocalDate.now(), null, null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_AMOUNT_INVALID"));
    }

    @Test
    void rejectInvalidInstallmentCount() {
        assertThatThrownBy(() -> Debt.createFromInstallmentExpense(1L, 10L, 99L, "Laptop", null, Money.cop(new BigDecimal("1200000")), 0, Money.cop(new BigDecimal("200000")), LocalDate.now(), null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_INSTALLMENT_COUNT_INVALID"));
    }

    @Test
    void rejectScheduledTotalLowerThanPrincipal() {
        assertThatThrownBy(() -> Debt.createFromInstallmentExpense(
                1L,
                10L,
                99L,
                "Laptop",
                null,
                Money.cop(new BigDecimal("1200000")),
                6,
                Money.cop(new BigDecimal("100000")),
                LocalDate.now(),
                null
        )).isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_SCHEDULED_TOTAL_INVALID"));
    }

    @Test
    void cancelActiveDebtWorksAndCannotCancelTwice() {
        Debt cancelled = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, null)
                .cancel();

        assertThat(cancelled.state()).isEqualTo(DebtState.CANCELLED);
        assertThatThrownBy(cancelled::cancel)
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_ALREADY_CANCELLED"));
    }

    @Test
    void partialPaymentReducesBalanceAndKeepsDebtActive() {
        Debt debt = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, null);

        Debt updated = debt.applyPayment(Money.cop(new BigDecimal("40000")));

        assertThat(updated.remainingBalance().amount()).isEqualByComparingTo("60000.00");
        assertThat(updated.state()).isEqualTo(DebtState.ACTIVE);
    }

    @Test
    void totalPaymentMarksDebtAsPaid() {
        Debt debt = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, null);

        Debt updated = debt.applyPayment(Money.cop(new BigDecimal("100000")));

        assertThat(updated.remainingBalance().amount()).isEqualByComparingTo("0.00");
        assertThat(updated.state()).isEqualTo(DebtState.PAID);
    }

    @Test
    void paymentGreaterThanBalanceFails() {
        Debt debt = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, null);

        assertThatThrownBy(() -> debt.applyPayment(Money.cop(new BigDecimal("100001"))))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE"));
    }

    @Test
    void paidDebtDoesNotAcceptPayment() {
        Debt paid = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, null)
                .applyPayment(Money.cop(new BigDecimal("100000")));

        assertThatThrownBy(() -> paid.applyPayment(Money.cop(new BigDecimal("1"))))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_ALREADY_PAID"));
    }

    @Test
    void cancelledDebtDoesNotAcceptPayment() {
        Debt cancelled = Debt.createManual(1L, 10L, "Loan", null, Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, null)
                .cancel();

        assertThatThrownBy(() -> cancelled.applyPayment(Money.cop(new BigDecimal("1"))))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_CANCELLED"));
    }
}
