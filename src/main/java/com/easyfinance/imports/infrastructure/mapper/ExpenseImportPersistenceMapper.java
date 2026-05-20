package com.easyfinance.imports.infrastructure.mapper;

import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.imports.domain.model.ExpenseImportBatch;
import com.easyfinance.imports.domain.model.ExpenseImportRow;
import com.easyfinance.imports.domain.model.ExpenseImportStatus;
import com.easyfinance.imports.infrastructure.persistence.jpa.ExpenseImportBatchJpaEntity;
import com.easyfinance.imports.infrastructure.persistence.jpa.ExpenseImportRowJpaEntity;
import com.easyfinance.imports.infrastructure.persistence.jpa.ExpenseImportStatusJpa;
import com.easyfinance.shared.domain.Money;

import java.util.List;

public class ExpenseImportPersistenceMapper {

    public ExpenseImportBatchJpaEntity toBatchEntity(ExpenseImportBatch batch) {
        ExpenseImportBatchJpaEntity entity = new ExpenseImportBatchJpaEntity();
        copyToBatchEntity(batch, entity);
        return entity;
    }

    public void copyToBatchEntity(ExpenseImportBatch batch, ExpenseImportBatchJpaEntity entity) {
        entity.setId(batch.id());
        entity.setAccountId(batch.accountId());
        entity.setParticipantId(batch.participantId());
        entity.setOriginalFilename(batch.originalFilename());
        entity.setStatus(ExpenseImportStatusJpa.valueOf(batch.status().name()));
        entity.setTotalRows(batch.totalRows());
        entity.setValidRows(batch.validRows());
        entity.setInvalidRows(batch.invalidRows());
        entity.setConfirmedAt(batch.confirmedAt());
    }

    public ExpenseImportBatch toBatchDomain(ExpenseImportBatchJpaEntity entity, List<ExpenseImportRow> rows) {
        return new ExpenseImportBatch(
                entity.getId(),
                entity.getAccountId(),
                entity.getParticipantId(),
                entity.getOriginalFilename(),
                ExpenseImportStatus.valueOf(entity.getStatus().name()),
                entity.getTotalRows(),
                entity.getValidRows(),
                entity.getInvalidRows(),
                entity.getConfirmedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                rows
        );
    }

    public ExpenseImportRowJpaEntity toRowEntity(ExpenseImportRow row, Long batchId) {
        ExpenseImportRowJpaEntity entity = new ExpenseImportRowJpaEntity();
        entity.setId(row.id());
        entity.setAccountId(row.accountId());
        entity.setBatchId(batchId);
        entity.setRowNumber(row.rowNumber());
        entity.setExpenseDate(row.expenseDate());
        entity.setDescription(row.description());
        entity.setAmount(row.amount() == null ? null : row.amount().amount());
        entity.setCurrency("COP");
        entity.setCategoryName(row.categoryName());
        entity.setCategoryId(row.categoryId());
        entity.setPaymentMethodName(row.paymentMethodName());
        entity.setPaymentMethodId(row.paymentMethodId());
        entity.setPaymentState(row.paymentState() == null ? null : row.paymentState().name());
        entity.setAppliesDebtPayment(row.appliesDebtPayment());
        entity.setDebtId(row.debtId());
        entity.setDebtLabel(row.debtLabel());
        entity.setDebtPaymentType(row.debtPaymentType() == null ? null : row.debtPaymentType().name());
        entity.setDebtPaymentNotes(row.debtPaymentNotes());
        entity.setValid(row.valid());
        entity.setErrorsJson(row.errors());
        entity.setCreatedExpenseId(row.createdExpenseId());
        entity.setCreatedDebtPaymentId(row.createdDebtPaymentId());
        return entity;
    }

    public ExpenseImportRow toRowDomain(ExpenseImportRowJpaEntity entity) {
        return new ExpenseImportRow(
                entity.getId(),
                entity.getAccountId(),
                entity.getBatchId(),
                entity.getRowNumber(),
                entity.getExpenseDate(),
                entity.getDescription(),
                entity.getAmount() == null ? null : Money.cop(entity.getAmount()),
                entity.getCategoryName(),
                entity.getCategoryId(),
                entity.getPaymentMethodName(),
                entity.getPaymentMethodId(),
                entity.getPaymentState() == null ? null : ExpensePaymentState.valueOf(entity.getPaymentState()),
                entity.isAppliesDebtPayment(),
                entity.getDebtId(),
                entity.getDebtLabel(),
                entity.getDebtPaymentType() == null ? null : DebtPaymentType.valueOf(entity.getDebtPaymentType()),
                entity.getDebtPaymentNotes(),
                entity.isValid(),
                entity.getErrorsJson() == null ? List.of() : entity.getErrorsJson(),
                entity.getCreatedExpenseId(),
                entity.getCreatedDebtPaymentId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
