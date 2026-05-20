package com.easyfinance.expenses.application.port.out;

import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.application.response.PageResponse;
import com.easyfinance.expenses.domain.model.Expense;

import java.util.Optional;

public interface ExpenseRepositoryPort {

    Expense save(Expense expense);

    Optional<Expense> findByAccountIdAndId(Long accountId, Long expenseId);

    PageResponse<Expense> findAll(ListExpensesQuery query);
}
