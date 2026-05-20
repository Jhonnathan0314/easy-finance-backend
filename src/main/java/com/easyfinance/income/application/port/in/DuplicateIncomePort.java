package com.easyfinance.income.application.port.in;

import com.easyfinance.income.application.command.DuplicateIncomeCommand;
import com.easyfinance.income.application.response.IncomeResponse;

public interface DuplicateIncomePort {
    IncomeResponse duplicateIncome(DuplicateIncomeCommand command);
}
