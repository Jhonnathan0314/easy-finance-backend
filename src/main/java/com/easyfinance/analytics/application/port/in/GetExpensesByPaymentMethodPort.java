package com.easyfinance.analytics.application.port.in;

import com.easyfinance.analytics.application.query.ExpenseBreakdownQuery;
import com.easyfinance.analytics.application.response.PaymentMethodBreakdownResponse;

public interface GetExpensesByPaymentMethodPort {
    PaymentMethodBreakdownResponse getExpensesByPaymentMethod(ExpenseBreakdownQuery query);
}
