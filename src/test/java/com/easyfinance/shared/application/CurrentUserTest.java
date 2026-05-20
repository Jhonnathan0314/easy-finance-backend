package com.easyfinance.shared.application;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    @Test
    void copiesRolesDefensively() {
        Set<String> roles = new HashSet<>();
        roles.add("USER");

        CurrentUser currentUser = new CurrentUser(1L, 10L, "jane@example.com", roles, true);
        roles.add("SUPER_ADMIN");

        assertThat(currentUser.globalRoles()).containsExactly("USER");
        assertThatThrownBy(() -> currentUser.globalRoles().add("OTHER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void convertsNullRolesToEmptySet() {
        CurrentUser currentUser = new CurrentUser(1L, 10L, "jane@example.com", null, true);

        assertThat(currentUser.globalRoles()).isEmpty();
    }
}
