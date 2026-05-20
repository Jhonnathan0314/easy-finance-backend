package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.template.ExpenseImportTemplateData;

public interface ExpenseImportTemplateGeneratorPort {

    byte[] generate(ExpenseImportTemplateData data);
}
