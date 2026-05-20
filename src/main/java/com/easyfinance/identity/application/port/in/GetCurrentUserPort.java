package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.response.AuthenticatedUserResponse;

public interface GetCurrentUserPort {

    AuthenticatedUserResponse getCurrentUser();
}

