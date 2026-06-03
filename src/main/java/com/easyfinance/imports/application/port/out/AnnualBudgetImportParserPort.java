package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.validation.AnnualBudgetImportParsedRow;

import java.util.List;

public interface AnnualBudgetImportParserPort {
    List<AnnualBudgetImportParsedRow> parse(ImportAnnualBudgetCommand command);
}

