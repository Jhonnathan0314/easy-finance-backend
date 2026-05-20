package com.easyfinance.identity.infrastructure.security;

import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.infrastructure.security.AuthenticatedUserStatusValidator;
import com.easyfinance.shared.infrastructure.security.JwtAccessDeniedException;
import com.easyfinance.shared.infrastructure.security.JwtAuthenticationException;
import org.springframework.stereotype.Component;

@Component
class IdentityAuthenticatedUserStatusValidator implements AuthenticatedUserStatusValidator {

    private final UserRepositoryPort userRepository;
    private final ParticipantRepositoryPort participantRepository;

    IdentityAuthenticatedUserStatusValidator(
            UserRepositoryPort userRepository,
            ParticipantRepositoryPort participantRepository
    ) {
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public void validate(CurrentUser currentUser) {
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> invalidToken());
        Participant participant = participantRepository.findByUserId(user.id())
                .filter(value -> value.id().equals(currentUser.participantId()))
                .orElseThrow(() -> invalidToken());

        if (user.status() == UserStatus.BLOCKED) {
            throw new JwtAccessDeniedException("USER_BLOCKED", "User is blocked.");
        }
        if (user.status() == UserStatus.INACTIVE) {
            throw new JwtAccessDeniedException("USER_NOT_ACTIVE", "User is not active.");
        }
        if (participant.status() == ParticipantStatus.INACTIVE) {
            throw new JwtAccessDeniedException("PARTICIPANT_NOT_ACTIVE", "Participant is not active.");
        }
    }

    private static JwtAuthenticationException invalidToken() {
        return new JwtAuthenticationException("INVALID_TOKEN", "Invalid token.");
    }
}
