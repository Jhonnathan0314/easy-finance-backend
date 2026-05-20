package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.CashflowQuery;
import com.easyfinance.analytics.application.response.CashflowResponse;

public interface GetCashflowPort {
    CashflowResponse getCashflow(CashflowQuery query);
}
