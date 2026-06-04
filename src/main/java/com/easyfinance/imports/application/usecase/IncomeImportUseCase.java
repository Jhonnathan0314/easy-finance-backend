package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportIncomeCommand;
import com.easyfinance.imports.application.port.in.GenerateIncomeImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportIncomePort;
import com.easyfinance.imports.application.port.in.PreviewIncomeImportPort;
import com.easyfinance.imports.application.port.out.IncomeImportParserPort;
import com.easyfinance.imports.application.port.out.IncomeImportTemplateGeneratorPort;
import com.easyfinance.imports.application.response.IncomeImportResponse;
import com.easyfinance.imports.application.response.IncomeImportRowResponse;
import com.easyfinance.imports.application.response.IncomeImportTemplateResponse;
import com.easyfinance.imports.application.template.IncomeImportTemplateData;
import com.easyfinance.imports.application.validation.IncomeImportParsedRow;
import com.easyfinance.income.application.command.CreateIncomeCommand;
import com.easyfinance.income.application.port.in.CreateIncomePort;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class IncomeImportUseCase implements GenerateIncomeImportTemplatePort, ImportIncomePort, PreviewIncomeImportPort {

    private static final String TEMPLATE_FILENAME = "easy-finance-income-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CatalogValidationPort catalogValidationPort;
    private final CategoryRepositoryPort categoryRepository;
    private final IncomeImportParserPort parserPort;
    private final IncomeImportTemplateGeneratorPort templateGeneratorPort;
    private final CreateIncomePort createIncomePort;
    private final long maxFileSizeBytes;

    public IncomeImportUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            CategoryRepositoryPort categoryRepository,
            IncomeImportParserPort parserPort,
            IncomeImportTemplateGeneratorPort templateGeneratorPort,
            CreateIncomePort createIncomePort,
            @Value("${easy-finance.imports.incomes.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.catalogValidationPort = catalogValidationPort;
        this.categoryRepository = categoryRepository;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.createIncomePort = createIncomePort;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveMember(accountId, currentUser().participantId());
        var categoryNames = categoryRepository.findActiveIncomeByAccountId(accountId)
                .stream()
                .map(category -> category.name())
                .toList();
        byte[] content = templateGeneratorPort.generate(new IncomeImportTemplateData(categoryNames));
        return new IncomeImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public IncomeImportResponse importIncomes(ImportIncomeCommand command) {
        accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser().participantId());
        ValidatedIncomeImport validatedImport = validateImport(command);

        if (validatedImport.hasErrors()) {
            return new IncomeImportResponse(0, validatedImport.rows());
        }

        List<IncomeImportRowResponse> createdRows = new ArrayList<>();
        for (ValidatedIncomeRow row : validatedImport.validRows()) {
            var created = createIncomePort.createIncome(new CreateIncomeCommand(
                    command.accountId(),
                    row.categoryId(),
                    row.description(),
                    com.easyfinance.shared.domain.Money.cop(row.amount()),
                    row.incomeDate()
            ));
            createdRows.add(new IncomeImportRowResponse(
                    row.rowNumber(),
                    row.incomeDate(),
                    row.description(),
                    row.categoryName(),
                    row.categoryId(),
                    row.amount(),
                    true,
                    created.id(),
                    List.of()
            ));
        }
        return new IncomeImportResponse(createdRows.size(), createdRows);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeImportResponse previewIncomes(ImportIncomeCommand command) {
        accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser().participantId());
        return new IncomeImportResponse(0, validateImport(command).rows());
    }

    private ValidatedIncomeImport validateImport(ImportIncomeCommand command) {
        validateFile(command);

        List<IncomeImportParsedRow> parsedRows = parserPort.parse(command, command.accountId());
        List<IncomeImportRowResponse> validationRows = new ArrayList<>();
        List<ValidatedIncomeRow> validRows = new ArrayList<>();

        for (IncomeImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            Long categoryId = resolveCategory(command.accountId(), parsedRow.categoryName(), errors);
            boolean valid = errors.isEmpty() && categoryId != null;
            if (valid) {
                validRows.add(new ValidatedIncomeRow(
                        parsedRow.rowNumber(),
                        parsedRow.incomeDate(),
                        parsedRow.description(),
                        parsedRow.categoryName(),
                        parsedRow.amount(),
                        categoryId
                ));
            }
            validationRows.add(new IncomeImportRowResponse(
                    parsedRow.rowNumber(),
                    parsedRow.incomeDate(),
                    parsedRow.description(),
                    parsedRow.categoryName(),
                    categoryId,
                    parsedRow.amount(),
                    valid,
                    null,
                    errors
            ));
        }

        return new ValidatedIncomeImport(validationRows, validRows);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long resolveCategory(Long accountId, String categoryName, List<String> errors) {
        var found = catalogValidationPort.findCategoryForValidation(accountId, normalize(categoryName));
        if (found.isEmpty()) {
            errors.add("Categoria no encontrada o inactiva");
            return null;
        }
        CategoryValidationView category = found.get();
        if (category.status() != CatalogStatus.ACTIVE) {
            errors.add("Categoria no encontrada o inactiva");
            return null;
        }
        if (category.type() != CategoryType.INCOME) {
            errors.add("Categoria debe ser de tipo INCOME");
            return null;
        }
        return category.id();
    }

    private void validateFile(ImportIncomeCommand command) {
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

    private record ValidatedIncomeRow(
            Integer rowNumber,
            java.time.LocalDate incomeDate,
            String description,
            String categoryName,
            BigDecimal amount,
            Long categoryId
    ) {
    }

    private record ValidatedIncomeImport(
            List<IncomeImportRowResponse> rows,
            List<ValidatedIncomeRow> validRows
    ) {
        boolean hasErrors() {
            return rows.stream().anyMatch(row -> !row.valid());
        }
    }
}

