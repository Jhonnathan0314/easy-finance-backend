package com.easyfinance.accounts.application.command;

import com.easyfinance.accounts.domain.model.AccountParticipantRole;

public record ChangeAccountMemberRoleCommand(Long accountId, Long participantId, AccountParticipantRole role) {
}
