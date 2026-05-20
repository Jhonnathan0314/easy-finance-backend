package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.MonthlyAnalyticsQuery;
import com.easyfinance.analytics.application.response.MonthlySummaryResponse;

public interface GetMonthlySummaryPort {
    MonthlySummaryResponse getMonthlySummary(MonthlyAnalyticsQuery query);
}
