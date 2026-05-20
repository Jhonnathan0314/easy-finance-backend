package com.easyfinance.expenses.infrastructure.debts;

import com.easyfinance.debts.application.port.out.ExpenseOriginValidationPort;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ExpenseOriginValidationAdapter implements ExpenseOriginValidationPort {

    private final ExpenseRepositoryPort expenseRepository;

    public ExpenseOriginValidationAdapter(ExpenseRepositoryPort expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public void validateInstallmentOrigin(Long accountId, Long originExpenseId) {
        var expense = expenseRepository.findByAccountIdAndId(accountId, originExpenseId)
                .orElseThrow(() -> new NotFoundException("DEBT_ORIGIN_EXPENSE_NOT_FOUND", "Debt origin expense was not found."));
        if (expense.expenseType() != ExpenseType.INSTALLMENT) {
            throw new BusinessRuleViolationException("DEBT_ORIGIN_EXPENSE_INVALID_TYPE", "Debt origin expense must be an INSTALLMENT expense.");
        }
    }
}
