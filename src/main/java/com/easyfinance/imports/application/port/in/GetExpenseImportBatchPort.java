package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.ExpenseImportBatchResponse;

public interface GetExpenseImportBatchPort {

    ExpenseImportBatchResponse get(Long accountId, Long batchId);
}
