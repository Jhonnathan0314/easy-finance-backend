package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.imports.application.response.DebtImportResponse;

public interface PreviewDebtImportPort {

    DebtImportResponse previewDebts(ImportDebtCommand command);
}
