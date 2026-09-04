package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.DebtImportTemplateResponse;

public interface GenerateDebtImportTemplatePort {
    DebtImportTemplateResponse generate(Long accountId);
}
