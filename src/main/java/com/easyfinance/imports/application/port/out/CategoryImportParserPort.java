package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.imports.application.validation.CategoryImportParsedRow;

import java.util.List;

public interface CategoryImportParserPort {

    List<CategoryImportParsedRow> parse(ImportCategoryCommand command);
}

