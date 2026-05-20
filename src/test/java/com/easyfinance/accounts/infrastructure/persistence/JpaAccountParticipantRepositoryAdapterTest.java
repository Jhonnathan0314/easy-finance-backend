package com.easyfinance.accounts.infrastructure.persistence;

import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.AccountParticipantJpaEntity;
import com.easyfinance.accounts.infrastructure.persistence.jpa.SpringDataAccountParticipantRepository;
import com.easyfinance.accounts.infrastructure.persistence.jpa.SpringDataAccountRepository;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaAccountParticipantRepositoryAdapterTest {

    private final SpringDataAccountParticipantRepository participantRepository = mock(SpringDataAccountParticipantRepository.class);
    private final SpringDataAccountRepository accountRepository = mock(SpringDataAccountRepository.class);
    private final JpaAccountParticipantRepositoryAdapter adapter = new JpaAccountParticipantRepositoryAdapter(participantRepository, accountRepository);

    @Test
    void translatesUniqueAccountParticipantConstraint() {
        givenAccount();
        when(participantRepository.saveAndFlush(any(AccountParticipantJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates constraint uq_account_participants_account_participant"));

        assertThatThrownBy(() -> adapter.save(AccountParticipant.create(1L, 20L, AccountParticipantRole.ACCOUNT_MEMBER)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("ACCOUNT_MEMBER_ALREADY_EXISTS"));
    }

    @Test
    void doesNotTranslateUnknownIntegrityErrors() {
        givenAccount();
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");
        when(participantRepository.saveAndFlush(any(AccountParticipantJpaEntity.class))).thenThrow(exception);

        assertThatThrownBy(() -> adapter.save(AccountParticipant.create(1L, 20L, AccountParticipantRole.ACCOUNT_MEMBER)))
                .isSameAs(exception);
    }

    private void givenAccount() {
        AccountJpaEntity account = new AccountJpaEntity();
        account.setId(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    }
}
