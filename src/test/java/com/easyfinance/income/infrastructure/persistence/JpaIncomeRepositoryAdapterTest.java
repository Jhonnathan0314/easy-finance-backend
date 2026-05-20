package com.easyfinance.income.infrastructure.persistence;

import com.easyfinance.income.domain.model.Income;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.income.infrastructure.persistence.jpa.SpringDataIncomeRepository;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaIncomeRepositoryAdapterTest {

    private final SpringDataIncomeRepository repository = mock(SpringDataIncomeRepository.class);
    private final JpaIncomeRepositoryAdapter adapter = new JpaIncomeRepositoryAdapter(repository);

    @Test
    void updateUsesAccountBoundary() {
        Income income = Income.restore(
                5L,
                2L,
                3L,
                10L,
                "Salary",
                Money.cop(new BigDecimal("2500000")),
                LocalDate.of(2026, 5, 10),
                IncomeStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        when(repository.findByAccountIdAndId(2L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(income))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("INCOME_NOT_FOUND"));
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
}
