package com.easyfinance.budgets.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BudgetExpenseExecutionQueryPort {
    Map<Long, BigDecimal> sumManualExecutionByCategory(Long accountId, LocalDate from, LocalDate to, List<Long> categoryIds);

    Map<CategoryParticipantKey, BigDecimal> sumManualExecutionByCategoryAndParticipant(
            Long accountId,
            LocalDate from,
            LocalDate to,
            List<CategoryParticipantKey> keys
    );

    record CategoryParticipantKey(Long categoryId, Long participantId) {
    }
}
