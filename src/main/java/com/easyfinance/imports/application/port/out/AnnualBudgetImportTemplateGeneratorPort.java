package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.template.AnnualBudgetImportTemplateData;

public interface AnnualBudgetImportTemplateGeneratorPort {
    byte[] generate(AnnualBudgetImportTemplateData data);
}

