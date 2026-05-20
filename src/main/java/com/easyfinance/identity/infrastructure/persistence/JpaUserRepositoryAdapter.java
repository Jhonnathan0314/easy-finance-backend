package com.easyfinance.identity.infrastructure.persistence;

import com.easyfinance.identity.application.port.out.UserRepositoryPort;
import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.infrastructure.mapper.UserPersistenceMapper;
import com.easyfinance.identity.infrastructure.persistence.jpa.GlobalRoleJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataGlobalRoleRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserJpaEntity;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository userRepository;
    private final SpringDataGlobalRoleRepository roleRepository;
    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    public JpaUserRepositoryAdapter(SpringDataUserRepository userRepository, SpringDataGlobalRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && userRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        try {
            Set<GlobalRoleJpaEntity> roles = user.globalRoles()
                    .stream()
                    .map(this::findRole)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            UserJpaEntity saved = userRepository.saveAndFlush(mapper.toEntity(user, roles));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isEmailUniqueConstraintViolation(ex)) {
                throw new BusinessRuleViolationException("EMAIL_ALREADY_REGISTERED", "Email is already registered.", ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(mapper::toDomain);
    }

    private GlobalRoleJpaEntity findRole(GlobalRoleName roleName) {
        return roleRepository.findByName(mapper.toJpaRole(roleName))
                .orElseThrow(() -> new NotFoundException("GLOBAL_ROLE_NOT_FOUND", "Global role was not found."));
    }

    private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uq_users_email")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
