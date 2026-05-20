package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.command.ChangeAccountMemberRoleCommand;
import com.easyfinance.accounts.application.response.AccountMemberResponse;

public interface ChangeAccountMemberRolePort {

    AccountMemberResponse changeMemberRole(ChangeAccountMemberRoleCommand command);
}
