package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.command.RegisterUserCommand;
import com.easyfinance.identity.application.response.AuthSessionResult;

public interface RegisterUserPort {

    AuthSessionResult register(RegisterUserCommand command);
}
