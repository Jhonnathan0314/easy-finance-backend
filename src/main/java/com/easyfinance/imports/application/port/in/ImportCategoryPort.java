package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.imports.application.response.CategoryImportResponse;

public interface ImportCategoryPort {

    CategoryImportResponse importCategories(ImportCategoryCommand command);
}

