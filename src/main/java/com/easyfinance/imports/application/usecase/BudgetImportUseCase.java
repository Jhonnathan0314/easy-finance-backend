package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.budgets.application.port.out.BudgetRepositoryPort;
import com.easyfinance.budgets.application.port.out.SubBudgetRepositoryPort;
import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.domain.model.SubBudget;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.port.in.GenerateAnnualBudgetImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportAnnualBudgetPort;
import com.easyfinance.imports.application.port.out.AnnualBudgetImportParserPort;
import com.easyfinance.imports.application.port.out.AnnualBudgetImportTemplateGeneratorPort;
import com.easyfinance.imports.application.response.AnnualBudgetImportResponse;
import com.easyfinance.imports.application.response.AnnualBudgetImportRowResponse;
import com.easyfinance.imports.application.response.AnnualBudgetImportTemplateResponse;
import com.easyfinance.imports.application.template.AnnualBudgetImportTemplateData;
import com.easyfinance.imports.application.validation.AnnualBudgetImportMonthScope;
import com.easyfinance.imports.application.validation.AnnualBudgetImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class BudgetImportUseCase implements GenerateAnnualBudgetImportTemplatePort, ImportAnnualBudgetPort {

    private static final String TEMPLATE_FILENAME = "easy-finance-annual-budget-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AnnualBudgetImportParserPort parserPort;
    private final AnnualBudgetImportTemplateGeneratorPort templateGeneratorPort;
    private final CategoryRepositoryPort categoryRepository;
    private final CatalogValidationPort catalogValidationPort;
    private final BudgetRepositoryPort budgetRepository;
    private final SubBudgetRepositoryPort subBudgetRepository;
    private final long maxFileSizeBytes;

    public BudgetImportUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            AnnualBudgetImportParserPort parserPort,
            AnnualBudgetImportTemplateGeneratorPort templateGeneratorPort,
            CategoryRepositoryPort categoryRepository,
            CatalogValidationPort catalogValidationPort,
            BudgetRepositoryPort budgetRepository,
            SubBudgetRepositoryPort subBudgetRepository,
            @Value("${easy-finance.imports.budgets-annual.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.categoryRepository = categoryRepository;
        this.catalogValidationPort = catalogValidationPort;
        this.budgetRepository = budgetRepository;
        this.subBudgetRepository = subBudgetRepository;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public AnnualBudgetImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentUser().participantId());
        List<String> categoryNames = categoryRepository.findActiveExpenseByAccountId(accountId).stream()
                .map(category -> category.name())
                .toList();
        byte[] content = templateGeneratorPort.generate(new AnnualBudgetImportTemplateData(categoryNames));
        return new AnnualBudgetImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public AnnualBudgetImportResponse importAnnualBudget(ImportAnnualBudgetCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser().participantId());
        validateFile(command);

        List<AnnualBudgetImportParsedRow> parsedRows = parserPort.parse(command);
        List<AnnualBudgetImportRowResponse> rowResponses = new ArrayList<>();
        if (parsedRows.isEmpty()) {
            return new AnnualBudgetImportResponse(null, 0, 0, List.of());
        }

        Integer year = null;
        String budgetName = null;
        Map<CombinationKey, ValidatedRow> baseRows = new HashMap<>();
        Map<Integer, Map<CombinationKey, ValidatedRow>> monthRows = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthRows.put(month, new HashMap<>());
        }

        boolean hasErrors = false;
        for (AnnualBudgetImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            Integer rowYear = parsedRow.year();
            if (rowYear != null) {
                if (year == null) {
                    year = rowYear;
                } else if (!Objects.equals(year, rowYear)) {
                    errors.add("Todas las filas deben tener el mismo año");
                }
            }

            if (parsedRow.budgetName() != null && !parsedRow.budgetName().isBlank()) {
                String current = parsedRow.budgetName().trim();
                if (budgetName == null) {
                    budgetName = current;
                } else if (!budgetName.equals(current)) {
                    errors.add("NombrePresupuesto debe ser consistente para todo el año");
                }
            }

            Long categoryId = resolveCategory(command.accountId(), parsedRow.categoryName(), errors);
            String normalizedSubBudgetName = normalize(parsedRow.subBudgetName());
            if (parsedRow.subBudgetName() != null && parsedRow.subBudgetName().length() > 150) {
                errors.add("NombreSubpresupuesto supera el máximo permitido");
            }
            if (errors.isEmpty()) {
                CombinationKey key = new CombinationKey(categoryId, normalizedSubBudgetName);
                ValidatedRow validated = new ValidatedRow(
                        parsedRow.rowNumber(),
                        parsedRow.subBudgetName().trim(),
                        categoryId,
                        Money.cop(parsedRow.plannedAmount()),
                        parsedRow.monthScope()
                );
                if (parsedRow.monthScope() instanceof AnnualBudgetImportMonthScope.AllMonths) {
                    if (baseRows.putIfAbsent(key, validated) != null) {
                        errors.add("Fila duplicada para Mes=Todos con la misma categoria y nombre");
                    }
                } else if (parsedRow.monthScope() instanceof AnnualBudgetImportMonthScope.SingleMonth singleMonth) {
                    Map<CombinationKey, ValidatedRow> monthMap = monthRows.get(singleMonth.month());
                    if (monthMap.putIfAbsent(key, validated) != null) {
                        errors.add("Fila duplicada para el mismo mes con la misma categoria y nombre");
                    }
                }
            }

            List<Integer> appliedMonths = errors.isEmpty() ? parsedRow.monthScope().months() : List.of();
            rowResponses.add(new AnnualBudgetImportRowResponse(parsedRow.rowNumber(), errors.isEmpty(), appliedMonths, errors));
            if (!errors.isEmpty()) {
                hasErrors = true;
            }
        }

        if (year == null) {
            return new AnnualBudgetImportResponse(null, 0, 0, rowResponses);
        }

        if (hasErrors) {
            return new AnnualBudgetImportResponse(year, 0, 0, rowResponses);
        }

        for (int month = 1; month <= 12; month++) {
            if (budgetRepository.findByAccountIdAndYearAndMonth(command.accountId(), year, month).isPresent()) {
                throw new BusinessRuleViolationException("ANNUAL_BUDGET_MONTH_ALREADY_EXISTS", "At least one budget month already exists for the requested year.");
            }
        }

        String finalBudgetName = budgetName;
        int createdSubBudgetsCount = 0;
        for (int month = 1; month <= 12; month++) {
            Budget budget = budgetRepository.save(Budget.create(command.accountId(), year, month, finalBudgetName).update(finalBudgetName, BudgetStatus.ACTIVE));
            Map<CombinationKey, ValidatedRow> finalRows = new HashMap<>(baseRows);
            finalRows.putAll(monthRows.get(month));
            for (ValidatedRow row : finalRows.values()) {
                subBudgetRepository.save(SubBudget.createManual(
                        command.accountId(),
                        budget.id(),
                        row.categoryId(),
                        row.subBudgetName(),
                        row.plannedAmount()
                ));
                createdSubBudgetsCount++;
            }
        }

        return new AnnualBudgetImportResponse(year, 12, createdSubBudgetsCount, rowResponses);
    }

    private Long resolveCategory(Long accountId, String categoryName, List<String> errors) {
        if (categoryName == null || categoryName.isBlank()) {
            errors.add("La categoria es requerida");
            return null;
        }
        Optional<CategoryValidationView> found = catalogValidationPort.findCategoryForValidation(accountId, normalize(categoryName));
        if (found.isEmpty()) {
            errors.add("Categoria no encontrada o inactiva");
            return null;
        }
        CategoryValidationView category = found.get();
        if (category.status() != CatalogStatus.ACTIVE) {
            errors.add("Categoria no encontrada o inactiva");
            return null;
        }
        if (category.type() != CategoryType.EXPENSE) {
            errors.add("Categoria debe ser de tipo EXPENSE");
            return null;
        }
        return category.id();
    }

    private void validateFile(ImportAnnualBudgetCommand command) {
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

    private record CombinationKey(Long categoryId, String normalizedSubBudgetName) {
    }

    private record ValidatedRow(
            int rowNumber,
            String subBudgetName,
            Long categoryId,
            Money plannedAmount,
            AnnualBudgetImportMonthScope monthScope
    ) {
    }
}

