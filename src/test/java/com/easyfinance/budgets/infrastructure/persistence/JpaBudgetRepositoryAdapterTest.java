package com.easyfinance.budgets.infrastructure.persistence;

import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetJpaEntity;
import com.easyfinance.budgets.infrastructure.persistence.jpa.BudgetStatusJpa;
import com.easyfinance.budgets.infrastructure.persistence.jpa.SpringDataBudgetRepository;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaBudgetRepositoryAdapterTest {

    @Test
    void monthlyBudgetUsesAtomicUpsertRepositoryMethod() {
        SpringDataBudgetRepository repository = mock(SpringDataBudgetRepository.class);
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(repository);
        BudgetJpaEntity entity = new BudgetJpaEntity();
        entity.setId(10L);
        entity.setAccountId(1L);
        entity.setYear(2026);
        entity.setMonth(5);
        entity.setName("Budget 2026-05");
        entity.setStatus(BudgetStatusJpa.ACTIVE);
        when(repository.upsertMonthlyBudget(1L, 2026, 5, "Budget 2026-05")).thenReturn(entity);

        var budget = adapter.getOrCreateMonthlyBudget(1L, 2026, 5, "Budget 2026-05");

        assertThat(budget.id()).isEqualTo(10L);
        verify(repository).upsertMonthlyBudget(1L, 2026, 5, "Budget 2026-05");
    }

    @Test
    void duplicatePeriodConstraintIsTranslated() {
        SpringDataBudgetRepository repository = mock(SpringDataBudgetRepository.class);
        JpaBudgetRepositoryAdapter adapter = new JpaBudgetRepositoryAdapter(repository);
        when(repository.saveAndFlush(any(BudgetJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates constraint uq_budgets_account_year_month"));

        assertThatThrownBy(() -> adapter.save(Budget.create(1L, 2026, 6, "June")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("BUDGET_TARGET_ALREADY_EXISTS"));
    }
}
