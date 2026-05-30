package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.response.CategoryImportTemplateResponse;

public interface GenerateCategoryImportTemplatePort {

    CategoryImportTemplateResponse generate(Long accountId);
}

