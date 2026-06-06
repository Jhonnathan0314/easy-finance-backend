package com.easyfinance.accounts.application.service;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AssignedParticipantValidator {

    private final AccountParticipantRepositoryPort accountParticipantRepository;

    public AssignedParticipantValidator(AccountParticipantRepositoryPort accountParticipantRepository) {
        this.accountParticipantRepository = accountParticipantRepository;
    }

    public Long resolveAssignedParticipantId(AccountAccess actorAccess, Long requestedParticipantId) {
        Long actorParticipantId = actorAccess.membership().participantId();
        if (requestedParticipantId == null || requestedParticipantId.equals(actorParticipantId)) {
            return actorParticipantId;
        }
        return validateExplicitParticipantId(actorAccess, requestedParticipantId);
    }

    public Long resolveNullableAssignedParticipantId(AccountAccess actorAccess, Long requestedParticipantId) {
        if (requestedParticipantId == null) {
            return null;
        }
        if (requestedParticipantId.equals(actorAccess.membership().participantId())) {
            return requestedParticipantId;
        }
        return validateExplicitParticipantId(actorAccess, requestedParticipantId);
    }

    private Long validateExplicitParticipantId(AccountAccess actorAccess, Long requestedParticipantId) {
        if (actorAccess.membership().role() != AccountParticipantRole.ACCOUNT_ADMIN) {
            throw new ForbiddenOperationException("ASSIGNED_PARTICIPANT_NOT_ALLOWED", "Only account admins can assign records to another participant.");
        }
        AccountParticipant assignedMembership = accountParticipantRepository
                .findByAccountIdAndParticipantId(actorAccess.account().id(), requestedParticipantId)
                .orElseThrow(() -> new NotFoundException("ASSIGNED_PARTICIPANT_NOT_FOUND", "Assigned participant was not found in this account."));
        if (assignedMembership.status() != AccountParticipantStatus.ACTIVE) {
            throw new ForbiddenOperationException("ASSIGNED_PARTICIPANT_NOT_ACTIVE", "Assigned participant is not active in this account.");
        }
        return assignedMembership.participantId();
    }
}
