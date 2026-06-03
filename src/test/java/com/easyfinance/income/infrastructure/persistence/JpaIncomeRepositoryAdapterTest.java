package com.easyfinance.income.infrastructure.persistence;

import com.easyfinance.income.domain.model.Income;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.infrastructure.persistence.jpa.IncomeJpaEntity;
import com.easyfinance.income.infrastructure.persistence.jpa.SpringDataIncomeRepository;
import com.easyfinance.shared.application.PageQuery;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void listAppliesYearRangeWhenMonthIsAbsent() throws Exception {
        assertDateRangeApplied(
                new ListIncomesQuery(1L, 2026, null, null, null, null, null, null, null, PageQuery.of(0, 20), null),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
    }

    @Test
    void listAppliesMonthRangeWhenYearAndMonthArePresent() throws Exception {
        assertDateRangeApplied(
                new ListIncomesQuery(1L, 2026, 5, null, null, null, null, null, null, PageQuery.of(0, 20), null),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );
    }

    @SuppressWarnings("unchecked")
    private void assertDateRangeApplied(ListIncomesQuery query, LocalDate expectedStart, LocalDate expectedEnd) throws Exception {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        ArgumentCaptor<Specification<IncomeJpaEntity>> specificationCaptor = ArgumentCaptor.forClass((Class) Specification.class);
        adapter.findAll(query);
        verify(repository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Specification<IncomeJpaEntity> specification = specificationCaptor.getValue();
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<Object> criteriaQuery = mock(CriteriaQuery.class);
        Root<IncomeJpaEntity> root = mock(Root.class);
        Path<Object> accountPath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);
        Path<LocalDate> datePath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("accountId")).thenReturn(accountPath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("incomeDate")).thenReturn((Path) datePath);
        when(builder.equal(any(), any())).thenReturn(predicate);
        when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        when(builder.greaterThanOrEqualTo(eq(datePath), eq(expectedStart))).thenReturn(predicate);
        when(builder.lessThanOrEqualTo(eq(datePath), eq(expectedEnd))).thenReturn(predicate);

        specification.toPredicate(root, criteriaQuery, builder);

        verify(builder).greaterThanOrEqualTo(datePath, expectedStart);
        verify(builder).lessThanOrEqualTo(datePath, expectedEnd);
    }
}
