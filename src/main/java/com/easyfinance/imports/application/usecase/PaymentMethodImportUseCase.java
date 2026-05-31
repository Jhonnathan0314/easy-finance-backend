package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.command.CreatePaymentMethodCommand;
import com.easyfinance.catalogs.application.port.in.CreatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.imports.application.command.ImportPaymentMethodCommand;
import com.easyfinance.imports.application.port.in.GeneratePaymentMethodImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportPaymentMethodPort;
import com.easyfinance.imports.application.port.out.PaymentMethodImportParserPort;
import com.easyfinance.imports.application.port.out.PaymentMethodImportTemplateGeneratorPort;
import com.easyfinance.imports.application.response.PaymentMethodImportResponse;
import com.easyfinance.imports.application.response.PaymentMethodImportRowResponse;
import com.easyfinance.imports.application.response.PaymentMethodImportTemplateResponse;
import com.easyfinance.imports.application.validation.PaymentMethodImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PaymentMethodImportUseCase implements GeneratePaymentMethodImportTemplatePort, ImportPaymentMethodPort {

    private static final String TEMPLATE_FILENAME = "easy-finance-payment-method-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final PaymentMethodImportParserPort parserPort;
    private final PaymentMethodImportTemplateGeneratorPort templateGeneratorPort;
    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final CreatePaymentMethodPort createPaymentMethodPort;
    private final long maxFileSizeBytes;

    public PaymentMethodImportUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            PaymentMethodImportParserPort parserPort,
            PaymentMethodImportTemplateGeneratorPort templateGeneratorPort,
            PaymentMethodRepositoryPort paymentMethodRepository,
            CreatePaymentMethodPort createPaymentMethodPort,
            @Value("${easy-finance.imports.payment-methods.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.paymentMethodRepository = paymentMethodRepository;
        this.createPaymentMethodPort = createPaymentMethodPort;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentUser().participantId());
        byte[] content = templateGeneratorPort.generate();
        return new PaymentMethodImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public PaymentMethodImportResponse importPaymentMethods(ImportPaymentMethodCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser().participantId());
        validateFile(command);

        List<PaymentMethodImportParsedRow> parsedRows = parserPort.parse(command);
        List<PaymentMethodImportRowResponse> validationRows = new ArrayList<>();
        List<ValidatedPaymentMethodRow> validRows = new ArrayList<>();

        for (PaymentMethodImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            if (errors.isEmpty() && parsedRow.name() != null && !parsedRow.name().isBlank()) {
                if (paymentMethodRepository.existsActiveByAccountIdAndNormalizedName(
                        command.accountId(),
                        parsedRow.name().trim().toLowerCase(Locale.ROOT)
                )) {
                    errors.add("Medio de pago ya existe");
                }
            }
            if (!errors.isEmpty()) {
                validationRows.add(new PaymentMethodImportRowResponse(parsedRow.rowNumber(), false, null, errors));
                continue;
            }
            validRows.add(new ValidatedPaymentMethodRow(parsedRow.rowNumber(), parsedRow.name().trim(), parsedRow.type()));
            validationRows.add(new PaymentMethodImportRowResponse(parsedRow.rowNumber(), true, null, List.of()));
        }

        boolean hasErrors = validationRows.stream().anyMatch(row -> !row.valid());
        if (hasErrors) {
            return new PaymentMethodImportResponse(0, validationRows);
        }

        try {
            List<PaymentMethodImportRowResponse> createdRows = new ArrayList<>();
            for (ValidatedPaymentMethodRow row : validRows) {
                var created = createPaymentMethodPort.createPaymentMethod(
                        new CreatePaymentMethodCommand(command.accountId(), row.name(), null, row.type())
                );
                createdRows.add(new PaymentMethodImportRowResponse(row.rowNumber(), true, created.id(), List.of()));
            }
            return new PaymentMethodImportResponse(createdRows.size(), createdRows);
        } catch (BusinessRuleViolationException ex) {
            if ("PAYMENT_METHOD_ALREADY_EXISTS".equals(ex.code())) {
                throw ex;
            }
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_ALREADY_EXISTS", "Payment method already exists.", ex);
        }
    }

    private void validateFile(ImportPaymentMethodCommand command) {
        if (command.inputStream() == null || command.originalFilename() == null || command.originalFilename().isBlank()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        if (!command.originalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessRuleViolationException("IMPORT_FILE_INVALID_TYPE", "Only .xlsx files are supported.");
        }
        if (command.sizeBytes() > maxFileSizeBytes) {
            throw new BusinessRuleViolationException("IMPORT_FILE_TOO_LARGE", "Import file is too large.");
        }
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private record ValidatedPaymentMethodRow(
            Integer rowNumber,
            String name,
            PaymentMethodType type
    ) {
    }
}

