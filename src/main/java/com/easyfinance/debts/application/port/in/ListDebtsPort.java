package com.easyfinance.debts.application.port.in;

import com.easyfinance.debts.application.query.ListDebtsQuery;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.debts.application.response.PageResponse;

public interface ListDebtsPort {
    PageResponse<DebtResponse> listDebts(ListDebtsQuery query);
}
