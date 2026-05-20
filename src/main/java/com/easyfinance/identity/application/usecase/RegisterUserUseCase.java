package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.command.RegisterUserCommand;
import com.easyfinance.identity.application.port.in.RegisterUserPort;
import com.easyfinance.identity.application.port.out.ParticipantRepositoryPort;
import com.easyfinance.identity.application.port.out.PasswordHasherPort;
import com.easyfinance.identity.application.port.out.TokenIssuerPort;
import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.application.response.AuthTokenResponse;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.identity.domain.model.Participant;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RegisterUserUseCase implements RegisterUserPort {

    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    private final UserRepositoryPort userRepository;
    private final ParticipantRepositoryPort participantRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;

    public RegisterUserUseCase(
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
    @Transactional
    public AuthTokenResponse register(RegisterUserCommand command) {
        validatePassword(command.password());
        String normalizedEmail = command.email() == null ? null : command.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleViolationException("EMAIL_ALREADY_REGISTERED", "Email is already registered.");
        }

        User savedUser = userRepository.save(User.register(normalizedEmail, passwordHasher.hash(command.password()), command.fullName()));
        Participant savedParticipant = participantRepository.save(Participant.createForUser(savedUser.id(), savedUser.fullName()));
        AuthenticatedUserResponse user = toResponse(savedUser, savedParticipant.id());

        return new AuthTokenResponse(tokenIssuer.issueToken(user), "Bearer", tokenIssuer.expiresInSeconds(), user);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || !LETTER.matcher(password).matches() || !DIGIT.matcher(password).matches()) {
            throw new BusinessRuleViolationException(
                    "PASSWORD_POLICY_VIOLATION",
                    "Password must have at least 8 characters, one letter, and one number."
            );
        }
    }

    private AuthenticatedUserResponse toResponse(User user, Long participantId) {
        Set<String> roles = user.globalRoles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUserResponse(user.id(), participantId, user.email(), user.fullName(), roles);
    }
}

