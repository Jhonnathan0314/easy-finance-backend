package com.easyfinance.identity.infrastructure.mapper;

import com.easyfinance.identity.domain.model.GlobalRoleName;
import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.domain.model.UserStatus;
import com.easyfinance.identity.infrastructure.persistence.jpa.GlobalRoleJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.GlobalRoleNameJpa;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserStatusJpa;

import java.util.Set;
import java.util.stream.Collectors;

public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        Set<GlobalRoleName> roles = entity.getGlobalRoles()
                .stream()
                .map(role -> GlobalRoleName.valueOf(role.getName().name()))
                .collect(Collectors.toUnmodifiableSet());

        return User.restore(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFullName(),
                UserStatus.valueOf(entity.getStatus().name()),
                roles
        );
    }

    public UserJpaEntity toEntity(User user, Set<GlobalRoleJpaEntity> roles) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setFullName(user.fullName());
        entity.setStatus(UserStatusJpa.valueOf(user.status().name()));
        entity.setGlobalRoles(roles);
        return entity;
    }

    public GlobalRoleNameJpa toJpaRole(GlobalRoleName roleName) {
        return GlobalRoleNameJpa.valueOf(roleName.name());
    }
}

