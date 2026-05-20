package com.easyfinance.accounts.application.port.in;

public interface RemoveAccountMemberPort {

    void removeMember(Long accountId, Long participantId);
}
