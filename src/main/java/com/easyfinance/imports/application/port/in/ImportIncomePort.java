package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.ImportIncomeCommand;
import com.easyfinance.imports.application.response.IncomeImportResponse;

public interface ImportIncomePort {
    IncomeImportResponse importIncomes(ImportIncomeCommand command);
}

