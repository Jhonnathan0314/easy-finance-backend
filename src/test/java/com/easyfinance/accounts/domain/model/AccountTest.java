package com.easyfinance.accounts.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void createsActiveAccount() {
        Account account = Account.create(" Home ", " Monthly finances ");

        assertThat(account.id()).isNull();
        assertThat(account.name()).isEqualTo("Home");
        assertThat(account.description()).isEqualTo("Monthly finances");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Account.create(" ", null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Account name is required.");
    }

    @Test
    void archivesWritableAccount() {
        Account account = Account.restore(1L, "Home", null, AccountStatus.ACTIVE, Instant.now(), Instant.now());

        assertThat(account.archive().status()).isEqualTo(AccountStatus.ARCHIVED);
    }

    @Test
    void archivedAccountIsNotWritable() {
        Account account = Account.restore(1L, "Home", null, AccountStatus.ARCHIVED, Instant.now(), Instant.now());

        assertThatThrownBy(() -> account.update("New", null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Account is not active.");
    }
}
