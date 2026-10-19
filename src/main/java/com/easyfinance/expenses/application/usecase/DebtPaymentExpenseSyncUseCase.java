package com.easyfinance.expenses.application.usecase;

import com.easyfinance.expenses.application.port.in.DebtPaymentExpenseSyncPort;
import com.easyfinance.expenses.application.port.out.ExpenseRepositoryPort;
import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.shared.domain.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DebtPaymentExpenseSyncUseCase implements DebtPaymentExpenseSyncPort {

    private final ExpenseRepositoryPort expenseRepository;

    public DebtPaymentExpenseSyncUseCase(ExpenseRepositoryPort expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    @Transactional
    public void updateExpenseForDebtPayment(Long accountId, Long debtPaymentId, Money amount, LocalDate expenseDate) {
        expenseRepository.findActiveBySourceDebtPaymentId(accountId, debtPaymentId).ifPresent(expense -> {
            Expense updated = expense.update(
                    expense.categoryId(),
                    expense.paymentMethodId(),
                    expense.participantId(),
                    expense.description(),
                    amount,
                    expenseDate,
                    expense.paymentState()
            );
            expenseRepository.save(updated);
        });
    }

    @Override
    @Transactional
    public void cancelExpenseForDebtPayment(Long accountId, Long debtPaymentId) {
        expenseRepository.findActiveBySourceDebtPaymentId(accountId, debtPaymentId)
                .ifPresent(expense -> expenseRepository.save(expense.cancel()));
    }
}
