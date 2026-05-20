package com.easyfinance.imports.application.port.out;

import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.domain.model.ExpenseImportRow;

import java.util.List;

public interface ExpenseImportParserPort {

    List<ExpenseImportRow> parse(PreviewExpenseImportCommand command, Long accountId);
}
