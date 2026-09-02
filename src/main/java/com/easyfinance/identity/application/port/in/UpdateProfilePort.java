package com.easyfinance.identity.application.port.in;

import com.easyfinance.identity.application.command.UpdateProfileCommand;
import com.easyfinance.identity.application.response.AuthenticatedUserResponse;

public interface UpdateProfilePort {

    AuthenticatedUserResponse updateProfile(UpdateProfileCommand command);
}
