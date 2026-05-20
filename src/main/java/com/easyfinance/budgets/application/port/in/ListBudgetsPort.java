package com.easyfinance.budgets.application.port.in;

import com.easyfinance.budgets.application.query.ListBudgetsQuery;
import com.easyfinance.budgets.application.response.BudgetResponse;
import com.easyfinance.budgets.application.response.PageResponse;

public interface ListBudgetsPort {
    PageResponse<BudgetResponse> listBudgets(ListBudgetsQuery query);
}
