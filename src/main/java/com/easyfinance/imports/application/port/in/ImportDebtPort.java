package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.imports.application.response.DebtImportResponse;

public interface ImportDebtPort {
    DebtImportResponse importDebts(ImportDebtCommand command);
}
