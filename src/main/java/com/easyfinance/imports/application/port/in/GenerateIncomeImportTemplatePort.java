package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.IncomeImportTemplateResponse;

public interface GenerateIncomeImportTemplatePort {
    IncomeImportTemplateResponse generate(Long accountId);
}

