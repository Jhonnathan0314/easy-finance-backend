package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.response.PaymentMethodTypeBreakdownResponse;

public interface GetExpensesByPaymentMethodTypePort {
    PaymentMethodTypeBreakdownResponse getExpensesByPaymentMethodType(ExpenseBreakdownQuery query);
}
