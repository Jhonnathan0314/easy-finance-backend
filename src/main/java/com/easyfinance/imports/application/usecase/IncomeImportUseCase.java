package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
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
import com.easyfinance.shared.domain.DomainException;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IncomeImportUseCase implements GenerateIncomeImportTemplatePort, ImportIncomePort, PreviewIncomeImportPort {

    private static final String TEMPLATE_FILENAME = "easy-finance-income-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final AccountParticipantRepositoryPort accountParticipantRepository;
    private final ParticipantLookupPort participantLookupPort;
    private final CatalogValidationPort catalogValidationPort;
    private final CategoryRepositoryPort categoryRepository;
    private final IncomeImportParserPort parserPort;
    private final IncomeImportTemplateGeneratorPort templateGeneratorPort;
    private final CreateIncomePort createIncomePort;
    private final long maxFileSizeBytes;

    public IncomeImportUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            AssignedParticipantValidator assignedParticipantValidator,
            AccountParticipantRepositoryPort accountParticipantRepository,
            ParticipantLookupPort participantLookupPort,
            CatalogValidationPort catalogValidationPort,
            CategoryRepositoryPort categoryRepository,
            IncomeImportParserPort parserPort,
            IncomeImportTemplateGeneratorPort templateGeneratorPort,
            CreateIncomePort createIncomePort,
            @Value("${easy-finance.imports.incomes.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.accountParticipantRepository = accountParticipantRepository;
        this.participantLookupPort = participantLookupPort;
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
        byte[] content = templateGeneratorPort.generate(new IncomeImportTemplateData(categoryNames, activeParticipantLabels(accountId)));
        return new IncomeImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public IncomeImportResponse importIncomes(ImportIncomeCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser().participantId());
        ValidatedIncomeImport validatedImport = validateImport(command, access);

        if (validatedImport.hasErrors()) {
            return new IncomeImportResponse(0, validatedImport.rows());
        }

        List<IncomeImportRowResponse> createdRows = new ArrayList<>();
        for (ValidatedIncomeRow row : validatedImport.validRows()) {
            var created = createIncomePort.createIncome(new CreateIncomeCommand(
                    command.accountId(),
                    row.participantId(),
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
                    row.participantLabel(),
                    row.participantId(),
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
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser().participantId());
        return new IncomeImportResponse(0, validateImport(command, access).rows());
    }

    private ValidatedIncomeImport validateImport(ImportIncomeCommand command, AccountAccess access) {
        validateFile(command);

        List<IncomeImportParsedRow> parsedRows = parserPort.parse(command, command.accountId());
        ParticipantCatalog participantCatalog = participantCatalog(command.accountId());
        List<IncomeImportRowResponse> validationRows = new ArrayList<>();
        List<ValidatedIncomeRow> validRows = new ArrayList<>();

        for (IncomeImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            Long categoryId = resolveCategory(command.accountId(), parsedRow.categoryName(), errors);
            ParticipantResolution participant = resolveParticipant(access, participantCatalog, parsedRow.participantLabel(), errors);
            boolean valid = errors.isEmpty() && categoryId != null && participant != null;
            if (valid) {
                validRows.add(new ValidatedIncomeRow(
                        parsedRow.rowNumber(),
                        parsedRow.incomeDate(),
                        parsedRow.description(),
                        parsedRow.categoryName(),
                        participant.label(),
                        participant.participantId(),
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
                    participant == null ? parsedRow.participantLabel() : participant.label(),
                    participant == null ? null : participant.participantId(),
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
        Long requestedParticipantId = null;
        if (participantLabel != null && !participantLabel.isBlank()) {
            List<ParticipantCandidate> candidates = participantCatalog.aliases().get(normalize(participantLabel));
            if (candidates == null || candidates.isEmpty()) {
                errors.add("Participante no encontrado o inactivo");
                return null;
            }
            if (candidates.size() > 1) {
                errors.add("Participante ambiguo");
                return null;
            }
            requestedParticipantId = candidates.getFirst().participantId();
        }
        try {
            Long resolvedParticipantId = assignedParticipantValidator.resolveAssignedParticipantId(access, requestedParticipantId);
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
            String participantLabel,
            Long participantId,
            BigDecimal amount,
            Long categoryId
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

    private record ValidatedIncomeImport(
            List<IncomeImportRowResponse> rows,
            List<ValidatedIncomeRow> validRows
    ) {
        boolean hasErrors() {
            return rows.stream().anyMatch(row -> !row.valid());
        }
    }
}

