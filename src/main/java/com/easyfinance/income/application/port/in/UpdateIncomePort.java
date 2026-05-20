package com.easyfinance.income.application.port.in;

import com.easyfinance.income.application.command.UpdateIncomeCommand;
import com.easyfinance.income.application.response.IncomeResponse;

public interface UpdateIncomePort {
    IncomeResponse updateIncome(UpdateIncomeCommand command);
}
