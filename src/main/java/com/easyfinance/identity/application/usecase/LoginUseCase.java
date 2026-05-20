package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.command.LoginCommand;
import com.easyfinance.identity.application.port.in.LoginPort;
import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.PasswordHasherPort;
import com.easyfinance.identity.application.port.out.TokenIssuerPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.application.response.AuthTokenResponse;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LoginUseCase implements LoginPort {

    private final UserRepositoryPort userRepository;
    private final ParticipantRepositoryPort participantRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    public LoginUseCase(
            UserRepositoryPort userRepository,
            ParticipantRepositoryPort participantRepository,
            PasswordHasherPort passwordHasher,
            TokenIssuerPort tokenIssuer
    ) {
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginCommand command) {
        String email = command.email() == null ? "" : command.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(LoginUseCase::invalidCredentials);

        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw invalidCredentials();
        }

        try {
            user.ensureCanLogin();
        } catch (BusinessRuleViolationException ex) {
            throw new UnauthorizedOperationException("USER_NOT_ALLOWED_TO_LOGIN", "User cannot login.", ex);
        }

        Participant participant = participantRepository.findByUserId(user.id())
                .orElseThrow(() -> new NotFoundException("PARTICIPANT_NOT_FOUND", "Participant was not found."));

        AuthenticatedUserResponse response = toResponse(user, participant.id());
        return new AuthTokenResponse(tokenIssuer.issueToken(response), "Bearer", tokenIssuer.expiresInSeconds(), response);
    }

    private static UnauthorizedOperationException invalidCredentials() {
        return new UnauthorizedOperationException("INVALID_CREDENTIALS", "Invalid email or password.");
    }

    private AuthenticatedUserResponse toResponse(User user, Long participantId) {
        Set<String> roles = user.globalRoles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUserResponse(user.id(), participantId, user.email(), user.fullName(), roles);
    }
}
