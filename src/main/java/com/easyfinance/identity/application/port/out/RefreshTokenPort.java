package com.easyfinance.identity.application.port.out;

import com.easyfinance.identity.application.response.IssuedRefreshToken;
import com.easyfinance.identity.application.response.RotatedRefreshToken;

public interface RefreshTokenPort {

    IssuedRefreshToken issue(Long userId);

    RotatedRefreshToken rotate(String rawToken);

    void revoke(String rawToken);
}
