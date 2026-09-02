package com.easyfinance.identity.entrypoint.rest;

import com.easyfinance.identity.application.port.in.GetCurrentUserPort;
import com.easyfinance.identity.application.port.in.LoginPort;
import com.easyfinance.identity.application.port.in.RegisterUserPort;
import com.easyfinance.identity.application.port.in.UpdateProfilePort;
import com.easyfinance.identity.entrypoint.rest.dto.AuthTokenResponseDto;
import com.easyfinance.identity.entrypoint.rest.dto.AuthenticatedUserDto;
import com.easyfinance.identity.entrypoint.rest.dto.LoginRequest;
import com.easyfinance.identity.entrypoint.rest.dto.RegisterRequest;
import com.easyfinance.identity.entrypoint.rest.dto.UpdateProfileRequest;
import com.easyfinance.identity.entrypoint.rest.mapper.AuthRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserPort registerUserPort;
    private final LoginPort loginPort;
    private final GetCurrentUserPort getCurrentUserPort;
    private final UpdateProfilePort updateProfilePort;

    public AuthController(
            RegisterUserPort registerUserPort,
            LoginPort loginPort,
            GetCurrentUserPort getCurrentUserPort,
            UpdateProfilePort updateProfilePort
    ) {
        this.registerUserPort = registerUserPort;
        this.loginPort = loginPort;
        this.getCurrentUserPort = getCurrentUserPort;
        this.updateProfilePort = updateProfilePort;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthTokenResponseDto register(@Valid @RequestBody RegisterRequest request) {
        return AuthRestMapper.toDto(registerUserPort.register(AuthRestMapper.toCommand(request)));
    }

    @PostMapping("/login")
    public AuthTokenResponseDto login(@Valid @RequestBody LoginRequest request) {
        return AuthRestMapper.toDto(loginPort.login(AuthRestMapper.toCommand(request)));
    }

    @GetMapping("/me")
    public AuthenticatedUserDto me() {
        return AuthRestMapper.toDto(getCurrentUserPort.getCurrentUser());
    }

    @PutMapping("/me")
    public AuthenticatedUserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return AuthRestMapper.toDto(updateProfilePort.updateProfile(AuthRestMapper.toCommand(request)));
    }
}
