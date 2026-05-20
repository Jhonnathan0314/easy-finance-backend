package com.easyfinance.income.application.port.in;

import com.easyfinance.income.application.response.IncomeResponse;

public interface GetIncomePort {
    IncomeResponse getIncome(Long accountId, Long incomeId);
}
