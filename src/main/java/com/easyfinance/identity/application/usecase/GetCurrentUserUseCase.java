package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.port.in.GetCurrentUserPort;
import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.ParticipantStatus;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GetCurrentUserUseCase implements GetCurrentUserPort {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepositoryPort userRepository;
    private final ParticipantRepositoryPort participantRepository;

    public GetCurrentUserUseCase(
            CurrentUserProvider currentUserProvider,
            UserRepositoryPort userRepository,
            ParticipantRepositoryPort participantRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUserResponse getCurrentUser() {
        CurrentUser currentUser = currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));

        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User was not found."));
        Participant participant = participantRepository.findByUserId(user.id())
                .orElseThrow(() -> new NotFoundException("PARTICIPANT_NOT_FOUND", "Participant was not found."));

        ensureCurrentIdentityIsActive(user, participant);

        Set<String> roles = user.globalRoles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUserResponse(user.id(), participant.id(), user.email(), user.fullName(), roles);
    }

    private void ensureCurrentIdentityIsActive(User user, Participant participant) {
        if (user.status() == UserStatus.BLOCKED) {
            throw new ForbiddenOperationException("USER_BLOCKED", "User is blocked.");
        }
        if (user.status() == UserStatus.INACTIVE) {
            throw new ForbiddenOperationException("USER_NOT_ACTIVE", "User is not active.");
        }
        if (participant.status() == ParticipantStatus.INACTIVE) {
            throw new ForbiddenOperationException("PARTICIPANT_NOT_ACTIVE", "Participant is not active.");
        }
    }
}
