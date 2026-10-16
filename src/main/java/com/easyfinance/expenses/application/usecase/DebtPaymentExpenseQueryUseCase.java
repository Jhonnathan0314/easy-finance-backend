package com.easyfinance.expenses.application.usecase;

import com.easyfinance.expenses.application.port.in.DebtPaymentExpenseQueryPort;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DebtPaymentExpenseQueryUseCase implements DebtPaymentExpenseQueryPort {

    private final ExpenseRepositoryPort expenseRepository;

    public DebtPaymentExpenseQueryUseCase(ExpenseRepositoryPort expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveExpenseForDebtPayment(Long accountId, Long debtPaymentId) {
        return expenseRepository.existsActiveBySourceDebtPaymentId(accountId, debtPaymentId);
    }
}
