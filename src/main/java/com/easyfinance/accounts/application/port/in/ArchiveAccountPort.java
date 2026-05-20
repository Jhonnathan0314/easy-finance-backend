package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.response.AccountResponse;

public interface ArchiveAccountPort {

    AccountResponse archiveAccount(Long accountId);
}
