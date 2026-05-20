package com.easyfinance.debts.infrastructure.persistence;

import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.debts.infrastructure.persistence.jpa.DebtJpaEntity;
import com.easyfinance.debts.infrastructure.persistence.jpa.SpringDataDebtRepository;
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

class JpaDebtRepositoryAdapterTest {

    @Test
    void updateInAnotherAccountReturnsNotFound() {
        SpringDataDebtRepository repository = mock(SpringDataDebtRepository.class);
        JpaDebtRepositoryAdapter adapter = new JpaDebtRepositoryAdapter(repository);
        Debt debt = debt(99L, 2L);
        when(repository.findByAccountIdAndId(2L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(debt))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("DEBT_NOT_FOUND"));
        verify(repository, never()).saveAndFlush(any(DebtJpaEntity.class));
    }

    @Test
    void findForUpdateUsesPessimisticRepositoryMethod() {
        SpringDataDebtRepository repository = mock(SpringDataDebtRepository.class);
        JpaDebtRepositoryAdapter adapter = new JpaDebtRepositoryAdapter(repository);

        adapter.findByAccountIdAndIdForUpdate(1L, 5L);

        verify(repository).findByAccountIdAndIdForUpdate(1L, 5L);
    }

    private static Debt debt(Long id, Long accountId) {
        return Debt.restore(id, accountId, 10L, null, DebtSourceType.MANUAL, "Loan", null, Money.cop(new BigDecimal("100000")), Money.cop(new BigDecimal("100000")), null, null, LocalDate.now(), null, DebtState.ACTIVE, null, Instant.now(), Instant.now());
    }
}
