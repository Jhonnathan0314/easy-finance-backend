package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.domain.model.ExpenseImportBatch;
import com.easyfinance.imports.domain.model.ExpenseImportRow;

import java.util.List;
import java.util.Optional;

public interface ExpenseImportRepositoryPort {

    ExpenseImportBatch savePreview(ExpenseImportBatch batch);

    Optional<ExpenseImportBatch> findByAccountIdAndId(Long accountId, Long batchId);

    Optional<ExpenseImportBatch> findByAccountIdAndIdForUpdate(Long accountId, Long batchId);

    ExpenseImportBatch saveBatch(ExpenseImportBatch batch);

    void updateCreatedExpenseId(Long accountId, Long rowId, Long expenseId);

    void updateCreatedDebtPaymentId(Long accountId, Long rowId, Long debtPaymentId);

    List<ExpenseImportRow> findRowsByBatch(Long accountId, Long batchId);
}
