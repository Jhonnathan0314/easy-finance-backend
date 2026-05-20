package com.easyfinance.identity.application.port.out;

import com.easyfinance.identity.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    boolean existsByEmail(String email);

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);
}

