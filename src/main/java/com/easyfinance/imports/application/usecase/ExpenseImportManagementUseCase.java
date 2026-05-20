package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.expenses.application.command.CreateImportedExpenseCommand;
import com.easyfinance.expenses.application.port.in.CreateImportedExpensePort;
import com.easyfinance.expenses.domain.model.ExpensePaymentState;
import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.application.port.in.ConfirmExpenseImportPort;
import com.easyfinance.imports.application.port.in.GenerateExpenseImportTemplatePort;
import com.easyfinance.imports.application.port.in.GetExpenseImportBatchPort;
import com.easyfinance.imports.application.port.in.PreviewExpenseImportPort;
import com.easyfinance.imports.application.port.out.ExpenseImportParserPort;
import com.easyfinance.imports.application.port.out.ExpenseImportRepositoryPort;
import com.easyfinance.imports.application.port.out.ExpenseImportTemplateGeneratorPort;
import com.easyfinance.imports.application.response.ExpenseImportBatchResponse;
import com.easyfinance.imports.application.response.ExpenseImportRowResponse;
import com.easyfinance.imports.application.response.ExpenseImportTemplateResponse;
import com.easyfinance.imports.application.response.ImportRowErrorResponse;
import com.easyfinance.imports.application.template.ExpenseImportTemplateData;
import com.easyfinance.imports.domain.model.ExpenseImportBatch;
import com.easyfinance.imports.domain.model.ExpenseImportRow;
import com.easyfinance.imports.domain.model.ImportRowError;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.DomainException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ExpenseImportManagementUseCase implements PreviewExpenseImportPort, ConfirmExpenseImportPort, GetExpenseImportBatchPort, GenerateExpenseImportTemplatePort {

    private static final String TEMPLATE_FILENAME = "easy-finance-expense-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CatalogValidationPort catalogValidationPort;
    private final CategoryRepositoryPort categoryRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final ExpenseImportParserPort parserPort;
    private final ExpenseImportTemplateGeneratorPort templateGeneratorPort;
    private final ExpenseImportRepositoryPort importRepository;
    private final CreateImportedExpensePort createImportedExpensePort;
    private final long maxFileSizeBytes;

    public ExpenseImportManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            CategoryRepositoryPort categoryRepository,
            PaymentMethodRepositoryPort paymentMethodRepository,
            ExpenseImportParserPort parserPort,
            ExpenseImportTemplateGeneratorPort templateGeneratorPort,
            ExpenseImportRepositoryPort importRepository,
            CreateImportedExpensePort createImportedExpensePort,
            @Value("${easy-finance.imports.expenses.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.catalogValidationPort = catalogValidationPort;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.importRepository = importRepository;
        this.createImportedExpensePort = createImportedExpensePort;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public ExpenseImportBatchResponse preview(PreviewExpenseImportCommand command) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        validateFile(command);
        List<ExpenseImportRow> parsedRows = parserPort.parse(command, command.accountId());
        List<ExpenseImportRow> validatedRows = parsedRows.stream().map(this::validateCatalogs).toList();
        ExpenseImportBatch batch = ExpenseImportBatch.preview(command.accountId(), currentUser.participantId(), command.originalFilename(), validatedRows);
        return toResponse(importRepository.savePreview(batch));
    }

    @Override
    @Transactional
    public ExpenseImportBatchResponse confirm(Long accountId, Long batchId) {
        CurrentUser currentUser = currentUser();
        accountAuthorizationService.requireActiveMemberForActiveAccount(accountId, currentUser.participantId());
        ExpenseImportBatch batch = importRepository.findByAccountIdAndIdForUpdate(accountId, batchId)
                .orElseThrow(() -> new NotFoundException("IMPORT_BATCH_NOT_FOUND", "Import batch was not found."));
        ExpenseImportBatch confirmed = batch.confirm(Instant.now());
        List<ExpenseImportRow> validRows = batch.rows().stream().filter(ExpenseImportRow::valid).toList();
        try {
            for (ExpenseImportRow row : validRows) {
                var expense = createImportedExpensePort.createImportedExpense(new CreateImportedExpenseCommand(
                        accountId,
                        row.categoryId(),
                        row.paymentMethodId(),
                        batch.participantId(),
                        row.description(),
                        row.amount(),
                        row.expenseDate(),
                        row.paymentState()
                ));
                importRepository.updateCreatedExpenseId(accountId, row.id(), expense.id());
            }
            return toResponse(importRepository.saveBatch(confirmed));
        } catch (DomainException ex) {
            throw new BusinessRuleViolationException("IMPORT_CONFIRMATION_FAILED", "Expense import confirmation failed.", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseImportBatchResponse get(Long accountId, Long batchId) {
        accountAuthorizationService.requireActiveMember(accountId, currentUser().participantId());
        ExpenseImportBatch batch = importRepository.findByAccountIdAndId(accountId, batchId)
                .orElseThrow(() -> new NotFoundException("IMPORT_BATCH_NOT_FOUND", "Import batch was not found."));
        return toResponse(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveMember(accountId, currentUser().participantId());
        var categoryNames = categoryRepository.findActiveExpenseByAccountId(accountId)
                .stream()
                .map(category -> category.name())
                .toList();
        var paymentMethodNames = paymentMethodRepository.findActiveByAccountId(accountId)
                .stream()
                .map(paymentMethod -> paymentMethod.name())
                .toList();
        byte[] content = templateGeneratorPort.generate(new ExpenseImportTemplateData(categoryNames, paymentMethodNames));
        return new ExpenseImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    private ExpenseImportRow validateCatalogs(ExpenseImportRow row) {
        if (!row.valid()) {
            return row;
        }
        List<ImportRowError> errors = new ArrayList<>(row.errors());
        Long categoryId = row.categoryId();
        Long paymentMethodId = row.paymentMethodId();

        var category = catalogValidationPort.findCategoryForValidation(row.accountId(), normalize(row.categoryName()));
        if (category.isEmpty()) {
            errors.add(new ImportRowError("Categoría", "CATEGORY_NOT_FOUND", "Category was not found."));
        } else {
            CategoryValidationView value = category.get();
            categoryId = value.id();
            if (value.status() != CatalogStatus.ACTIVE) {
                errors.add(new ImportRowError("Categoría", "CATEGORY_INACTIVE", "Category is inactive."));
            }
            if (value.type() != CategoryType.EXPENSE) {
                errors.add(new ImportRowError("Categoría", "CATEGORY_INVALID_TYPE", "Category must be an EXPENSE category."));
            }
        }

        var paymentMethod = catalogValidationPort.findPaymentMethodForValidation(row.accountId(), normalize(row.paymentMethodName()));
        if (paymentMethod.isEmpty()) {
            errors.add(new ImportRowError("MedioPago", "PAYMENT_METHOD_NOT_FOUND", "Payment method was not found."));
        } else {
            PaymentMethodValidationView value = paymentMethod.get();
            paymentMethodId = value.id();
            if (value.status() != CatalogStatus.ACTIVE) {
                errors.add(new ImportRowError("MedioPago", "PAYMENT_METHOD_INACTIVE", "Payment method is inactive."));
            }
        }

        return copyWithValidation(row, categoryId, paymentMethodId, errors);
    }

    private static ExpenseImportRow copyWithValidation(ExpenseImportRow row, Long categoryId, Long paymentMethodId, List<ImportRowError> errors) {
        return new ExpenseImportRow(row.id(), row.accountId(), row.batchId(), row.rowNumber(), row.expenseDate(), row.description(), row.amount(), row.categoryName(), categoryId, row.paymentMethodName(), paymentMethodId, row.paymentState(), errors.isEmpty(), errors, row.createdExpenseId(), row.createdAt(), row.updatedAt());
    }

    private void validateFile(PreviewExpenseImportCommand command) {
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static ExpenseImportBatchResponse toResponse(ExpenseImportBatch batch) {
        return new ExpenseImportBatchResponse(
                batch.id(),
                batch.accountId(),
                batch.participantId(),
                batch.originalFilename(),
                batch.status().name(),
                batch.totalRows(),
                batch.validRows(),
                batch.invalidRows(),
                batch.confirmedAt(),
                batch.rows().stream().map(ExpenseImportManagementUseCase::toRowResponse).toList()
        );
    }

    private static ExpenseImportRowResponse toRowResponse(ExpenseImportRow row) {
        return new ExpenseImportRowResponse(
                row.id(),
                row.rowNumber(),
                row.expenseDate(),
                row.description(),
                row.amount() == null ? null : row.amount().amount(),
                row.amount() == null ? "COP" : row.amount().currency().name(),
                row.categoryName(),
                row.categoryId(),
                row.paymentMethodName(),
                row.paymentMethodId(),
                row.paymentState() == null ? null : row.paymentState().name(),
                row.valid(),
                row.errors().stream().map(error -> new ImportRowErrorResponse(error.column(), error.code(), error.message())).toList(),
                row.createdExpenseId()
        );
    }
}
