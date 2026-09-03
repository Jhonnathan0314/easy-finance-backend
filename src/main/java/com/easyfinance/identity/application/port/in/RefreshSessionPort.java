package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.response.AuthSessionResult;

public interface RefreshSessionPort {

    AuthSessionResult refreshSession(String refreshToken);
}
