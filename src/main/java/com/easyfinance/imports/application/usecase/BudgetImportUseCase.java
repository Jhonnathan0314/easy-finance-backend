package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
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
import com.easyfinance.imports.application.port.in.PreviewAnnualBudgetImportPort;
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
import com.easyfinance.shared.domain.DomainException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BudgetImportUseCase implements GenerateAnnualBudgetImportTemplatePort, ImportAnnualBudgetPort, PreviewAnnualBudgetImportPort {

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
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final AccountParticipantRepositoryPort accountParticipantRepository;
    private final ParticipantLookupPort participantLookupPort;
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
            AssignedParticipantValidator assignedParticipantValidator,
            AccountParticipantRepositoryPort accountParticipantRepository,
            ParticipantLookupPort participantLookupPort,
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
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.accountParticipantRepository = accountParticipantRepository;
        this.participantLookupPort = participantLookupPort;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional(readOnly = true)
    public AnnualBudgetImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentUser().participantId());
        List<String> categoryNames = categoryRepository.findActiveExpenseByAccountId(accountId).stream()
                .map(category -> category.name())
                .toList();
        byte[] content = templateGeneratorPort.generate(new AnnualBudgetImportTemplateData(categoryNames, activeParticipantLabels(accountId)));
        return new AnnualBudgetImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public AnnualBudgetImportResponse importAnnualBudget(ImportAnnualBudgetCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser().participantId());
        ValidatedAnnualBudgetImport validatedImport = validateImport(command, access);

        if (validatedImport.year() == null) {
            return new AnnualBudgetImportResponse(null, 0, 0, validatedImport.rows());
        }
        if (hasExistingBudgetInYear(command.accountId(), validatedImport.year())) {
            throw new BusinessRuleViolationException("ANNUAL_BUDGET_MONTH_ALREADY_EXISTS", "At least one budget month already exists for the requested year.");
        }
        if (validatedImport.hasErrors()) {
            return new AnnualBudgetImportResponse(validatedImport.year(), 0, 0, validatedImport.rows());
        }

        int createdSubBudgetsCount = 0;
        for (int month = 1; month <= 12; month++) {
            Budget budget = budgetRepository.save(Budget.create(command.accountId(), validatedImport.year(), month, validatedImport.budgetName())
                    .update(validatedImport.budgetName(), BudgetStatus.ACTIVE));
            Map<CombinationKey, ValidatedRow> finalRows = new HashMap<>(validatedImport.baseRows());
            finalRows.putAll(validatedImport.monthRows().get(month));
            for (ValidatedRow row : finalRows.values()) {
                subBudgetRepository.save(SubBudget.createManual(
                        command.accountId(),
                        budget.id(),
                        row.categoryId(),
                        row.participantId(),
                        row.subBudgetName(),
                        row.plannedAmount()
                ));
                createdSubBudgetsCount++;
            }
        }

        return new AnnualBudgetImportResponse(validatedImport.year(), 12, createdSubBudgetsCount, validatedImport.rows());
    }

    @Override
    @Transactional(readOnly = true)
    public AnnualBudgetImportResponse previewAnnualBudget(ImportAnnualBudgetCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentUser().participantId());
        ValidatedAnnualBudgetImport validatedImport = validateImport(command, access);
        if (validatedImport.year() != null && !validatedImport.hasErrors() && hasExistingBudgetInYear(command.accountId(), validatedImport.year())) {
            return new AnnualBudgetImportResponse(
                    validatedImport.year(),
                    0,
                    0,
                    validatedImport.rows().stream()
                            .map(row -> withAdditionalError(row, "ANNUAL_BUDGET_MONTH_ALREADY_EXISTS"))
                            .toList()
            );
        }
        return new AnnualBudgetImportResponse(validatedImport.year(), 0, 0, validatedImport.rows());
    }

    private ValidatedAnnualBudgetImport validateImport(ImportAnnualBudgetCommand command, AccountAccess access) {
        validateFile(command);

        List<AnnualBudgetImportParsedRow> parsedRows = parserPort.parse(command);
        List<AnnualBudgetImportRowResponse> rowResponses = new ArrayList<>();
        if (parsedRows.isEmpty()) {
            return ValidatedAnnualBudgetImport.empty();
        }
        ParticipantCatalog participantCatalog = participantCatalog(command.accountId());

        Integer year = null;
        String budgetName = null;
        Map<CombinationKey, ValidatedRow> baseRows = new HashMap<>();
        Map<Integer, Map<CombinationKey, ValidatedRow>> monthRows = emptyMonthRows();

        for (AnnualBudgetImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            Integer rowYear = parsedRow.year();
            if (rowYear != null) {
                if (year == null) {
                    year = rowYear;
                } else if (!Objects.equals(year, rowYear)) {
                    errors.add("Todas las filas deben tener el mismo ano");
                }
            }

            if (parsedRow.budgetName() != null && !parsedRow.budgetName().isBlank()) {
                String current = parsedRow.budgetName().trim();
                if (budgetName == null) {
                    budgetName = current;
                } else if (!budgetName.equals(current)) {
                    errors.add("NombrePresupuesto debe ser consistente para todo el ano");
                }
            }

            Long categoryId = resolveCategory(command.accountId(), parsedRow.categoryName(), errors);
            ParticipantResolution participant = resolveParticipant(access, participantCatalog, parsedRow.participantLabel(), errors);
            String normalizedSubBudgetName = normalize(parsedRow.subBudgetName());
            if (parsedRow.subBudgetName() != null && parsedRow.subBudgetName().length() > 150) {
                errors.add("NombreSubpresupuesto supera el maximo permitido");
            }
            if (errors.isEmpty()) {
                CombinationKey key = new CombinationKey(categoryId, normalizedSubBudgetName, participant.participantId());
                ValidatedRow validated = new ValidatedRow(
                        parsedRow.rowNumber(),
                        parsedRow.subBudgetName().trim(),
                        categoryId,
                        participant.participantId(),
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
            rowResponses.add(new AnnualBudgetImportRowResponse(
                    parsedRow.rowNumber(),
                    parsedRow.year(),
                    monthLabel(parsedRow.monthScope()),
                    parsedRow.budgetName(),
                    parsedRow.categoryName(),
                    categoryId,
                    parsedRow.subBudgetName(),
                    parsedRow.plannedAmount(),
                    participant == null ? parsedRow.participantLabel() : participant.label(),
                    participant == null ? null : participant.participantId(),
                    errors.isEmpty(),
                    appliedMonths,
                    errors
            ));
        }

        return new ValidatedAnnualBudgetImport(year, budgetName, rowResponses, baseRows, monthRows);
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

    private boolean hasExistingBudgetInYear(Long accountId, Integer year) {
        for (int month = 1; month <= 12; month++) {
            if (budgetRepository.findByAccountIdAndYearAndMonth(accountId, year, month).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private Map<Integer, Map<CombinationKey, ValidatedRow>> emptyMonthRows() {
        Map<Integer, Map<CombinationKey, ValidatedRow>> monthRows = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthRows.put(month, new HashMap<>());
        }
        return monthRows;
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

    private static AnnualBudgetImportRowResponse withAdditionalError(AnnualBudgetImportRowResponse row, String error) {
        List<String> errors = new ArrayList<>(row.errors());
        errors.add(error);
        return new AnnualBudgetImportRowResponse(
                row.rowNumber(),
                row.year(),
                row.month(),
                row.budgetName(),
                row.categoryName(),
                row.categoryId(),
                row.subBudgetName(),
                row.plannedAmount(),
                row.participantLabel(),
                row.participantId(),
                false,
                List.of(),
                errors
        );
    }

    private static String monthLabel(AnnualBudgetImportMonthScope monthScope) {
        if (monthScope instanceof AnnualBudgetImportMonthScope.SingleMonth singleMonth) {
            return String.valueOf(singleMonth.month());
        }
        return "Todos";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> activeParticipantLabels(Long accountId) {
        return participantCatalog(accountId).byId().values()
                .stream()
                .map(ParticipantCandidate::label)
                .sorted()
                .toList();
    }

    private ParticipantCatalog participantCatalog(Long accountId) {
        var activeMemberships = accountParticipantRepository.findByAccountId(accountId)
                .stream()
                .filter(membership -> membership.status() == AccountParticipantStatus.ACTIVE)
                .toList();
        Map<Long, ParticipantInfo> participants = participantLookupPort.findByParticipantIds(
                activeMemberships.stream().map(membership -> membership.participantId()).toList()
        );
        Map<Long, ParticipantCandidate> byId = activeMemberships.stream()
                .map(membership -> participants.get(membership.participantId()))
                .filter(Objects::nonNull)
                .filter(ParticipantInfo::active)
                .map(info -> new ParticipantCandidate(info.participantId(), participantLabel(info), info.displayName(), info.email()))
                .collect(Collectors.toMap(ParticipantCandidate::participantId, Function.identity(), (first, second) -> first));
        Map<String, List<ParticipantCandidate>> aliases = new HashMap<>();
        for (ParticipantCandidate candidate : byId.values()) {
            addAlias(aliases, candidate.label(), candidate);
            addAlias(aliases, candidate.displayName(), candidate);
            addAlias(aliases, candidate.email(), candidate);
        }
        return new ParticipantCatalog(byId, aliases);
    }

    private ParticipantResolution resolveParticipant(
            AccountAccess access,
            ParticipantCatalog participantCatalog,
            String participantLabel,
            List<String> errors
    ) {
        if (participantLabel == null || participantLabel.isBlank()) {
            return new ParticipantResolution(null, null);
        }
        List<ParticipantCandidate> candidates = participantCatalog.aliases().get(normalize(participantLabel));
        if (candidates == null || candidates.isEmpty()) {
            errors.add("Participante no encontrado o inactivo");
            return null;
        }
        if (candidates.size() > 1) {
            errors.add("Participante ambiguo");
            return null;
        }
        try {
            Long resolvedParticipantId = assignedParticipantValidator.resolveNullableAssignedParticipantId(access, candidates.getFirst().participantId());
            ParticipantCandidate candidate = participantCatalog.byId().get(resolvedParticipantId);
            return new ParticipantResolution(resolvedParticipantId, candidate == null ? participantLabel : candidate.label());
        } catch (DomainException ex) {
            errors.add(participantErrorMessage(ex));
            return null;
        }
    }

    private static void addAlias(Map<String, List<ParticipantCandidate>> aliases, String alias, ParticipantCandidate candidate) {
        String normalized = normalize(alias);
        if (normalized.isBlank()) {
            return;
        }
        aliases.computeIfAbsent(normalized, ignored -> new ArrayList<>());
        if (aliases.get(normalized).stream().noneMatch(existing -> existing.participantId().equals(candidate.participantId()))) {
            aliases.get(normalized).add(candidate);
        }
    }

    private static String participantLabel(ParticipantInfo participant) {
        if (participant.email() == null || participant.email().isBlank()) {
            return participant.displayName();
        }
        return participant.displayName() + " <" + participant.email() + ">";
    }

    private static String participantErrorMessage(DomainException ex) {
        return switch (ex.code()) {
            case "ASSIGNED_PARTICIPANT_NOT_ALLOWED" -> "Participante no permitido para el usuario actual";
            case "ASSIGNED_PARTICIPANT_NOT_FOUND", "ASSIGNED_PARTICIPANT_NOT_ACTIVE" -> "Participante no encontrado o inactivo";
            default -> "Participante invalido";
        };
    }

    private record CombinationKey(Long categoryId, String normalizedSubBudgetName, Long participantId) {
    }

    private record ValidatedRow(
            int rowNumber,
            String subBudgetName,
            Long categoryId,
            Long participantId,
            Money plannedAmount,
            AnnualBudgetImportMonthScope monthScope
    ) {
    }

    private record ParticipantCandidate(
            Long participantId,
            String label,
            String displayName,
            String email
    ) {
    }

    private record ParticipantCatalog(
            Map<Long, ParticipantCandidate> byId,
            Map<String, List<ParticipantCandidate>> aliases
    ) {
    }

    private record ParticipantResolution(
            Long participantId,
            String label
    ) {
    }

    private record ValidatedAnnualBudgetImport(
            Integer year,
            String budgetName,
            List<AnnualBudgetImportRowResponse> rows,
            Map<CombinationKey, ValidatedRow> baseRows,
            Map<Integer, Map<CombinationKey, ValidatedRow>> monthRows
    ) {
        static ValidatedAnnualBudgetImport empty() {
            return new ValidatedAnnualBudgetImport(null, null, List.of(), Map.of(), Map.of());
        }

        boolean hasErrors() {
            return rows.stream().anyMatch(row -> !row.valid());
        }
    }
}
