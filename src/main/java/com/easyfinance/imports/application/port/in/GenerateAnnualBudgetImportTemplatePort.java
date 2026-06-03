package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.AnnualBudgetImportTemplateResponse;

public interface GenerateAnnualBudgetImportTemplatePort {
    AnnualBudgetImportTemplateResponse generate(Long accountId);
}

