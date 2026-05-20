package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.response.DebtSummaryResponse;

public interface GetDebtSummaryPort {
    DebtSummaryResponse getDebtSummary(Long accountId);
}
