package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.command.UpdateAccountCommand;
import com.easyfinance.accounts.application.response.AccountResponse;

public interface UpdateAccountPort {

    AccountResponse updateAccount(UpdateAccountCommand command);
}
