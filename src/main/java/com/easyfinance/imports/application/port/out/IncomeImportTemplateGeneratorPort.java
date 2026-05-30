package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.template.IncomeImportTemplateData;

public interface IncomeImportTemplateGeneratorPort {
    byte[] generate(IncomeImportTemplateData data);
}

