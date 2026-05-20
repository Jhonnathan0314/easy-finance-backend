package com.easyfinance.accounts.application.port.out;

import com.easyfinance.accounts.application.response.ParticipantInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ParticipantLookupPort {

    Optional<ParticipantInfo> findByParticipantId(Long participantId);

    Optional<ParticipantInfo> findActiveByEmail(String email);

    Map<Long, ParticipantInfo> findByParticipantIds(Collection<Long> participantIds);
}
