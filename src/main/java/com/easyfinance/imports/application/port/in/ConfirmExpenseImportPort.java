package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.ExpenseImportBatchResponse;

public interface ConfirmExpenseImportPort {

    ExpenseImportBatchResponse confirm(Long accountId, Long batchId);
}
