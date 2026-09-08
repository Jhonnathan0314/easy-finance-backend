package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.debts.application.command.CreateManualDebtCommand;
import com.easyfinance.debts.application.port.in.CreateManualDebtPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.imports.application.command.ImportDebtCommand;
import com.easyfinance.imports.application.port.in.GenerateDebtImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportDebtPort;
import com.easyfinance.imports.application.port.in.PreviewDebtImportPort;
import com.easyfinance.imports.application.port.out.DebtImportParserPort;
import com.easyfinance.imports.application.port.out.DebtImportTemplateGeneratorPort;
import com.easyfinance.imports.application.response.DebtImportResponse;
import com.easyfinance.imports.application.response.DebtImportRowResponse;
import com.easyfinance.imports.application.response.DebtImportTemplateResponse;
import com.easyfinance.imports.application.template.DebtImportTemplateData;
import com.easyfinance.imports.application.validation.DebtImportParsedRow;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.DomainException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DebtImportUseCase implements GenerateDebtImportTemplatePort, ImportDebtPort, PreviewDebtImportPort {

    private static final String TEMPLATE_FILENAME = "easy-finance-debt-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final AccountParticipantRepositoryPort accountParticipantRepository;
    private final ParticipantLookupPort participantLookupPort;
    private final DebtImportParserPort parserPort;
    private final DebtImportTemplateGeneratorPort templateGeneratorPort;
    private final CreateManualDebtPort createManualDebtPort;
    private final DebtRepositoryPort debtRepository;
    private final long maxFileSizeBytes;

    @Autowired
    public DebtImportUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            AssignedParticipantValidator assignedParticipantValidator,
            AccountParticipantRepositoryPort accountParticipantRepository,
            ParticipantLookupPort participantLookupPort,
            DebtImportParserPort parserPort,
            DebtImportTemplateGeneratorPort templateGeneratorPort,
            CreateManualDebtPort createManualDebtPort,
            DebtRepositoryPort debtRepository,
            @Value("${easy-finance.imports.debts.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.accountParticipantRepository = accountParticipantRepository;
        this.participantLookupPort = participantLookupPort;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.createManualDebtPort = createManualDebtPort;
        this.debtRepository = debtRepository;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public DebtImportUseCase(CurrentUserProvider a, AccountAuthorizationService b, AssignedParticipantValidator c, AccountParticipantRepositoryPort d, ParticipantLookupPort e, DebtImportParserPort f, DebtImportTemplateGeneratorPort g, CreateManualDebtPort h, long i) {
        this(a,b,c,d,e,f,g,h,null,i);
    }

    @Override
    @Transactional(readOnly = true)
    public DebtImportTemplateResponse generate(Long accountId) {
        accountAuthorizationService.requireActiveMember(accountId, currentUser().participantId());
        byte[] content = templateGeneratorPort.generate(new DebtImportTemplateData(activeParticipantLabels(accountId)));
        return new DebtImportTemplateResponse(TEMPLATE_FILENAME, TEMPLATE_CONTENT_TYPE, content);
    }

    @Override
    @Transactional
    public DebtImportResponse importDebts(ImportDebtCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser().participantId());
        ValidatedDebtImport validatedImport = validateImport(command, access);

        if (validatedImport.hasErrors()) {
            return new DebtImportResponse(0, validatedImport.rows());
        }

        List<DebtImportRowResponse> createdRows = new ArrayList<>();
        for (ValidatedDebtRow row : validatedImport.validRows()) {
            var created = createManualDebtPort.createManualDebt(new CreateManualDebtCommand(
                    command.accountId(),
                    row.participantId(),
                    row.name(),
                    row.description(),
                    Money.cop(row.totalAmount()),
                    row.installmentCount(),
                    row.installmentAmount() == null ? null : Money.cop(row.installmentAmount()),
                    row.startDate(),
                    row.dueDate(),
                    row.notes(),
                    row.remainingBalance() == null ? null : Money.cop(row.remainingBalance())
            ));
            if (debtRepository != null && "CANCELLED".equalsIgnoreCase(row.status())) debtRepository.findByAccountIdAndId(command.accountId(), created.id()).ifPresent(d -> debtRepository.save(d.cancel()));
            createdRows.add(new DebtImportRowResponse(
                    row.rowNumber(),
                    row.name(),
                    row.description(),
                    row.totalAmount(),
                    row.remainingBalance(),
                    row.installmentCount(),
                    row.installmentAmount(),
                    row.startDate(),
                    row.dueDate(),
                    row.participantLabel(),
                    row.participantId(),
                    row.notes(),
                    true,
                    created.id(),
                    List.of()
            ));
        }
        return new DebtImportResponse(createdRows.size(), createdRows);
    }

    @Override
    @Transactional(readOnly = true)
    public DebtImportResponse previewDebts(ImportDebtCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser().participantId());
        return new DebtImportResponse(0, validateImport(command, access).rows());
    }

    private ValidatedDebtImport validateImport(ImportDebtCommand command, AccountAccess access) {
        validateFile(command);

        List<DebtImportParsedRow> parsedRows = parserPort.parse(command, command.accountId());
        ParticipantCatalog participantCatalog = participantCatalog(command.accountId());
        List<DebtImportRowResponse> validationRows = new ArrayList<>();
        List<ValidatedDebtRow> validRows = new ArrayList<>();

        for (DebtImportParsedRow parsedRow : parsedRows) {
            List<String> errors = new ArrayList<>(parsedRow.errors());
            ParticipantResolution participant = resolveParticipant(access, participantCatalog, parsedRow.participantLabel(), errors);
            boolean valid = errors.isEmpty() && participant != null;
            if (valid) {
                validRows.add(new ValidatedDebtRow(
                        parsedRow.rowNumber(),
                        parsedRow.name(),
                        parsedRow.description(),
                        parsedRow.totalAmount(),
                        parsedRow.remainingBalance(),
                        parsedRow.installmentCount(),
                        parsedRow.installmentAmount(),
                        parsedRow.startDate(),
                        parsedRow.dueDate(),
                        participant.label(),
                        participant.participantId(),
                        parsedRow.notes(),
                        parsedRow.status()
                ));
            }
            validationRows.add(new DebtImportRowResponse(
                    parsedRow.rowNumber(),
                    parsedRow.name(),
                    parsedRow.description(),
                    parsedRow.totalAmount(),
                    parsedRow.remainingBalance(),
                    parsedRow.installmentCount(),
                    parsedRow.installmentAmount(),
                    parsedRow.startDate(),
                    parsedRow.dueDate(),
                    participant == null ? parsedRow.participantLabel() : participant.label(),
                    participant == null ? null : participant.participantId(),
                    parsedRow.notes(),
                    valid,
                    null,
                    errors
            ));
        }

        return new ValidatedDebtImport(validationRows, validRows);
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

    private void validateFile(ImportDebtCommand command) {
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

    private record ValidatedDebtRow(
            Integer rowNumber,
            String name,
            String description,
            BigDecimal totalAmount,
            BigDecimal remainingBalance,
            Integer installmentCount,
            BigDecimal installmentAmount,
            LocalDate startDate,
            LocalDate dueDate,
            String participantLabel,
            Long participantId,
            String notes
            , String status
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

    private record ValidatedDebtImport(
            List<DebtImportRowResponse> rows,
            List<ValidatedDebtRow> validRows
    ) {
        boolean hasErrors() {
            return rows.stream().anyMatch(row -> !row.valid());
        }
    }
}
