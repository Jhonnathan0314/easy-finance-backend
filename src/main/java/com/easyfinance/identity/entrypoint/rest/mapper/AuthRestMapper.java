package com.easyfinance.identity.entrypoint.rest.mapper;

import com.easyfinance.identity.application.command.LoginCommand;
import com.easyfinance.identity.application.command.RegisterUserCommand;
import com.easyfinance.identity.application.command.UpdateProfileCommand;
import com.easyfinance.identity.application.response.AuthTokenResponse;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;
import com.easyfinance.identity.entrypoint.rest.dto.AuthTokenResponseDto;
import com.easyfinance.identity.entrypoint.rest.dto.AuthenticatedUserDto;
import com.easyfinance.identity.entrypoint.rest.dto.LoginRequest;
import com.easyfinance.identity.entrypoint.rest.dto.RegisterRequest;
import com.easyfinance.identity.entrypoint.rest.dto.UpdateProfileRequest;

public final class AuthRestMapper {

    private AuthRestMapper() {
    }

    public static RegisterUserCommand toCommand(RegisterRequest request) {
        return new RegisterUserCommand(request.email(), request.password(), request.fullName());
    }

    public static LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.email(), request.password());
    }

    public static UpdateProfileCommand toCommand(UpdateProfileRequest request) {
        return new UpdateProfileCommand(request.fullName());
    }

    public static AuthTokenResponseDto toDto(AuthTokenResponse response) {
        return new AuthTokenResponseDto(
                response.accessToken(),
                response.tokenType(),
                response.expiresIn(),
                toDto(response.user())
        );
    }

    public static AuthenticatedUserDto toDto(AuthenticatedUserResponse response) {
        return new AuthenticatedUserDto(
                response.userId(),
                response.participantId(),
                response.email(),
                response.fullName(),
                response.globalRoles()
        );
    }
}
