package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.template.DebtImportTemplateData;

public interface DebtImportTemplateGeneratorPort {
    byte[] generate(DebtImportTemplateData data);
}
