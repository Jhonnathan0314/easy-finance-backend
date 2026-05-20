package com.easyfinance.identity.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void registersActiveUserWithDefaultUserRole() {
        User user = User.register(" USER@Example.COM ", "$2a$hash", " Jane Doe ");

        assertThat(user.id()).isNull();
        assertThat(user.email()).isEqualTo("user@example.com");
        assertThat(user.fullName()).isEqualTo("Jane Doe");
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.globalRoles()).containsExactly(GlobalRoleName.USER);
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> User.register("not-an-email", "$2a$hash", "Jane Doe"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Email is invalid.");
    }

    @Test
    void blockedUserCannotLogin() {
        User user = User.restore(1L, "user@example.com", "$2a$hash", "Jane Doe", UserStatus.BLOCKED, Set.of(GlobalRoleName.USER));

        assertThatThrownBy(user::ensureCanLogin)
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("User cannot login.");
    }
}
