package com.easyfinance.analytics.application.port.in;
import com.easyfinance.analytics.application.query.DebtAnalyticsQuery;
import com.easyfinance.analytics.application.response.DebtAnalyticsResponse;
public interface GetDebtAnalyticsPort { DebtAnalyticsResponse getDebtAnalytics(DebtAnalyticsQuery query); }
