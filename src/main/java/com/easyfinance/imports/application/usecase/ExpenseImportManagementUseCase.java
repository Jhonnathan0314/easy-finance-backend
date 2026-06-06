package com.easyfinance.imports.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.port.out.AccountParticipantRepositoryPort;
import com.easyfinance.accounts.application.port.out.ParticipantLookupPort;
import com.easyfinance.accounts.application.response.ParticipantInfo;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.AccountParticipantStatus;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.debts.application.command.RegisterDebtPaymentCommand;
import com.easyfinance.debts.application.port.in.RegisterDebtPaymentPort;
import com.easyfinance.debts.application.port.out.DebtRepositoryPort;
import com.easyfinance.debts.domain.model.Debt;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.expenses.application.command.CreateDebtPaymentExpenseCommand;
import com.easyfinance.expenses.application.command.CreateImportedExpenseCommand;
import com.easyfinance.expenses.application.port.in.CreateDebtPaymentExpensePort;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseImportManagementUseCase implements PreviewExpenseImportPort, ConfirmExpenseImportPort, GetExpenseImportBatchPort, GenerateExpenseImportTemplatePort {

    private static final String TEMPLATE_FILENAME = "easy-finance-expense-import-template.xlsx";
    private static final String TEMPLATE_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DEFAULT_DEBT_PAYMENT_NOTES = "Pago registrado desde importacion de gastos";

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final AccountParticipantRepositoryPort accountParticipantRepository;
    private final ParticipantLookupPort participantLookupPort;
    private final CatalogValidationPort catalogValidationPort;
    private final CategoryRepositoryPort categoryRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;
    private final DebtRepositoryPort debtRepository;
    private final ExpenseImportParserPort parserPort;
    private final ExpenseImportTemplateGeneratorPort templateGeneratorPort;
    private final ExpenseImportRepositoryPort importRepository;
    private final CreateImportedExpensePort createImportedExpensePort;
    private final CreateDebtPaymentExpensePort createDebtPaymentExpensePort;
    private final RegisterDebtPaymentPort registerDebtPaymentPort;
    private final long maxFileSizeBytes;

    public ExpenseImportManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            AssignedParticipantValidator assignedParticipantValidator,
            AccountParticipantRepositoryPort accountParticipantRepository,
            ParticipantLookupPort participantLookupPort,
            CatalogValidationPort catalogValidationPort,
            CategoryRepositoryPort categoryRepository,
            PaymentMethodRepositoryPort paymentMethodRepository,
            DebtRepositoryPort debtRepository,
            ExpenseImportParserPort parserPort,
            ExpenseImportTemplateGeneratorPort templateGeneratorPort,
            ExpenseImportRepositoryPort importRepository,
            CreateImportedExpensePort createImportedExpensePort,
            CreateDebtPaymentExpensePort createDebtPaymentExpensePort,
            RegisterDebtPaymentPort registerDebtPaymentPort,
            @Value("${easy-finance.imports.expenses.max-file-size-bytes:5242880}") long maxFileSizeBytes
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.accountParticipantRepository = accountParticipantRepository;
        this.participantLookupPort = participantLookupPort;
        this.catalogValidationPort = catalogValidationPort;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.debtRepository = debtRepository;
        this.parserPort = parserPort;
        this.templateGeneratorPort = templateGeneratorPort;
        this.importRepository = importRepository;
        this.createImportedExpensePort = createImportedExpensePort;
        this.createDebtPaymentExpensePort = createDebtPaymentExpensePort;
        this.registerDebtPaymentPort = registerDebtPaymentPort;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    @Transactional
    public ExpenseImportBatchResponse preview(PreviewExpenseImportCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        validateFile(command);
        List<ExpenseImportRow> parsedRows = parserPort.parse(command, command.accountId());
        ParticipantCatalog participantCatalog = participantCatalog(command.accountId());
        List<ExpenseImportRow> validatedRows = parsedRows.stream()
                .map(this::validateCatalogs)
                .map(this::validateDebtPayment)
                .map(row -> validateParticipant(row, access, participantCatalog))
                .toList();
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
                Long rowParticipantId = rowParticipantId(row, batch);
                if (row.appliesDebtPayment()) {
                    var debtPayment = registerDebtPaymentPort.registerDebtPayment(new RegisterDebtPaymentCommand(
                            accountId,
                            rowParticipantId,
                            row.debtId(),
                            row.debtPaymentType(),
                            row.amount(),
                            row.expenseDate(),
                            debtPaymentNotes(row),
                            false,
                            null,
                            null,
                            null
                    ));
                    var expense = createDebtPaymentExpensePort.createDebtPaymentExpense(new CreateDebtPaymentExpenseCommand(
                            accountId,
                            row.categoryId(),
                            row.paymentMethodId(),
                            rowParticipantId,
                            debtPayment.payment().id(),
                            row.description(),
                            row.amount(),
                            row.expenseDate()
                    ));
                    importRepository.updateCreatedExpenseId(accountId, row.id(), expense.id());
                    importRepository.updateCreatedDebtPaymentId(accountId, row.id(), debtPayment.payment().id());
                } else {
                    var expense = createImportedExpensePort.createImportedExpense(new CreateImportedExpenseCommand(
                            accountId,
                            row.categoryId(),
                            row.paymentMethodId(),
                            rowParticipantId,
                            row.description(),
                            row.amount(),
                            row.expenseDate(),
                            row.paymentState()
                    ));
                    importRepository.updateCreatedExpenseId(accountId, row.id(), expense.id());
                }
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
        var debtOptions = debtRepository.findActiveByAccountId(accountId)
                .stream()
                .map(debt -> new ExpenseImportTemplateData.DebtOption(debt.id(), debtLabel(debt)))
                .toList();
        byte[] content = templateGeneratorPort.generate(new ExpenseImportTemplateData(categoryNames, paymentMethodNames, debtOptions, activeParticipantLabels(accountId)));
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

    private ExpenseImportRow validateDebtPayment(ExpenseImportRow row) {
        if (!row.valid() || !row.appliesDebtPayment()) {
            return row;
        }
        List<ImportRowError> errors = new ArrayList<>(row.errors());
        Long debtId = row.debtId();

        if (debtId == null) {
            errors.add(new ImportRowError("Deuda", "IMPORT_DEBT_NOT_FOUND", "Debt was not found."));
            return copyWithDebtValidation(row, debtId, errors);
        }

        var debt = debtRepository.findByAccountIdAndId(row.accountId(), debtId);
        if (debt.isEmpty()) {
            errors.add(new ImportRowError("Deuda", "IMPORT_DEBT_NOT_FOUND", "Debt was not found."));
        } else {
            Debt value = debt.get();
            if (value.state() != DebtState.ACTIVE) {
                errors.add(new ImportRowError("Deuda", "IMPORT_DEBT_NOT_ACTIVE", "Debt is not active."));
            }
            if (row.amount() != null && row.amount().amount().compareTo(value.remainingBalance().amount()) > 0) {
                errors.add(new ImportRowError("Monto", "IMPORT_DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE", "Debt payment exceeds remaining balance."));
            }
        }

        return copyWithDebtValidation(row, debtId, errors);
    }

    private ExpenseImportRow validateParticipant(ExpenseImportRow row, AccountAccess access, ParticipantCatalog participantCatalog) {
        List<ImportRowError> errors = new ArrayList<>(row.errors());
        ParticipantResolution participant = resolveParticipant(access, participantCatalog, row.participantLabel(), errors);
        return copyWithParticipantValidation(
                row,
                participant == null ? row.participantLabel() : participant.label(),
                participant == null ? null : participant.participantId(),
                errors
        );
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
            List<ImportRowError> errors
    ) {
        Long requestedParticipantId = null;
        if (participantLabel != null && !participantLabel.isBlank()) {
            List<ParticipantCandidate> candidates = participantCatalog.aliases().get(normalize(participantLabel));
            if (candidates == null || candidates.isEmpty()) {
                errors.add(new ImportRowError("Participante", "IMPORT_PARTICIPANT_NOT_FOUND", "Participant was not found or is inactive."));
                return null;
            }
            if (candidates.size() > 1) {
                errors.add(new ImportRowError("Participante", "IMPORT_PARTICIPANT_AMBIGUOUS", "Participant label is ambiguous."));
                return null;
            }
            requestedParticipantId = candidates.getFirst().participantId();
        }
        try {
            Long resolvedParticipantId = assignedParticipantValidator.resolveAssignedParticipantId(access, requestedParticipantId);
            ParticipantCandidate candidate = participantCatalog.byId().get(resolvedParticipantId);
            return new ParticipantResolution(resolvedParticipantId, candidate == null ? participantLabel : candidate.label());
        } catch (DomainException ex) {
            errors.add(new ImportRowError("Participante", participantErrorCode(ex), participantErrorMessage(ex)));
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

    private static String participantErrorCode(DomainException ex) {
        return switch (ex.code()) {
            case "ASSIGNED_PARTICIPANT_NOT_ALLOWED" -> "IMPORT_PARTICIPANT_NOT_ALLOWED";
            case "ASSIGNED_PARTICIPANT_NOT_FOUND", "ASSIGNED_PARTICIPANT_NOT_ACTIVE" -> "IMPORT_PARTICIPANT_NOT_FOUND";
            default -> "IMPORT_PARTICIPANT_INVALID";
        };
    }

    private static String participantErrorMessage(DomainException ex) {
        return switch (ex.code()) {
            case "ASSIGNED_PARTICIPANT_NOT_ALLOWED" -> "Current user cannot assign this participant.";
            case "ASSIGNED_PARTICIPANT_NOT_FOUND", "ASSIGNED_PARTICIPANT_NOT_ACTIVE" -> "Participant was not found or is inactive.";
            default -> "Participant is invalid.";
        };
    }

    private static ExpenseImportRow copyWithValidation(ExpenseImportRow row, Long categoryId, Long paymentMethodId, List<ImportRowError> errors) {
        return new ExpenseImportRow(row.id(), row.accountId(), row.batchId(), row.rowNumber(), row.expenseDate(), row.description(), row.amount(), row.categoryName(), categoryId, row.paymentMethodName(), paymentMethodId, row.paymentState(), row.participantLabel(), row.participantId(), row.appliesDebtPayment(), row.debtId(), row.debtLabel(), row.debtPaymentType(), row.debtPaymentNotes(), errors.isEmpty(), errors, row.createdExpenseId(), row.createdDebtPaymentId(), row.createdAt(), row.updatedAt());
    }

    private static ExpenseImportRow copyWithDebtValidation(ExpenseImportRow row, Long debtId, List<ImportRowError> errors) {
        return new ExpenseImportRow(row.id(), row.accountId(), row.batchId(), row.rowNumber(), row.expenseDate(), row.description(), row.amount(), row.categoryName(), row.categoryId(), row.paymentMethodName(), row.paymentMethodId(), row.paymentState(), row.participantLabel(), row.participantId(), row.appliesDebtPayment(), debtId, row.debtLabel(), row.debtPaymentType(), row.debtPaymentNotes(), errors.isEmpty(), errors, row.createdExpenseId(), row.createdDebtPaymentId(), row.createdAt(), row.updatedAt());
    }

    private static ExpenseImportRow copyWithParticipantValidation(ExpenseImportRow row, String participantLabel, Long participantId, List<ImportRowError> errors) {
        return new ExpenseImportRow(row.id(), row.accountId(), row.batchId(), row.rowNumber(), row.expenseDate(), row.description(), row.amount(), row.categoryName(), row.categoryId(), row.paymentMethodName(), row.paymentMethodId(), row.paymentState(), participantLabel, participantId, row.appliesDebtPayment(), row.debtId(), row.debtLabel(), row.debtPaymentType(), row.debtPaymentNotes(), errors.isEmpty(), errors, row.createdExpenseId(), row.createdDebtPaymentId(), row.createdAt(), row.updatedAt());
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

    private static String debtLabel(Debt debt) {
        return "%s | Saldo: %s | Inicio: %s | %s".formatted(
                debt.name(),
                debt.remainingBalance().amount().setScale(2).toPlainString(),
                debt.startDate(),
                debt.sourceType().name()
        );
    }

    private static String debtPaymentNotes(ExpenseImportRow row) {
        if (row.debtPaymentNotes() == null || row.debtPaymentNotes().isBlank()) {
            return DEFAULT_DEBT_PAYMENT_NOTES;
        }
        return row.debtPaymentNotes().trim();
    }

    private static Long rowParticipantId(ExpenseImportRow row, ExpenseImportBatch batch) {
        return row.participantId() == null ? batch.participantId() : row.participantId();
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
                row.participantLabel(),
                row.participantId(),
                row.appliesDebtPayment(),
                row.debtId(),
                row.debtLabel(),
                row.debtPaymentType() == null ? null : row.debtPaymentType().name(),
                row.debtPaymentNotes(),
                row.valid(),
                row.errors().stream().map(error -> new ImportRowErrorResponse(error.column(), error.code(), error.message())).toList(),
                row.createdExpenseId(),
                row.createdDebtPaymentId()
        );
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
}
