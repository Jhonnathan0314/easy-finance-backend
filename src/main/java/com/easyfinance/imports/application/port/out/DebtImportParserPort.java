package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.imports.application.validation.DebtImportParsedRow;

import java.util.List;

public interface DebtImportParserPort {
    List<DebtImportParsedRow> parse(ImportDebtCommand command, Long accountId);
}
