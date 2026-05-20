package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.MonthlyAnalyticsQuery;
import com.easyfinance.analytics.application.response.BudgetVsExpensesByCategoryResponse;

public interface GetBudgetVsExpensesByCategoryPort {
    BudgetVsExpensesByCategoryResponse getBudgetVsExpensesByCategory(MonthlyAnalyticsQuery query);
}
