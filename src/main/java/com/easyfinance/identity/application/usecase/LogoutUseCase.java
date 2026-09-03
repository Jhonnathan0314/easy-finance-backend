package com.easyfinance.identity.application.usecase;

import com.easyfinance.identity.application.port.in.LogoutPort;
import com.easyfinance.identity.application.port.out.RefreshTokenPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase implements LogoutPort {

    private final RefreshTokenPort refreshTokenPort;

    public LogoutUseCase(RefreshTokenPort refreshTokenPort) {
        this.refreshTokenPort = refreshTokenPort;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenPort.revoke(refreshToken);
    }
}
