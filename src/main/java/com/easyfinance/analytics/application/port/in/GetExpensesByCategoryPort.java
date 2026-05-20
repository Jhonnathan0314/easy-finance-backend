package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.response.CategoryBreakdownResponse;

public interface GetExpensesByCategoryPort {
    CategoryBreakdownResponse getExpensesByCategory(ExpenseBreakdownQuery query);
}
