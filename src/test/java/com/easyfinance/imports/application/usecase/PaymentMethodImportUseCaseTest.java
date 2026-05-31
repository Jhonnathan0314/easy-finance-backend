package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.port.in.CreatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.imports.application.command.ImportPaymentMethodCommand;
import com.easyfinance.imports.application.port.out.PaymentMethodImportParserPort;
import com.easyfinance.imports.application.port.out.PaymentMethodImportTemplateGeneratorPort;
import com.easyfinance.imports.application.validation.PaymentMethodImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMethodImportUseCaseTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final AccountAuthorizationService accountAuthorizationService = mock(AccountAuthorizationService.class);
    private final PaymentMethodImportParserPort parserPort = mock(PaymentMethodImportParserPort.class);
    private final PaymentMethodImportTemplateGeneratorPort templateGeneratorPort = mock(PaymentMethodImportTemplateGeneratorPort.class);
    private final PaymentMethodRepositoryPort paymentMethodRepository = mock(PaymentMethodRepositoryPort.class);
    private final CreatePaymentMethodPort createPaymentMethodPort = mock(CreatePaymentMethodPort.class);
    private final PaymentMethodImportUseCase useCase = new PaymentMethodImportUseCase(
            currentUserProvider,
            accountAuthorizationService,
            parserPort,
            templateGeneratorPort,
            paymentMethodRepository,
            createPaymentMethodPort,
            5_242_880
    );

    @Test
    void importCreatesAllWhenRowsAreValid() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new PaymentMethodImportParsedRow(2, "Efectivo", PaymentMethodType.CASH, List.of()),
                new PaymentMethodImportParsedRow(3, "Nequi", PaymentMethodType.DIGITAL_WALLET, List.of())
        ));
        when(createPaymentMethodPort.createPaymentMethod(any()))
                .thenReturn(paymentMethod(101L, "Efectivo", "CASH"))
                .thenReturn(paymentMethod(102L, "Nequi", "DIGITAL_WALLET"));

        var response = useCase.importPaymentMethods(command("payment-methods.xlsx"));

        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.rows()).allMatch(row -> row.valid() && row.createdPaymentMethodId() != null);
    }

    @Test
    void importDoesNotCreateAnyWhenOneRowIsInvalid() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new PaymentMethodImportParsedRow(2, "Efectivo", PaymentMethodType.CASH, List.of()),
                new PaymentMethodImportParsedRow(3, null, null, List.of("Nombre requerido"))
        ));

        var response = useCase.importPaymentMethods(command("payment-methods.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows()).anyMatch(row -> !row.valid());
        verify(createPaymentMethodPort, never()).createPaymentMethod(any());
    }

    @Test
    void importDetectsDuplicateActivePaymentMethodInDatabase() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new PaymentMethodImportParsedRow(2, "Efectivo", PaymentMethodType.CASH, List.of())
        ));
        when(paymentMethodRepository.existsActiveByAccountIdAndNormalizedName(1L, "efectivo"))
                .thenReturn(true);

        var response = useCase.importPaymentMethods(command("payment-methods.xlsx"));

        assertThat(response.createdCount()).isZero();
        assertThat(response.rows().getFirst().errors()).contains("Medio de pago ya existe");
        verify(createPaymentMethodPort, never()).createPaymentMethod(any());
    }

    @Test
    void importAllowsSameNameWhenOnlyInactiveExists() {
        givenCurrentUser();
        when(parserPort.parse(any())).thenReturn(List.of(
                new PaymentMethodImportParsedRow(2, "Efectivo", PaymentMethodType.CASH, List.of())
        ));
        when(paymentMethodRepository.existsActiveByAccountIdAndNormalizedName(1L, "efectivo"))
                .thenReturn(false);
        when(createPaymentMethodPort.createPaymentMethod(any())).thenReturn(paymentMethod(101L, "Efectivo", "CASH"));

        var response = useCase.importPaymentMethods(command("payment-methods.xlsx"));

        assertThat(response.createdCount()).isEqualTo(1);
        assertThat(response.rows().getFirst().createdPaymentMethodId()).isEqualTo(101L);
    }

    @Test
    void importFailsForInvalidExtension() {
        givenCurrentUser();

        assertThatThrownBy(() -> useCase.importPaymentMethods(command("payment-methods.xls")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("IMPORT_FILE_INVALID_TYPE"));
    }

    @Test
    void generateTemplateReturnsWorkbook() {
        givenCurrentUser();
        when(templateGeneratorPort.generate()).thenReturn(new byte[]{1, 2, 3});

        var response = useCase.generate(1L);

        assertThat(response.filename()).isEqualTo("easy-finance-payment-method-import-template.xlsx");
        assertThat(response.content()).containsExactly(1, 2, 3);
        verify(accountAuthorizationService).requireActiveAdminForActiveAccount(1L, 10L);
    }

    @Test
    void importRequiresAdminRole() {
        givenCurrentUser();
        when(accountAuthorizationService.requireActiveAdminForActiveAccount(1L, 10L))
                .thenThrow(new ForbiddenOperationException("ACCOUNT_ADMIN_REQUIRED", "Account admin role is required."));

        assertThatThrownBy(() -> useCase.importPaymentMethods(command("payment-methods.xlsx")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("ACCOUNT_ADMIN_REQUIRED"));
    }

    @Test
    void importRequiresActiveAccount() {
        givenCurrentUser();
        when(accountAuthorizationService.requireActiveAdminForActiveAccount(1L, 10L))
                .thenThrow(new ForbiddenOperationException("ACCOUNT_NOT_ACTIVE", "Account is not active."));

        assertThatThrownBy(() -> useCase.importPaymentMethods(command("payment-methods.xlsx")))
                .isInstanceOfSatisfying(ForbiddenOperationException.class, ex ->
                        assertThat(ex.code()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
    }

    private void givenCurrentUser() {
        when(currentUserProvider.currentUser()).thenReturn(Optional.of(new CurrentUser(1L, 10L, "admin@example.com", Set.of("USER"), true)));
    }

    private static ImportPaymentMethodCommand command(String filename) {
        return new ImportPaymentMethodCommand(1L, filename, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 100, new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    private static PaymentMethodResponse paymentMethod(Long id, String name, String type) {
        return new PaymentMethodResponse(id, 1L, name, null, type, "ACTIVE", Instant.now(), Instant.now());
    }
}

