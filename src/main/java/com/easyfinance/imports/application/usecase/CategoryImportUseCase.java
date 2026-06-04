package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.command.CreateCategoryCommand;
import com.easyfinance.catalogs.application.port.in.CreateCategoryPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.imports.application.port.in.GenerateCategoryImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportCategoryPort;
import com.easyfinance.imports.application.port.in.PreviewCategoryImportPort;
import com.easyfinance.imports.application.port.out.CategoryImportParserPort;
import com.easyfinance.imports.application.port.out.CategoryImportTemplateGeneratorPort;
import com.easyfinance.imports.application.response.CategoryImportResponse;
import com.easyfinance.imports.application.response.CategoryImportRowResponse;
import com.easyfinance.imports.application.response.CategoryImportTemplateResponse;
import com.easyfinance.imports.application.validation.CategoryImportParsedRow;
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
public class CategoryImportUseCase implements GenerateCategoryImportTemplatePort, ImportCategoryPort, PreviewCategoryImportPort {

    private static final String TEMPLATE_FILENAME = "easy-finance-category-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CategoryImportParserPort parserPort;
    private final CategoryImportTemplateGeneratorPort templateGeneratorPort;
    private final CategoryRepositoryPort categoryRepository;
    private final CreateCategoryPort createCategoryPort;
    private final long maxFileSizeBytes;

    public CategoryImportUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CategoryImportParserPort parserPort,
            CategoryImportTemplateGeneratorPort templateGeneratorPort,
            CategoryRepositoryPort categoryRepository,
            CreateCategoryPort createCategoryPort,
            @Value("${easy-finance.imports.categories.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.categoryRepository = categoryRepository;
        this.createCategoryPort = createCategoryPort;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentUser().participantId());
        byte[] content = templateGeneratorPort.generate();
        return new CategoryImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public CategoryImportResponse importCategories(ImportCategoryCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser().participantId());
        ValidatedCategoryImport validatedImport = validateImport(command);

        if (validatedImport.hasErrors()) {
            return new CategoryImportResponse(0, validatedImport.rows());
        }

        try {
            List<CategoryImportRowResponse> createdRows = new ArrayList<>();
            for (ValidatedCategoryRow row : validatedImport.validRows()) {
                var created = createCategoryPort.createCategory(
                        new CreateCategoryCommand(command.accountId(), row.name(), row.description(), row.type())
                );
                createdRows.add(new CategoryImportRowResponse(
                        row.rowNumber(),
                        row.name(),
                        row.description(),
                        row.type(),
                        true,
                        created.id(),
                        List.of()
                ));
            }
            return new CategoryImportResponse(createdRows.size(), createdRows);
        } catch (BusinessRuleViolationException ex) {
            if ("CATEGORY_ALREADY_EXISTS".equals(ex.code())) {
                throw ex;
            }
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException("CATEGORY_ALREADY_EXISTS", "Category already exists.", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryImportResponse previewCategories(ImportCategoryCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser().participantId());
        return new CategoryImportResponse(0, validateImport(command).rows());
    }

    private ValidatedCategoryImport validateImport(ImportCategoryCommand command) {
        validateFile(command);

        List<CategoryImportParsedRow> parsedRows = parserPort.parse(command);
        List<CategoryImportRowResponse> validationRows = new ArrayList<>();
        List<ValidatedCategoryRow> validRows = new ArrayList<>();

        for (CategoryImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            if (errors.isEmpty() && parsedRow.type() != null && parsedRow.name() != null && !parsedRow.name().isBlank()) {
                if (categoryRepository.existsActiveByAccountIdAndTypeAndNormalizedName(
                        command.accountId(),
                        parsedRow.type(),
                        parsedRow.name().trim().toLowerCase(Locale.ROOT)
                )) {
                    errors.add("Categoria ya existe");
                }
            }

            boolean valid = errors.isEmpty();
            if (valid) {
                validRows.add(new ValidatedCategoryRow(
                        parsedRow.rowNumber(),
                        parsedRow.name().trim(),
                        parsedRow.description(),
                        parsedRow.type()
                ));
            }
            validationRows.add(new CategoryImportRowResponse(
                    parsedRow.rowNumber(),
                    parsedRow.name(),
                    parsedRow.description(),
                    parsedRow.type(),
                    valid,
                    null,
                    errors
            ));
        }

        return new ValidatedCategoryImport(validationRows, validRows);
    }

    private void validateFile(ImportCategoryCommand command) {
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

    private record ValidatedCategoryRow(
            Integer rowNumber,
            String name,
            String description,
            CategoryType type
    ) {
    }

    private record ValidatedCategoryImport(
            List<CategoryImportRowResponse> rows,
            List<ValidatedCategoryRow> validRows
    ) {
        boolean hasErrors() {
            return rows.stream().anyMatch(row -> !row.valid());
        }
    }
}
