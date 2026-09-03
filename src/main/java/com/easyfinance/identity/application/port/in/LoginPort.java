package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.command.LoginCommand;
import com.easyfinance.identity.application.response.AuthSessionResult;

public interface LoginPort {

    AuthSessionResult login(LoginCommand command);
}
