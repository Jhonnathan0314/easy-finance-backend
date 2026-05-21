package com.easyfinance.expenses.infrastructure.budgets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcBudgetExpenseExecutionQueryAdapterTest {

    @Test
    void sumsOnlyActiveSimpleManualOrImportedExpensesScopedByAccountMonthAndCategories() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcBudgetExpenseExecutionQueryAdapter adapter = new JdbcBudgetExpenseExecutionQueryAdapter(jdbcTemplate);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
        when(jdbcTemplate.query(sqlCaptor.capture(), paramsCaptor.capture(), any(RowMapper.class)))
                .thenReturn(List.of(Map.entry(7L, new BigDecimal("45000.00"))));

        Map<Long, BigDecimal> result = adapter.sumManualExecutionByCategory(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                List.of(7L, 8L)
        );

        assertThat(result).containsEntry(7L, new BigDecimal("45000.00"));
        assertThat(sqlCaptor.getValue())
                .contains("e.account_id = :accountId")
                .contains("e.expense_date BETWEEN :from AND :to")
                .contains("e.status = 'ACTIVE'")
                .contains("e.expense_type = 'SIMPLE'")
                .contains("e.source_type IN ('MANUAL', 'IMPORT')")
                .contains("e.category_id IN (:categoryIds)");
        MapSqlParameterSource params = (MapSqlParameterSource) paramsCaptor.getValue();
        assertThat(params.getValue("accountId")).isEqualTo(1L);
        assertThat(params.getValue("from")).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(params.getValue("to")).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(params.getValue("categoryIds")).isEqualTo(List.of(7L, 8L));
    }

    @Test
    void emptyCategoriesDoNotQueryDatabase() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        JdbcBudgetExpenseExecutionQueryAdapter adapter = new JdbcBudgetExpenseExecutionQueryAdapter(jdbcTemplate);

        Map<Long, BigDecimal> result = adapter.sumManualExecutionByCategory(1L, LocalDate.now(), LocalDate.now(), List.of());

        assertThat(result).isEmpty();
        verify(jdbcTemplate, never()).query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class));
    }
}
