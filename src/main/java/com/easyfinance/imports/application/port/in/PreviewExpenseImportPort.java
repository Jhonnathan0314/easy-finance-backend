package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.application.response.ExpenseImportBatchResponse;

public interface PreviewExpenseImportPort {

    ExpenseImportBatchResponse preview(PreviewExpenseImportCommand command);
}
