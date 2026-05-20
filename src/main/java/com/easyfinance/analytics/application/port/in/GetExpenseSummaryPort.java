package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.ExpenseSummaryQuery;
import com.easyfinance.analytics.application.response.ExpenseSummaryResponse;

public interface GetExpenseSummaryPort {
    ExpenseSummaryResponse getExpenseSummary(ExpenseSummaryQuery query);
}
