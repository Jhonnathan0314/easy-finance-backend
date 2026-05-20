package com.easyfinance.identity.infrastructure.persistence;

import com.easyfinance.identity.domain.model.User;
import com.easyfinance.identity.infrastructure.persistence.jpa.GlobalRoleJpaEntity;
import com.easyfinance.identity.infrastructure.persistence.jpa.GlobalRoleNameJpa;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataGlobalRoleRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.SpringDataUserRepository;
import com.easyfinance.identity.infrastructure.persistence.jpa.UserJpaEntity;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaUserRepositoryAdapterTest {

    private final SpringDataUserRepository userRepository = mock(SpringDataUserRepository.class);
    private final SpringDataGlobalRoleRepository roleRepository = mock(SpringDataGlobalRoleRepository.class);
    private final JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(userRepository, roleRepository);

    @Test
    void mapsEmailUniqueConstraintViolationToBusinessError() {
        GlobalRoleJpaEntity role = new GlobalRoleJpaEntity();
        role.setId(1L);
        role.setName(GlobalRoleNameJpa.USER);
        when(roleRepository.findByName(GlobalRoleNameJpa.USER)).thenReturn(Optional.of(role));
        when(userRepository.saveAndFlush(any(UserJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_users_email\""));

        assertThatThrownBy(() -> adapter.save(User.register("jane@example.com", "$2a$hash", "Jane Doe")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("EMAIL_ALREADY_REGISTERED"));
    }
}
