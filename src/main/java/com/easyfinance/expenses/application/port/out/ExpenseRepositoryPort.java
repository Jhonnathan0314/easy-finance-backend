package com.easyfinance.expenses.application.port.out;

import com.easyfinance.expenses.application.query.CreditCardClosingQuery;
import com.easyfinance.expenses.application.query.ListExpensesQuery;
import com.easyfinance.expenses.application.response.PageResponse;
import com.easyfinance.expenses.domain.model.Expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepositoryPort {

    Expense save(Expense expense);

    List<Expense> saveAll(List<Expense> expenses);

    Optional<Expense> findByAccountIdAndId(Long accountId, Long expenseId);

    PageResponse<Expense> findAll(ListExpensesQuery query);

    List<Expense> findEligibleForClosing(CreditCardClosingQuery query);
}
