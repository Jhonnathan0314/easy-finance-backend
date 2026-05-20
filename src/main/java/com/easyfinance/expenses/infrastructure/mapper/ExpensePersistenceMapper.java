package com.easyfinance.expenses.infrastructure.mapper;

import com.easyfinance.expenses.domain.model.Expense;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.expenses.domain.model.ExpenseStatus;
import com.easyfinance.expenses.domain.model.ExpenseType;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseJpaEntity;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpensePaymentStateJpa;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseStatusJpa;
import com.easyfinance.expenses.infrastructure.persistence.jpa.ExpenseTypeJpa;
import com.easyfinance.shared.domain.CurrencyCode;
import com.easyfinance.shared.domain.Money;

public class ExpensePersistenceMapper {

    public Expense toDomain(ExpenseJpaEntity entity) {
        return Expense.restore(
                entity.getId(),
                entity.getAccountId(),
                entity.getCategoryId(),
                entity.getPaymentMethodId(),
                entity.getParticipantId(),
                entity.getDescription(),
                new Money(entity.getAmount(), CurrencyCode.valueOf(entity.getCurrency())),
                entity.getExpenseDate(),
                ExpensePaymentState.valueOf(entity.getPaymentState().name()),
                ExpenseStatus.valueOf(entity.getStatus().name()),
                ExpenseType.valueOf(entity.getExpenseType().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ExpenseJpaEntity toEntity(Expense expense) {
        ExpenseJpaEntity entity = new ExpenseJpaEntity();
        copyToEntity(expense, entity);
        return entity;
    }

    public void copyToEntity(Expense expense, ExpenseJpaEntity entity) {
        entity.setId(expense.id());
        entity.setAccountId(expense.accountId());
        entity.setCategoryId(expense.categoryId());
        entity.setPaymentMethodId(expense.paymentMethodId());
        entity.setParticipantId(expense.participantId());
        entity.setDescription(expense.description());
        entity.setAmount(expense.amount().amount());
        entity.setCurrency(expense.amount().currency().name());
        entity.setExpenseDate(expense.expenseDate());
        entity.setPaymentState(ExpensePaymentStateJpa.valueOf(expense.paymentState().name()));
        entity.setStatus(ExpenseStatusJpa.valueOf(expense.status().name()));
        entity.setExpenseType(ExpenseTypeJpa.valueOf(expense.expenseType().name()));
    }
}
