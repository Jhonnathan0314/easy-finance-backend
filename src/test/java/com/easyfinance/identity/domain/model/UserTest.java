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

    @Test
    void renameUpdatesFullNameAndKeepsOtherFields() {
        User user = User.restore(1L, "user@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER));

        User renamed = user.rename(" Jane Smith ");

        assertThat(renamed.id()).isEqualTo(1L);
        assertThat(renamed.email()).isEqualTo("user@example.com");
        assertThat(renamed.passwordHash()).isEqualTo("$2a$hash");
        assertThat(renamed.fullName()).isEqualTo("Jane Smith");
        assertThat(renamed.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(renamed.globalRoles()).containsExactly(GlobalRoleName.USER);
    }

    @Test
    void renameRejectsBlankFullName() {
        User user = User.restore(1L, "user@example.com", "$2a$hash", "Jane Doe", UserStatus.ACTIVE, Set.of(GlobalRoleName.USER));

        assertThatThrownBy(() -> user.rename("   "))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("FULL_NAME_REQUIRED"));
    }
}
