package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.response.DebtResponse;

public interface GetDebtPort {
    DebtResponse getDebt(Long accountId, Long debtId);
}
