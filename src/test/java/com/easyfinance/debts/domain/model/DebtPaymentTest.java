package com.easyfinance.debts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtPaymentTest {

    @Test
    void createPaymentWorks() {
        DebtPayment payment = DebtPayment.create(1L, 2L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("50000")), Money.zeroCop(), LocalDate.of(2026, 5, 11), "First payment");

        assertThat(payment.status()).isEqualTo(DebtPaymentStatus.ACTIVE);
        assertThat(payment.amount().amount()).isEqualByComparingTo("50000.00");
        assertThat(payment.capitalAmount().amount()).isEqualByComparingTo("50000.00");
        assertThat(payment.interestAmount().amount()).isEqualByComparingTo("0.00");
        assertThat(payment.notes()).isEqualTo("First payment");
    }

    @Test
    void createPaymentWithInterestSplitsCapitalAndInterest() {
        DebtPayment payment = DebtPayment.create(1L, 2L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("80000")), Money.cop(new BigDecimal("20000")), LocalDate.of(2026, 5, 11), "Installment with interest");

        assertThat(payment.capitalAmount().amount()).isEqualByComparingTo("80000.00");
        assertThat(payment.interestAmount().amount()).isEqualByComparingTo("20000.00");
        assertThat(payment.amount().amount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void rejectInvalidAmount() {
        assertThatThrownBy(() -> DebtPayment.create(1L, 2L, 10L, DebtPaymentType.INSTALLMENT, Money.zeroCop(), Money.zeroCop(), LocalDate.now(), null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_AMOUNT_INVALID"));
    }

    @Test
    void rejectMissingInterestAmount() {
        assertThatThrownBy(() -> DebtPayment.create(1L, 2L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("50000")), null, LocalDate.now(), null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_INTEREST_AMOUNT_INVALID"));
    }

    @Test
    void rejectInterestOnCapitalPayment() {
        assertThatThrownBy(() -> DebtPayment.create(1L, 2L, 10L, DebtPaymentType.CAPITAL_PAYMENT, Money.cop(new BigDecimal("50000")), Money.cop(new BigDecimal("1")), LocalDate.now(), null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_CAPITAL_PAYMENT_INTEREST_NOT_ALLOWED"));
    }

    @Test
    void rejectMissingDate() {
        assertThatThrownBy(() -> DebtPayment.create(1L, 2L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("50000")), Money.zeroCop(), null, null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_DATE_INVALID"));
    }

    @Test
    void rejectMissingType() {
        assertThatThrownBy(() -> DebtPayment.create(1L, 2L, 10L, null, Money.cop(new BigDecimal("50000")), Money.zeroCop(), LocalDate.now(), null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_TYPE_INVALID"));
    }
}
