package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.ExpenseImportTemplateResponse;

public interface GenerateExpenseImportTemplatePort {

    ExpenseImportTemplateResponse generate(Long accountId);
}
