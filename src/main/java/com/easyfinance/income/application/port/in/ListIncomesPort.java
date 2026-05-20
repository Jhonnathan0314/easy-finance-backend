package com.easyfinance.income.application.port.in;

import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.application.response.IncomeResponse;
import com.easyfinance.income.application.response.PageResponse;

public interface ListIncomesPort {
    PageResponse<IncomeResponse> listIncomes(ListIncomesQuery query);
}
