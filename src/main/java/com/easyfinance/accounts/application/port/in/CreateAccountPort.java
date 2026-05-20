package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.command.CreateAccountCommand;
import com.easyfinance.accounts.application.response.AccountResponse;

public interface CreateAccountPort {

    AccountResponse createAccount(CreateAccountCommand command);
}
