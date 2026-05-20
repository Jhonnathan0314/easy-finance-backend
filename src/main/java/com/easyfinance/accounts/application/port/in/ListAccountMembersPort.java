package com.easyfinance.accounts.application.port.in;

import com.easyfinance.accounts.application.response.AccountMemberResponse;

import java.util.List;

public interface ListAccountMembersPort {

    List<AccountMemberResponse> listMembers(Long accountId);
}
