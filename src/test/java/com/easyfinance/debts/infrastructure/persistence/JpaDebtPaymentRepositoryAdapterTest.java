package com.easyfinance.debts.infrastructure.persistence;

import com.easyfinance.debts.domain.model.DebtPayment;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtPaymentJpaEntity;
import com.easyfinance.debts.infrastructure.persistence.jpa.SpringDataDebtPaymentRepository;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaDebtPaymentRepositoryAdapterTest {

    @Test
    void updateInAnotherDebtOrAccountReturnsNotFound() {
        SpringDataDebtPaymentRepository repository = mock(SpringDataDebtPaymentRepository.class);
        JpaDebtPaymentRepositoryAdapter adapter = new JpaDebtPaymentRepositoryAdapter(repository);
        DebtPayment payment = payment(99L);
        when(repository.findByAccountIdAndDebtIdAndId(1L, 5L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(payment))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_PAYMENT_NOT_FOUND"));
        verify(repository, never()).saveAndFlush(any(DebtPaymentJpaEntity.class));
    }

    @Test
    void findByScopedIdsUsesRepositoryScopedMethod() {
        SpringDataDebtPaymentRepository repository = mock(SpringDataDebtPaymentRepository.class);
        JpaDebtPaymentRepositoryAdapter adapter = new JpaDebtPaymentRepositoryAdapter(repository);

        adapter.findByAccountIdAndDebtIdAndId(1L, 5L, 99L);

        verify(repository).findByAccountIdAndDebtIdAndId(1L, 5L, 99L);
    }

    private static DebtPayment payment(Long id) {
        return DebtPayment.restore(id, 1L, 5L, 10L, DebtPaymentType.INSTALLMENT, Money.cop(new BigDecimal("50000")), LocalDate.now(), null, DebtPaymentStatus.ACTIVE, Instant.now(), Instant.now());
    }
}
