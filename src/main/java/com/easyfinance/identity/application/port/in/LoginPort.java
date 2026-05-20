package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.command.LoginCommand;
import com.easyfinance.identity.application.response.AuthTokenResponse;

public interface LoginPort {

    AuthTokenResponse login(LoginCommand command);
}

