package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.MonthlyAnalyticsQuery;
import com.easyfinance.analytics.application.response.BudgetSummaryResponse;

public interface GetBudgetSummaryPort {
    BudgetSummaryResponse getBudgetSummary(MonthlyAnalyticsQuery query);
}
