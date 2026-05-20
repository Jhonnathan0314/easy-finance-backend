package com.easyfinance.accounts.application.command;

import com.easyfinance.accounts.domain.model.AccountParticipantRole;

public record AddAccountMemberCommand(Long accountId, String email, AccountParticipantRole role) {
}
