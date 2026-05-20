package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.IncomeBreakdownQuery;
import com.easyfinance.analytics.application.response.CategoryBreakdownResponse;

public interface GetIncomesByCategoryPort {
    CategoryBreakdownResponse getIncomesByCategory(IncomeBreakdownQuery query);
}
