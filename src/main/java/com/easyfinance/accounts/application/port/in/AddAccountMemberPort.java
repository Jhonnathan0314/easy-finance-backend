package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.command.AddAccountMemberCommand;
import com.easyfinance.accounts.application.response.AccountMemberResponse;

public interface AddAccountMemberPort {

    AccountMemberResponse addMember(AddAccountMemberCommand command);
}
