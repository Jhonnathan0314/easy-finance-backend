package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.Account;
import com.easyfinance.accounts.domain.model.AccountParticipant;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.accounts.domain.model.AccountStatus;
import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.port.in.CreateManualDebtPort;
import com.easyfinance.debts.application.response.DebtResponse;
import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.imports.application.port.out.DebtImportParserPort;
import com.easyfinance.imports.application.port.out.DebtImportTemplateGeneratorPort;
import com.easyfinance.imports.application.validation.DebtImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebtImportUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final AssignedParticipantValidator assignedParticipantValidator = mock(AssignedParticipantValidator.class);
    private final AccountParticipantRepositoryPort accountParticipantRepository = mock(AccountParticipantRepositoryPort.class);
    private final ParticipantLookupPort participantLookupPort = mock(ParticipantLookupPort.class);
    private final DebtImportParserPort parserPort = mock(DebtImportParserPort.class);
    private final DebtImportTemplateGeneratorPort templateGeneratorPort = mock(DebtImportTemplateGeneratorPort.class);
    private final CreateManualDebtPort createManualDebtPort = mock(CreateManualDebtPort.class);
    private final DebtImportUseCase useCase = new DebtImportUseCase(
            currentUserProvider,
            accountAuthorizationService,
            assignedParticipantValidator,
            accountParticipantRepository,
            participantLookupPort,
            parserPort,
            templateGeneratorPort,
            createManualDebtPort,
            5_242_880
    );

    @Test
    void importCreatesAllWhenRowsAreValid() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new DebtImportParsedRow(2, "Prestamo carro", null, new BigDecimal("5000000"), null, null, null, LocalDate.of(2026, 5, 1), null, null, null, List.of()),
                new DebtImportParsedRow(3, "Prestamo casa", "Hipoteca", new BigDecimal("100000000"), new BigDecimal("60000000"), 120, new BigDecimal("1200000"), LocalDate.of(2020, 1, 1), null, "Ana Gomez <ana@example.com>", "Migrada", List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(null))).thenReturn(10L);
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L))).thenReturn(20L);
        when(createManualDebtPort.createManualDebt(any()))
                .thenReturn(debtResponse(101L, 10L))
                .thenReturn(debtResponse(102L, 20L));

        var response = useCase.importDebts(command("debts.xlsx"));

        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.rows()).allMatch(row -> row.valid() && row.createdDebtId() != null);
        assertThat(response.rows().getFirst().participantId()).isEqualTo(10L);
        assertThat(response.rows().get(1).participantId()).isEqualTo(20L);
        verify(createManualDebtPort, times(2)).createManualDebt(any());
    }

    @Test
    void importDoesNotCreateAnyWhenOneRowIsInvalid() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new DebtImportParsedRow(2, "Prestamo carro", null, new BigDecimal("5000000"), null, null, null, LocalDate.of(2026, 5, 1), null, null, null, List.of()),
                new DebtImportParsedRow(3, null, null, null, null, null, null, null, null, null, null, List.of("Nombre requerido"))
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(null))).thenReturn(10L);

        var response = useCase.importDebts(command("debts.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows()).anyMatch(row -> !row.valid());
        verify(createManualDebtPort, never()).createManualDebt(any());
    }

    @Test
    void previewReturnsParsedRowDataAndDoesNotCreate() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new DebtImportParsedRow(2, "Prestamo carro", "Compra vehiculo", new BigDecimal("5000000"), new BigDecimal("3000000"), null, null, LocalDate.of(2026, 5, 1), null, "Ana Gomez <ana@example.com>", "Nota", List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L))).thenReturn(20L);

        var response = useCase.previewDebts(command("debts.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().name()).isEqualTo("Prestamo carro");
        assertThat(response.rows().getFirst().description()).isEqualTo("Compra vehiculo");
        assertThat(response.rows().getFirst().totalAmount()).isEqualByComparingTo("5000000");
        assertThat(response.rows().getFirst().remainingBalance()).isEqualByComparingTo("3000000");
        assertThat(response.rows().getFirst().participantLabel()).isEqualTo("Ana Gomez <ana@example.com>");
        assertThat(response.rows().getFirst().participantId()).isEqualTo(20L);
        verify(createManualDebtPort, never()).createManualDebt(any());
    }

    @Test
    void importPassesInitialRemainingBalanceToCreateManualDebtCommand() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new DebtImportParsedRow(2, "Prestamo carro", null, new BigDecimal("5000000"), new BigDecimal("3000000"), null, null, LocalDate.of(2026, 5, 1), null, null, null, List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(null))).thenReturn(10L);
        when(createManualDebtPort.createManualDebt(any())).thenReturn(debtResponse(101L, 10L));

        useCase.importDebts(command("debts.xlsx"));

        var captor = forClass(CreateManualDebtCommand.class);
        verify(createManualDebtPort).createManualDebt(captor.capture());
        assertThat(captor.getValue().initialRemainingBalance().amount()).isEqualByComparingTo("3000000");
    }

    @Test
    void importDoesNotCreateWhenMemberAssignsAnotherParticipant() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_MEMBER);
        givenParticipants();
        when(parserPort.parse(any(), any())).thenReturn(List.of(
                new DebtImportParsedRow(2, "Prestamo carro", null, new BigDecimal("5000000"), null, null, null, LocalDate.of(2026, 5, 1), null, "Ana Gomez <ana@example.com>", null, List.of())
        ));
        when(assignedParticipantValidator.resolveAssignedParticipantId(any(), eq(20L)))
                .thenThrow(new ForbiddenOperationException("ASSIGNED_PARTICIPANT_NOT_ALLOWED", "Only account admins can assign records to another participant."));

        var response = useCase.importDebts(command("debts.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().valid()).isFalse();
        assertThat(response.rows().getFirst().errors()).contains("Participante no permitido para el usuario actual");
        verify(createManualDebtPort, never()).createManualDebt(any());
    }

    @Test
    void importFailsForInvalidFileExtension() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_ADMIN);

        assertThatThrownBy(() -> useCase.importDebts(command("debts.xls")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void generateTemplateReturnsParticipantLabels() {
        givenCurrentUser();
        givenAccountAccess(AccountParticipantRole.ACCOUNT_MEMBER);
        givenParticipants();
        when(templateGeneratorPort.generate(any())).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).isEqualTo("easy-finance-debt-import-template.xlsx");
        assertThat(response.content()).containsExactly(1, 2, 3);
        verify(accountAuthorizationService).requireActiveMember(1L, 10L);
    }

    private void givenCurrentUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "user@example.com", Set.of("USER"), true)));
    }

    private void givenAccountAccess(AccountParticipantRole role) {
        var access = new AccountAccess(
                Account.restore(1L, "Casa", null, AccountStatus.ACTIVE, Instant.now(), Instant.now()),
                AccountParticipant.restore(1L, 1L, 10L, role, AccountParticipantStatus.ACTIVE, Instant.now(), Instant.now(), Instant.now())
        );
        when(accountAuthorizationService.requireActiveMemberForActiveAccount(1L, 10L)).thenReturn(access);
        when(accountAuthorizationService.requireActiveMember(1L, 10L)).thenReturn(access);
    }

    private void givenParticipants() {
        when(accountParticipantRepository.findByAccountId(1L)).thenReturn(List.of(
                AccountParticipant.restore(1L, 1L, 10L, AccountParticipantRole.ACCOUNT_ADMIN, AccountParticipantStatus.ACTIVE, Instant.now(), Instant.now(), Instant.now()),
                AccountParticipant.restore(2L, 1L, 20L, AccountParticipantRole.ACCOUNT_MEMBER, AccountParticipantStatus.ACTIVE, Instant.now(), Instant.now(), Instant.now())
        ));
        when(participantLookupPort.findByParticipantIds(List.of(10L, 20L))).thenReturn(Map.of(
                10L, new ParticipantInfo(10L, 1L, "user@example.com", "Usuario Actual", true),
                20L, new ParticipantInfo(20L, 2L, "ana@example.com", "Ana Gomez", true)
        ));
    }

    private static DebtResponse debtResponse(Long id, Long participantId) {
        return new DebtResponse(
                id, 1L, participantId, null, "MANUAL", "Loan", null,
                new BigDecimal("5000000.00"), new BigDecimal("5000000.00"), "COP",
                new BigDecimal("5000000.00"), "COP", null, null, null,
                LocalDate.of(2026, 5, 1), null, "ACTIVE", null, Instant.now(), Instant.now()
        );
    }

    private static ImportDebtCommand command(String filename) {
        return new ImportDebtCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }
}
