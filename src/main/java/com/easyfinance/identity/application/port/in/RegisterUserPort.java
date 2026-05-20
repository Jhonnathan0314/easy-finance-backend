package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.command.RegisterUserCommand;
import com.easyfinance.identity.application.response.AuthTokenResponse;

public interface RegisterUserPort {

    AuthTokenResponse register(RegisterUserCommand command);
}

