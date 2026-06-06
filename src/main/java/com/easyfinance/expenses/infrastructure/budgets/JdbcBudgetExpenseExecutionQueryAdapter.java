package com.easyfinance.expenses.infrastructure.budgets;

import com.easyfinance.budgets.application.port.out.BudgetExpenseExecutionQueryPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JdbcBudgetExpenseExecutionQueryAdapter implements BudgetExpenseExecutionQueryPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcBudgetExpenseExecutionQueryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<Long, BigDecimal> sumManualExecutionByCategory(Long accountId, LocalDate from, LocalDate to, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        String sql = """
                SELECT e.category_id, COALESCE(SUM(e.amount), 0) AS amount
                FROM expenses e
                WHERE e.account_id = :accountId
                  AND e.expense_date BETWEEN :from AND :to
                  AND e.status = 'ACTIVE'
                  AND e.expense_type = 'SIMPLE'
                  AND e.source_type IN ('MANUAL', 'IMPORT')
                  AND e.category_id IN (:categoryIds)
                GROUP BY e.category_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("from", from)
                .addValue("to", to)
                .addValue("categoryIds", categoryIds);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> Map.entry(rs.getLong("category_id"), rs.getBigDecimal("amount")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Map<CategoryParticipantKey, BigDecimal> sumManualExecutionByCategoryAndParticipant(
            Long accountId,
            LocalDate from,
            LocalDate to,
            List<CategoryParticipantKey> keys
    ) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        Set<CategoryParticipantKey> requestedKeys = Set.copyOf(keys);
        List<Long> categoryIds = keys.stream().map(CategoryParticipantKey::categoryId).distinct().toList();
        List<Long> participantIds = keys.stream().map(CategoryParticipantKey::participantId).distinct().toList();
        String sql = """
                SELECT e.category_id, e.participant_id, COALESCE(SUM(e.amount), 0) AS amount
                FROM expenses e
                WHERE e.account_id = :accountId
                  AND e.expense_date BETWEEN :from AND :to
                  AND e.status = 'ACTIVE'
                  AND e.expense_type = 'SIMPLE'
                  AND e.source_type IN ('MANUAL', 'IMPORT')
                  AND e.category_id IN (:categoryIds)
                  AND e.participant_id IN (:participantIds)
                GROUP BY e.category_id, e.participant_id
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountId", accountId)
                .addValue("from", from)
                .addValue("to", to)
                .addValue("categoryIds", categoryIds)
                .addValue("participantIds", participantIds);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> Map.entry(
                        new CategoryParticipantKey(rs.getLong("category_id"), rs.getLong("participant_id")),
                        rs.getBigDecimal("amount")
                ))
                .stream()
                .filter(entry -> requestedKeys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
