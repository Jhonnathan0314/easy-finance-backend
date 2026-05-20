package com.easyfinance.income.application.port.out;

import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.application.response.PageResponse;
import com.easyfinance.income.domain.model.Income;

import java.util.Optional;

public interface IncomeRepositoryPort {
    Income save(Income income);

    Optional<Income> findByAccountIdAndId(Long accountId, Long incomeId);

    PageResponse<Income> findAll(ListIncomesQuery query);
}
