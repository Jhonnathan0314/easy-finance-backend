package com.easyfinance.expenses.application.port.in;

import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.application.response.ExpenseResponse;
import com.easyfinance.expenses.application.response.PageResponse;

public interface ListExpensesPort {
    PageResponse<ExpenseResponse> listExpenses(ListExpensesQuery query);
}
