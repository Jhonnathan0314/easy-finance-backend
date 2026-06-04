package com.easyfinance.imports.application.port.in;

import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.response.AnnualBudgetImportResponse;

public interface PreviewAnnualBudgetImportPort {

    AnnualBudgetImportResponse previewAnnualBudget(ImportAnnualBudgetCommand command);
}
