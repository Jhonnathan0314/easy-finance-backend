package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.CashflowSummaryQuery;
import com.easyfinance.analytics.application.response.CashflowSummaryResponse;

public interface GetCashflowSummaryPort {
    CashflowSummaryResponse getCashflowSummary(CashflowSummaryQuery query);
}
