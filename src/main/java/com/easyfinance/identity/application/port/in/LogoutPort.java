package com.easyfinance.identity.application.port.in;

public interface LogoutPort {

    void logout(String refreshToken);
}
