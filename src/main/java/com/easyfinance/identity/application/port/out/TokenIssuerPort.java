package com.easyfinance.identity.application.port.out;

import com.easyfinance.identity.application.response.AuthenticatedUserResponse;

public interface TokenIssuerPort {

    String issueToken(AuthenticatedUserResponse user);

    long expiresInSeconds();
}

