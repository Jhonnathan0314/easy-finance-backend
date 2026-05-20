package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.query.ListAccountsQuery;
import com.easyfinance.accounts.application.response.AccountResponse;
import com.easyfinance.accounts.application.response.PageResponse;

public interface ListAccountsPort {

    PageResponse<AccountResponse> listMyAccounts(ListAccountsQuery query);
}
