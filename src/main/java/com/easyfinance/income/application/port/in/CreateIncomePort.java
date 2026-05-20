package com.easyfinance.income.application.port.in;

import com.easyfinance.income.application.command.CreateIncomeCommand;
import com.easyfinance.income.application.response.IncomeResponse;

public interface CreateIncomePort {
    IncomeResponse createIncome(CreateIncomeCommand command);
}
