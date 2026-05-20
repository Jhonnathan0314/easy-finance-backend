package com.easyfinance.debts.application.port.out;

import com.easyfinance.debts.application.query.ListDebtsQuery;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.Debt;

import java.util.Optional;

public interface DebtRepositoryPort {

    Debt save(Debt debt);

    Optional<Debt> findByAccountIdAndId(Long accountId, Long debtId);

    Optional<Debt> findByAccountIdAndIdForUpdate(Long accountId, Long debtId);

    PageResponse<Debt> findAll(ListDebtsQuery query);
}
