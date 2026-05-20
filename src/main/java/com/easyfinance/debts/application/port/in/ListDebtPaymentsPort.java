package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.query.ListDebtPaymentsQuery;
import com.easyfinance.debts.application.response.DebtPaymentResponse;
import com.easyfinance.debts.application.response.PageResponse;

public interface ListDebtPaymentsPort {

    PageResponse<DebtPaymentResponse> listDebtPayments(ListDebtPaymentsQuery query);
}
