package com.easyfinance.income.application.port.in;

public interface CancelIncomePort {
    void cancelIncome(Long accountId, Long incomeId);
}
