package com.easyfinance.shared.application;

import java.util.Optional;

public interface CurrentUserProvider {

    Optional<CurrentUser> currentUser();

    default Optional<Long> currentUserId() {
        return currentUser().map(CurrentUser::userId);
    }
}
