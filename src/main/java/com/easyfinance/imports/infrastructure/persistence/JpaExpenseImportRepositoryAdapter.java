package com.easyfinance.imports.infrastructure.persistence;

import com.easyfinance.imports.application.port.out.ExpenseImportRepositoryPort;
import com.easyfinance.imports.domain.model.ExpenseImportBatch;
import com.easyfinance.imports.domain.model.ExpenseImportRow;
import com.easyfinance.imports.infrastructure.mapper.ExpenseImportPersistenceMapper;
import com.easyfinance.imports.infrastructure.persistence.jpa.ExpenseImportBatchJpaEntity;
import com.easyfinance.imports.infrastructure.persistence.jpa.SpringDataExpenseImportBatchRepository;
import com.easyfinance.imports.infrastructure.persistence.jpa.SpringDataExpenseImportRowRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaExpenseImportRepositoryAdapter implements ExpenseImportRepositoryPort {

    private final SpringDataExpenseImportBatchRepository batchRepository;
    private final SpringDataExpenseImportRowRepository rowRepository;
    private final ExpenseImportPersistenceMapper mapper = new ExpenseImportPersistenceMapper();

    public JpaExpenseImportRepositoryAdapter(
            SpringDataExpenseImportBatchRepository batchRepository,
            SpringDataExpenseImportRowRepository rowRepository
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
    }

    @Override
    public ExpenseImportBatch savePreview(ExpenseImportBatch batch) {
        ExpenseImportBatchJpaEntity savedBatch = batchRepository.saveAndFlush(mapper.toBatchEntity(batch));
        rowRepository.saveAllAndFlush(batch.rows().stream()
                .map(row -> mapper.toRowEntity(row, savedBatch.getId()))
                .toList());
        return findByAccountIdAndId(savedBatch.getAccountId(), savedBatch.getId()).orElseThrow();
    }

    @Override
    public Optional<ExpenseImportBatch> findByAccountIdAndId(Long accountId, Long batchId) {
        return batchRepository.findByAccountIdAndId(accountId, batchId)
                .map(batch -> mapper.toBatchDomain(batch, findRowsByBatch(accountId, batchId)));
    }

    @Override
    public Optional<ExpenseImportBatch> findByAccountIdAndIdForUpdate(Long accountId, Long batchId) {
        return batchRepository.findByAccountIdAndIdForUpdate(accountId, batchId)
                .map(batch -> mapper.toBatchDomain(batch, findRowsByBatch(accountId, batchId)));
    }

    @Override
    public ExpenseImportBatch saveBatch(ExpenseImportBatch batch) {
        ExpenseImportBatchJpaEntity entity = batchRepository.findByAccountIdAndId(batch.accountId(), batch.id()).orElseThrow();
        mapper.copyToBatchEntity(batch, entity);
        ExpenseImportBatchJpaEntity saved = batchRepository.saveAndFlush(entity);
        return mapper.toBatchDomain(saved, findRowsByBatch(saved.getAccountId(), saved.getId()));
    }

    @Override
    public void updateCreatedExpenseId(Long accountId, Long rowId, Long expenseId) {
        var row = rowRepository.findByAccountIdAndId(accountId, rowId).orElseThrow();
        row.setCreatedExpenseId(expenseId);
        rowRepository.saveAndFlush(row);
    }

    @Override
    public void updateCreatedDebtPaymentId(Long accountId, Long rowId, Long debtPaymentId) {
        var row = rowRepository.findByAccountIdAndId(accountId, rowId).orElseThrow();
        row.setCreatedDebtPaymentId(debtPaymentId);
        rowRepository.saveAndFlush(row);
    }

    @Override
    public List<ExpenseImportRow> findRowsByBatch(Long accountId, Long batchId) {
        return rowRepository.findByAccountIdAndBatchIdOrderByRowNumberAsc(accountId, batchId)
                .stream()
                .map(mapper::toRowDomain)
                .toList();
    }
}
