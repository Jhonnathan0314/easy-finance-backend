package com.easyfinance.accounts.application.port.out;

import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.application.response.PageResponse;
import com.easyfinance.shared.application.PageQuery;

import java.util.List;
import java.util.Optional;

public interface AccountParticipantRepositoryPort {

    AccountParticipant save(AccountParticipant accountParticipant);

    Optional<AccountParticipant> findByAccountIdAndParticipantId(Long accountId, Long participantId);

    List<AccountParticipant> findByAccountId(Long accountId);

    void lockByAccountId(Long accountId);

    long countByAccountIdAndRoleAndStatus(Long accountId, AccountParticipantRole role, AccountParticipantStatus status);

    PageResponse<AccountParticipant> findMembershipsForParticipant(Long participantId, PageQuery pageQuery);
}
