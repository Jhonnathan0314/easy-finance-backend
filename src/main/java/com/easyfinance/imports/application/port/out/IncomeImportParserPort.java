package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.command.ImportIncomeCommand;
import com.easyfinance.imports.application.validation.IncomeImportParsedRow;

import java.util.List;

public interface IncomeImportParserPort {
    List<IncomeImportParsedRow> parse(ImportIncomeCommand command, Long accountId);
}

