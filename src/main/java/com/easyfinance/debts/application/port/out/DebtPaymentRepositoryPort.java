package com.easyfinance.debts.application.port.out;

import com.easyfinance.debts.application.query.ListDebtPaymentsQuery;
import com.easyfinance.debts.application.response.PageResponse;
import com.easyfinance.debts.domain.model.DebtPayment;

import java.util.Optional;

public interface DebtPaymentRepositoryPort {

    DebtPayment save(DebtPayment payment);

    Optional<DebtPayment> findByAccountIdAndDebtIdAndId(Long accountId, Long debtId, Long paymentId);

    PageResponse<DebtPayment> findAll(ListDebtPaymentsQuery query);
}
