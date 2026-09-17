package com.easyfinance.budgets.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.ApplySubBudgetForwardCommand;
import com.easyfinance.budgets.application.command.CreateAnnualBudgetCommand;
import com.easyfinance.budgets.application.command.CreateAnnualSubBudgetBaseCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.budgets.application.command.CreateSubBudgetCommand;
import com.easyfinance.budgets.application.command.DuplicateBudgetCommand;
import com.easyfinance.budgets.application.command.PreviewSubBudgetForwardCommand;
import com.easyfinance.budgets.application.command.SubBudgetForwardAction;
import com.easyfinance.budgets.application.command.UpdateSubBudgetCommand;
import com.easyfinance.budgets.application.command.UpsertBudgetCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.budgets.application.port.in.CreateAnnualBudgetPort;
import com.easyfinance.budgets.application.port.in.CreateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DeactivateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DuplicateBudgetPort;
import com.easyfinance.budgets.application.port.in.GetBudgetPort;
import com.easyfinance.budgets.application.port.in.ListBudgetsPort;
import com.easyfinance.budgets.application.port.in.SubBudgetForwardPort;
import com.easyfinance.budgets.application.port.in.UpdateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.UpsertBudgetPort;
import com.easyfinance.budgets.application.port.out.BudgetImpactRepositoryPort;
import com.easyfinance.budgets.application.port.out.BudgetExpenseExecutionQueryPort;
import com.easyfinance.budgets.application.port.out.BudgetRepositoryPort;
import com.easyfinance.budgets.application.port.out.SubBudgetRepositoryPort;
import com.easyfinance.budgets.application.query.ListBudgetsQuery;
import com.easyfinance.budgets.application.response.BudgetDetailResponse;
import com.easyfinance.budgets.application.response.BudgetImpactResponse;
import com.easyfinance.budgets.application.response.BudgetResponse;
import com.easyfinance.budgets.application.response.AnnualBudgetResponse;
import com.easyfinance.budgets.application.response.PageResponse;
import com.easyfinance.budgets.application.response.SubBudgetForwardApplyResponse;
import com.easyfinance.budgets.application.response.SubBudgetForwardMonthPlan;
import com.easyfinance.budgets.application.response.SubBudgetForwardMonthResult;
import com.easyfinance.budgets.application.response.SubBudgetForwardMonthStatus;
import com.easyfinance.budgets.application.response.SubBudgetForwardPlanResponse;
import com.easyfinance.budgets.application.response.SubBudgetResponse;
import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.BudgetImpact;
import com.easyfinance.budgets.domain.model.BudgetStatus;
import com.easyfinance.budgets.domain.model.SubBudgetSourceType;
import com.easyfinance.budgets.domain.model.SubBudgetStatus;
import com.easyfinance.budgets.domain.model.SubBudget;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.Money;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetManagementUseCase implements
        UpsertBudgetPort,
        CreateAnnualBudgetPort,
        GetBudgetPort,
        ListBudgetsPort,
        CreateSubBudgetPort,
        UpdateSubBudgetPort,
        DeactivateSubBudgetPort,
        DuplicateBudgetPort,
        BudgetDebtImpactPort,
        SubBudgetForwardPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final CatalogValidationPort catalogValidationPort;
    private final BudgetRepositoryPort budgetRepository;
    private final SubBudgetRepositoryPort subBudgetRepository;
    private final BudgetImpactRepositoryPort impactRepository;
    private final BudgetExpenseExecutionQueryPort expenseExecutionQueryPort;

    public BudgetManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            AssignedParticipantValidator assignedParticipantValidator,
            CatalogValidationPort catalogValidationPort,
            BudgetRepositoryPort budgetRepository,
            SubBudgetRepositoryPort subBudgetRepository,
            BudgetImpactRepositoryPort impactRepository,
            BudgetExpenseExecutionQueryPort expenseExecutionQueryPort
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.catalogValidationPort = catalogValidationPort;
        this.budgetRepository = budgetRepository;
        this.subBudgetRepository = subBudgetRepository;
        this.impactRepository = impactRepository;
        this.expenseExecutionQueryPort = expenseExecutionQueryPort;
    }

    @Override
    @Transactional
    public BudgetResponse upsertBudget(UpsertBudgetCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Budget probe = Budget.create(command.accountId(), command.year(), command.month(), command.name());
        Budget budget = budgetRepository.findByAccountIdAndYearAndMonth(command.accountId(), command.year(), command.month())
                .map(existing -> existing.update(command.name(), command.status()))
                .orElse(probe.update(command.name(), command.status()));
        return toBudgetResponse(budgetRepository.save(budget));
    }

    @Override
    @Transactional
    public AnnualBudgetResponse createAnnualBudget(CreateAnnualBudgetCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        for (int month = 1; month <= 12; month++) {
            if (budgetRepository.findByAccountIdAndYearAndMonth(command.accountId(), command.year(), month).isPresent()) {
                throw new BusinessRuleViolationException("ANNUAL_BUDGET_MONTH_ALREADY_EXISTS", "At least one budget month already exists for the requested year.");
            }
        }

        BudgetStatus status = command.status() == null ? com.easyfinance.budgets.domain.model.BudgetStatus.ACTIVE : command.status();
        List<CreateAnnualSubBudgetBaseCommand> baseSubBudgets = command.subBudgets() == null ? List.of() : command.subBudgets();
        baseSubBudgets.forEach(baseSubBudget -> {
            if (baseSubBudget.plannedAmount() == null || baseSubBudget.plannedAmount().amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException("PLANNED_AMOUNT_INVALID", "Planned amount must be greater than zero.");
            }
            validateActiveCategory(command.accountId(), baseSubBudget.categoryId());
        });

        try {
            List<BudgetResponse> createdBudgets = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                Budget createdBudget = budgetRepository.save(Budget.create(command.accountId(), command.year(), month, command.name()).update(command.name(), status));
                for (CreateAnnualSubBudgetBaseCommand baseSubBudget : baseSubBudgets) {
                    subBudgetRepository.save(SubBudget.createManual(
                            command.accountId(),
                            createdBudget.id(),
                            baseSubBudget.categoryId(),
                            baseSubBudget.name(),
                            baseSubBudget.plannedAmount()
                    ));
                }
                createdBudgets.add(toBudgetResponse(createdBudget));
            }
            return new AnnualBudgetResponse(command.accountId(), command.year(), createdBudgets);
        } catch (BusinessRuleViolationException ex) {
            if ("BUDGET_TARGET_ALREADY_EXISTS".equals(ex.code())) {
                throw new BusinessRuleViolationException("ANNUAL_BUDGET_MONTH_ALREADY_EXISTS", "At least one budget month already exists for the requested year.", ex);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetDetailResponse getBudget(Long accountId, Integer year, Integer month) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        Budget budget = budgetRepository.findByAccountIdAndYearAndMonth(accountId, year, month)
                .orElseThrow(() -> new NotFoundException("BUDGET_NOT_FOUND", "Budget was not found."));
        return detailResponse(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BudgetResponse> listBudgets(ListBudgetsQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        PageResponse<Budget> page = budgetRepository.findAll(query);
        return new PageResponse<>(
                page.content().stream().map(this::toBudgetResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    @Override
    @Transactional
    public BudgetDetailResponse duplicateBudget(DuplicateBudgetCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Budget source = budgetRepository.findByAccountIdAndYearAndMonth(command.accountId(), command.sourceYear(), command.sourceMonth())
                .orElseThrow(() -> new NotFoundException("BUDGET_NOT_FOUND", "Budget was not found."));
        if (budgetRepository.findByAccountIdAndYearAndMonth(command.accountId(), command.targetYear(), command.targetMonth()).isPresent()) {
            throw new BusinessRuleViolationException("BUDGET_TARGET_ALREADY_EXISTS", "Target budget already exists.");
        }

        String targetName = command.name() == null || command.name().isBlank() ? source.name() : command.name();
        Budget target = budgetRepository.save(Budget.create(command.accountId(), command.targetYear(), command.targetMonth(), targetName));
        List<SubBudgetResponse> copiedSubBudgets = subBudgetRepository.findByAccountIdAndBudgetId(command.accountId(), source.id())
                .stream()
                .filter(subBudget -> subBudget.sourceType() == SubBudgetSourceType.MANUAL)
                .filter(subBudget -> subBudget.status() == SubBudgetStatus.ACTIVE)
                .map(subBudget -> SubBudget.createManual(
                        command.accountId(),
                        target.id(),
                        subBudget.categoryId(),
                        subBudget.participantId(),
                        subBudget.name(),
                        subBudget.plannedAmount()
                ))
                .map(subBudgetRepository::save)
                .map(this::toSubBudgetResponse)
                .toList();
        return new BudgetDetailResponse(toBudgetResponse(target), copiedSubBudgets, List.of());
    }

    @Override
    @Transactional
    public SubBudgetResponse createSubBudget(CreateSubBudgetCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Budget budget = findBudget(command.accountId(), command.budgetId());
        budget.ensureActive();
        validateActiveCategory(command.accountId(), command.categoryId());
        Long assignedParticipantId = assignedParticipantValidator.resolveNullableAssignedParticipantId(access, command.participantId());
        SubBudget subBudget = SubBudget.createManual(command.accountId(), command.budgetId(), command.categoryId(), assignedParticipantId, command.name(), command.plannedAmount());
        return toSubBudgetResponse(subBudgetRepository.save(subBudget));
    }

    @Override
    @Transactional
    public SubBudgetResponse updateSubBudget(UpdateSubBudgetCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Budget budget = findBudget(command.accountId(), command.budgetId());
        budget.ensureActive();
        validateActiveCategory(command.accountId(), command.categoryId());
        SubBudget subBudget = findSubBudget(command.accountId(), command.budgetId(), command.subBudgetId());
        Long assignedParticipantId = assignedParticipantValidator.resolveNullableAssignedParticipantId(access, command.participantId());
        return toSubBudgetResponse(subBudgetRepository.save(subBudget.updateManual(command.categoryId(), assignedParticipantId, command.name(), command.plannedAmount())));
    }

    @Override
    @Transactional
    public void deactivateSubBudget(Long accountId, Long budgetId, Long subBudgetId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentParticipantId());
        Budget budget = findBudget(accountId, budgetId);
        budget.ensureActive();
        SubBudget subBudget = findSubBudget(accountId, budgetId, subBudgetId);
        subBudgetRepository.save(subBudget.deactivateManual());
    }

    @Override
    @Transactional(readOnly = true)
    public SubBudgetForwardPlanResponse previewSubBudgetForward(PreviewSubBudgetForwardCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Long resolvedParticipantId = assignedParticipantValidator.resolveNullableAssignedParticipantId(access, command.participantId());
        ForwardContext context = buildForwardContext(command.accountId(), command.budgetId(), command.action(), command.subBudgetId(), command.categoryId(), resolvedParticipantId, command.name(), command.plannedAmount());
        List<SubBudgetForwardMonthPlan> months = context.entries().stream().map(entry -> toMonthPlanDto(entry, context)).toList();
        return new SubBudgetForwardPlanResponse(months);
    }

    @Override
    @Transactional
    public SubBudgetForwardApplyResponse applySubBudgetForward(ApplySubBudgetForwardCommand command) {
        AccountAccess access = accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Long resolvedParticipantId = assignedParticipantValidator.resolveNullableAssignedParticipantId(access, command.participantId());
        ForwardContext context = buildForwardContext(command.accountId(), command.budgetId(), command.action(), command.subBudgetId(), command.categoryId(), resolvedParticipantId, command.name(), command.plannedAmount());
        Set<Integer> selectedMonths = command.months() == null ? Set.of() : new HashSet<>(command.months());
        UUID groupId = resolveGroupIdForApply(context);

        List<SubBudgetForwardMonthResult> results = new ArrayList<>();
        for (ForwardMonthEntry entry : context.entries()) {
            boolean isSource = entry.month().equals(context.sourceBudget().month());
            boolean included = isSource || selectedMonths.contains(entry.month());
            results.add(included ? applyToMonth(context, entry, groupId) : new SubBudgetForwardMonthResult(entry.year(), entry.month(), "SKIPPED", "NOT_SELECTED"));
        }
        return new SubBudgetForwardApplyResponse(results);
    }

    private ForwardContext buildForwardContext(Long accountId, Long budgetId, SubBudgetForwardAction action, Long subBudgetId, Long categoryId, Long participantId, String name, Money plannedAmount) {
        Budget sourceBudget = findBudget(accountId, budgetId);
        sourceBudget.ensureActive();

        SubBudget sourceSubBudget = null;
        if (action != SubBudgetForwardAction.CREATE) {
            if (subBudgetId == null) {
                throw new BusinessRuleViolationException("SUB_BUDGET_NOT_FOUND", "Sub-budget was not found.");
            }
            sourceSubBudget = findSubBudget(accountId, budgetId, subBudgetId);
            sourceSubBudget.ensureManualEditable();
        }

        String normalizedName = name == null ? null : name.trim();
        validateProposedPayload(action, normalizedName, plannedAmount);
        if (action != SubBudgetForwardAction.DELETE) {
            validateActiveCategory(accountId, categoryId);
        }

        UUID groupId = sourceSubBudget == null ? null : sourceSubBudget.recurringGroupId();
        Long matchCategoryId = sourceSubBudget != null ? sourceSubBudget.categoryId() : categoryId;
        Long matchParticipantId = sourceSubBudget != null ? sourceSubBudget.participantId() : participantId;
        String matchName = sourceSubBudget != null ? sourceSubBudget.name() : normalizedName;

        List<ForwardMonthEntry> entries = new ArrayList<>();
        entries.add(new ForwardMonthEntry(sourceBudget.year(), sourceBudget.month(), sourceBudget, sourceSubBudget, sourceStatus(action)));

        for (int month = sourceBudget.month() + 1; month <= 12; month++) {
            Optional<Budget> monthBudgetOpt = budgetRepository.findByAccountIdAndYearAndMonth(accountId, sourceBudget.year(), month);
            if (monthBudgetOpt.isEmpty()) {
                entries.add(new ForwardMonthEntry(sourceBudget.year(), month, null, null, SubBudgetForwardMonthStatus.SKIPPED_NO_BUDGET));
                continue;
            }
            Budget monthBudget = monthBudgetOpt.get();
            if (monthBudget.status() != BudgetStatus.ACTIVE) {
                entries.add(new ForwardMonthEntry(sourceBudget.year(), month, monthBudget, null, SubBudgetForwardMonthStatus.SKIPPED_CLOSED));
                continue;
            }
            SubBudget matched = groupId == null
                    ? null
                    : subBudgetRepository.findActiveByAccountIdAndBudgetIdAndRecurringGroupId(accountId, monthBudget.id(), groupId).orElse(null);
            if (matched == null) {
                matched = subBudgetRepository.findManualActiveByAccountIdAndBudgetIdAndCategoryIdAndParticipantIdAndName(
                        accountId, monthBudget.id(), matchCategoryId, matchParticipantId, matchName
                ).orElse(null);
            }
            SubBudgetForwardMonthStatus status = resolveStatus(action, matched, categoryId, participantId, normalizedName, plannedAmount);
            entries.add(new ForwardMonthEntry(sourceBudget.year(), month, monthBudget, matched, status));
        }

        return new ForwardContext(accountId, action, sourceBudget, sourceSubBudget, categoryId, participantId, normalizedName, plannedAmount, entries);
    }

    private SubBudgetForwardMonthStatus resolveStatus(SubBudgetForwardAction action, SubBudget matched, Long proposedCategoryId, Long proposedParticipantId, String proposedName, Money proposedPlannedAmount) {
        if (action == SubBudgetForwardAction.DELETE) {
            return matched == null ? SubBudgetForwardMonthStatus.NO_CHANGE : SubBudgetForwardMonthStatus.WILL_DEACTIVATE;
        }
        if (matched == null) {
            return SubBudgetForwardMonthStatus.WILL_CREATE;
        }
        boolean sameValues = Objects.equals(matched.categoryId(), proposedCategoryId)
                && Objects.equals(matched.participantId(), proposedParticipantId)
                && Objects.equals(matched.name(), proposedName)
                && matched.plannedAmount().amount().compareTo(proposedPlannedAmount.amount()) == 0;
        return sameValues ? SubBudgetForwardMonthStatus.WILL_UPDATE : SubBudgetForwardMonthStatus.DIVERGES;
    }

    private SubBudgetForwardMonthStatus sourceStatus(SubBudgetForwardAction action) {
        return switch (action) {
            case CREATE -> SubBudgetForwardMonthStatus.WILL_CREATE;
            case UPDATE -> SubBudgetForwardMonthStatus.WILL_UPDATE;
            case DELETE -> SubBudgetForwardMonthStatus.WILL_DEACTIVATE;
        };
    }

    private void validateProposedPayload(SubBudgetForwardAction action, String name, Money plannedAmount) {
        if (action == SubBudgetForwardAction.DELETE) {
            return;
        }
        if (name == null || name.isBlank()) {
            throw new BusinessRuleViolationException("SUB_BUDGET_NAME_REQUIRED", "Sub-budget name is required.");
        }
        if (plannedAmount == null) {
            throw new BusinessRuleViolationException("SUB_BUDGET_AMOUNT_INVALID", "Sub-budget amount cannot be negative.");
        }
    }

    private UUID resolveGroupIdForApply(ForwardContext context) {
        if (context.action() == SubBudgetForwardAction.DELETE) {
            return null;
        }
        if (context.sourceSubBudget() != null && context.sourceSubBudget().recurringGroupId() != null) {
            return context.sourceSubBudget().recurringGroupId();
        }
        return UUID.randomUUID();
    }

    private SubBudgetForwardMonthResult applyToMonth(ForwardContext context, ForwardMonthEntry entry, UUID groupId) {
        return switch (entry.status()) {
            case SKIPPED_CLOSED -> new SubBudgetForwardMonthResult(entry.year(), entry.month(), "SKIPPED", "BUDGET_CLOSED");
            case SKIPPED_NO_BUDGET -> new SubBudgetForwardMonthResult(entry.year(), entry.month(), "SKIPPED", "BUDGET_NOT_FOUND");
            case NO_CHANGE -> new SubBudgetForwardMonthResult(entry.year(), entry.month(), "SKIPPED", "NO_CHANGE");
            case WILL_CREATE -> {
                SubBudget created = SubBudget.createManual(
                        context.accountId(), entry.budget().id(), context.proposedCategoryId(), context.proposedParticipantId(), context.proposedName(), context.proposedPlannedAmount()
                ).withRecurringGroupId(groupId);
                subBudgetRepository.save(created);
                yield new SubBudgetForwardMonthResult(entry.year(), entry.month(), "APPLIED", null);
            }
            case WILL_UPDATE, DIVERGES -> {
                SubBudget updated = entry.current().updateManual(context.proposedCategoryId(), context.proposedParticipantId(), context.proposedName(), context.proposedPlannedAmount());
                if (!Objects.equals(updated.recurringGroupId(), groupId)) {
                    updated = updated.withRecurringGroupId(groupId);
                }
                subBudgetRepository.save(updated);
                yield new SubBudgetForwardMonthResult(entry.year(), entry.month(), "APPLIED", null);
            }
            case WILL_DEACTIVATE -> {
                subBudgetRepository.save(entry.current().deactivateManual());
                yield new SubBudgetForwardMonthResult(entry.year(), entry.month(), "APPLIED", null);
            }
        };
    }

    private SubBudgetForwardMonthPlan toMonthPlanDto(ForwardMonthEntry entry, ForwardContext context) {
        SubBudget current = entry.current();
        boolean showProposed = context.action() != SubBudgetForwardAction.DELETE
                && entry.status() != SubBudgetForwardMonthStatus.SKIPPED_CLOSED
                && entry.status() != SubBudgetForwardMonthStatus.SKIPPED_NO_BUDGET;
        return new SubBudgetForwardMonthPlan(
                entry.year(),
                entry.month(),
                entry.budget() == null ? null : entry.budget().id(),
                entry.status(),
                current == null ? null : current.id(),
                current == null ? null : current.name(),
                current == null ? null : current.categoryId(),
                current == null ? null : current.participantId(),
                current == null ? null : current.plannedAmount().amount(),
                showProposed ? context.proposedName() : null,
                showProposed ? context.proposedCategoryId() : null,
                showProposed ? context.proposedParticipantId() : null,
                showProposed && context.proposedPlannedAmount() != null ? context.proposedPlannedAmount().amount() : null
        );
    }

    private record ForwardContext(
            Long accountId,
            SubBudgetForwardAction action,
            Budget sourceBudget,
            SubBudget sourceSubBudget,
            Long proposedCategoryId,
            Long proposedParticipantId,
            String proposedName,
            Money proposedPlannedAmount,
            List<ForwardMonthEntry> entries
    ) {
    }

    private record ForwardMonthEntry(Integer year, Integer month, Budget budget, SubBudget current, SubBudgetForwardMonthStatus status) {
    }

    @Override
    @Transactional
    public void createImpactsForInstallmentDebt(CreateDebtBudgetImpactsCommand command) {
        validateInstallmentTotals(command);
        for (int installmentIndex = 0; installmentIndex < command.installmentCount(); installmentIndex++) {
            LocalDate period = command.firstInstallmentDate().plusMonths(installmentIndex);
            Budget budget = getOrCreateBudget(command.accountId(), period.getYear(), period.getMonthValue());
            String subBudgetName = "Debt: " + command.debtName();
            SubBudget subBudget = subBudgetRepository.findDebtDerivedByAccountIdAndBudgetIdAndDebtId(command.accountId(), budget.id(), command.debtId())
                    .orElseGet(() -> subBudgetRepository.save(SubBudget.createDebtDerived(command.accountId(), budget.id(), command.categoryId(), command.participantId(), command.debtId(), subBudgetName, command.installmentAmount())));
            if (impactRepository.findByAccountIdAndDebtIdAndPeriod(command.accountId(), command.debtId(), period.getYear(), period.getMonthValue()).isEmpty()) {
                impactRepository.save(BudgetImpact.createDebtInstallment(
                        command.accountId(),
                        budget.id(),
                        subBudget.id(),
                        command.debtId(),
                        command.expenseId(),
                        period.getYear(),
                        period.getMonthValue(),
                        command.installmentAmount()
                ));
            }
        }
    }

    @Override
    @Transactional
    public void applyDebtPaymentToImpacts(ApplyDebtPaymentImpactCommand command) {
        Money remainingPayment = command.amount();
        List<BudgetImpact> impacts = impactRepository.findActiveByAccountIdAndDebtIdOrderByPeriod(command.accountId(), command.debtId());
        for (BudgetImpact impact : impacts) {
            if (remainingPayment.isZero()) {
                break;
            }
            Money unpaid = impact.unpaidAmount();
            if (unpaid.isZero()) {
                continue;
            }
            BigDecimal appliedAmount = remainingPayment.amount().min(unpaid.amount());
            BudgetImpact updated = impact.applyPayment(Money.cop(appliedAmount));
            impactRepository.save(updated);
            remainingPayment = Money.cop(remainingPayment.amount().subtract(appliedAmount));
        }
        if (!remainingPayment.isZero()) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_UPDATE_FAILED", "Debt payment could not be fully applied to budget impacts.");
        }
        if (command.debtFullySettled()) {
            cancelUnreachedFutureImpacts(command.accountId(), command.debtId(), YearMonth.from(command.settlementDate()));
        }
    }

    private void cancelUnreachedFutureImpacts(Long accountId, Long debtId, YearMonth settlementPeriod) {
        List<BudgetImpact> impacts = impactRepository.findNonCancelledByAccountIdAndDebtIdOrderByPeriod(accountId, debtId);
        Set<Long> futureSubBudgetIds = new HashSet<>();
        for (BudgetImpact impact : impacts) {
            YearMonth impactPeriod = YearMonth.of(impact.periodYear(), impact.periodMonth());
            if (impactPeriod.isAfter(settlementPeriod)) {
                impactRepository.save(impact.cancel());
                futureSubBudgetIds.add(impact.subBudgetId());
            }
        }
        if (futureSubBudgetIds.isEmpty()) {
            return;
        }
        List<SubBudget> derivedSubBudgets = subBudgetRepository.findDebtDerivedActiveByAccountIdAndDebtId(accountId, debtId);
        for (SubBudget subBudget : derivedSubBudgets) {
            if (futureSubBudgetIds.contains(subBudget.id())) {
                subBudgetRepository.save(subBudget.deactivateDebtDerived());
            }
        }
    }

    @Override
    @Transactional
    public void cancelActiveImpactsForDebt(Long accountId, Long debtId) {
        List<BudgetImpact> impacts = impactRepository.findNonCancelledByAccountIdAndDebtIdOrderByPeriod(accountId, debtId);
        for (BudgetImpact impact : impacts) {
            impactRepository.save(impact.cancel());
        }
        List<SubBudget> derivedSubBudgets = subBudgetRepository.findDebtDerivedActiveByAccountIdAndDebtId(accountId, debtId);
        for (SubBudget subBudget : derivedSubBudgets) {
            subBudgetRepository.save(subBudget.deactivateDebtDerived());
        }
    }

    private Budget getOrCreateBudget(Long accountId, Integer year, Integer month) {
        return budgetRepository.getOrCreateMonthlyBudget(accountId, year, month, "Budget " + year + "-" + String.format("%02d", month));
    }

    private void validateInstallmentTotals(CreateDebtBudgetImpactsCommand command) {
        if (command.installmentCount() == null || command.installmentAmount() == null || command.totalAmount() == null || command.firstInstallmentDate() == null) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_CREATION_FAILED", "Installment debt data is incomplete.");
        }
        BigDecimal expectedTotal = command.installmentAmount().amount().multiply(BigDecimal.valueOf(command.installmentCount()));
        if (expectedTotal.compareTo(command.totalAmount().amount()) != 0) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_CREATION_FAILED", "Installment amount multiplied by installment count must match financed debt total amount.");
        }
    }

    private BudgetDetailResponse detailResponse(Budget budget) {
        List<SubBudget> subBudgets = subBudgetRepository.findByAccountIdAndBudgetId(budget.accountId(), budget.id());
        List<BudgetImpact> budgetImpacts = impactRepository.findByAccountIdAndBudgetId(budget.accountId(), budget.id());
        Map<Long, BigDecimal> manualSpentBySubBudget = manualSpentBySubBudget(
                subBudgets,
                manualSpentByGlobalCategory(budget, subBudgets),
                manualSpentByAssignedCategoryAndParticipant(budget, subBudgets)
        );
        Map<Long, BigDecimal> debtPaidBySubBudget = budgetImpacts.stream()
                .filter(impact -> impact.status() != com.easyfinance.budgets.domain.model.BudgetImpactStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        BudgetImpact::subBudgetId,
                        Collectors.reducing(BigDecimal.ZERO, impact -> impact.paidAmount().amount(), BigDecimal::add)
        ));
        List<SubBudgetResponse> subBudgetResponses = subBudgets.stream()
                .map(subBudget -> toSubBudgetResponse(subBudget, spentAmount(subBudget, manualSpentBySubBudget, debtPaidBySubBudget)))
                .toList();
        List<BudgetImpactResponse> impacts = budgetImpacts.stream().map(this::toImpactResponse).toList();
        return new BudgetDetailResponse(toBudgetResponse(budget), subBudgetResponses, impacts);
    }

    private Budget findBudget(Long accountId, Long budgetId) {
        return budgetRepository.findByAccountIdAndId(accountId, budgetId)
                .orElseThrow(() -> new NotFoundException("BUDGET_NOT_FOUND", "Budget was not found."));
    }

    private SubBudget findSubBudget(Long accountId, Long budgetId, Long subBudgetId) {
        return subBudgetRepository.findByAccountIdAndBudgetIdAndId(accountId, budgetId, subBudgetId)
                .orElseThrow(() -> new NotFoundException("SUB_BUDGET_NOT_FOUND", "Sub-budget was not found."));
    }

    private void validateActiveCategory(Long accountId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        CategoryValidationView category = catalogValidationPort.findCategoryForValidation(accountId, categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category was not found."));
        if (category.status() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("CATEGORY_INACTIVE", "Category is inactive.");
        }
        if (category.type() != CategoryType.EXPENSE) {
            throw new BusinessRuleViolationException("EXPENSE_CATEGORY_INVALID_TYPE", "Budget sub-budget category must be an EXPENSE category.");
        }
    }

    private Long currentParticipantId() {
        CurrentUser currentUser = currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
        return currentUser.participantId();
    }

    private BudgetResponse toBudgetResponse(Budget budget) {
        return new BudgetResponse(budget.id(), budget.accountId(), budget.year(), budget.month(), budget.name(), budget.status().name(), budget.createdAt(), budget.updatedAt());
    }

    private SubBudgetResponse toSubBudgetResponse(SubBudget subBudget) {
        return toSubBudgetResponse(subBudget, subBudget.spentAmount().amount());
    }

    private SubBudgetResponse toSubBudgetResponse(SubBudget subBudget, BigDecimal spentAmount) {
        return new SubBudgetResponse(
                subBudget.id(),
                subBudget.accountId(),
                subBudget.budgetId(),
                subBudget.categoryId(),
                subBudget.participantId(),
                subBudget.debtId(),
                subBudget.name(),
                subBudget.plannedAmount().amount(),
                subBudget.plannedAmount().currency().name(),
                spentAmount.setScale(2, RoundingMode.HALF_UP),
                subBudget.spentAmount().currency().name(),
                subBudget.status().name(),
                subBudget.sourceType().name(),
                subBudget.createdAt(),
                subBudget.updatedAt()
        );
    }

    private BudgetImpactResponse toImpactResponse(BudgetImpact impact) {
        return new BudgetImpactResponse(
                impact.id(),
                impact.accountId(),
                impact.budgetId(),
                impact.subBudgetId(),
                impact.debtId(),
                impact.expenseId(),
                impact.periodYear(),
                impact.periodMonth(),
                impact.expectedAmount().amount(),
                impact.expectedAmount().currency().name(),
                impact.paidAmount().amount(),
                impact.paidAmount().currency().name(),
                impact.status().name(),
                impact.sourceType().name(),
                impact.createdAt(),
                impact.updatedAt()
        );
    }

    private Map<Long, BigDecimal> manualSpentByGlobalCategory(Budget budget, List<SubBudget> subBudgets) {
        List<Long> categoryIds = subBudgets.stream()
                .filter(subBudget -> subBudget.status() == SubBudgetStatus.ACTIVE)
                .filter(subBudget -> subBudget.sourceType() == SubBudgetSourceType.MANUAL)
                .filter(subBudget -> subBudget.participantId() == null)
                .map(SubBudget::categoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        YearMonth period = YearMonth.of(budget.year(), budget.month());
        return expenseExecutionQueryPort.sumManualExecutionByCategory(
                budget.accountId(),
                period.atDay(1),
                period.atEndOfMonth(),
                categoryIds
        );
    }

    private Map<BudgetExpenseExecutionQueryPort.CategoryParticipantKey, BigDecimal> manualSpentByAssignedCategoryAndParticipant(Budget budget, List<SubBudget> subBudgets) {
        List<BudgetExpenseExecutionQueryPort.CategoryParticipantKey> keys = subBudgets.stream()
                .filter(subBudget -> subBudget.status() == SubBudgetStatus.ACTIVE)
                .filter(subBudget -> subBudget.sourceType() == SubBudgetSourceType.MANUAL)
                .filter(subBudget -> subBudget.categoryId() != null)
                .filter(subBudget -> subBudget.participantId() != null)
                .map(subBudget -> new BudgetExpenseExecutionQueryPort.CategoryParticipantKey(subBudget.categoryId(), subBudget.participantId()))
                .distinct()
                .toList();
        if (keys.isEmpty()) {
            return Map.of();
        }
        YearMonth period = YearMonth.of(budget.year(), budget.month());
        return expenseExecutionQueryPort.sumManualExecutionByCategoryAndParticipant(
                budget.accountId(),
                period.atDay(1),
                period.atEndOfMonth(),
                keys
        );
    }

    private Map<Long, BigDecimal> manualSpentBySubBudget(
            List<SubBudget> subBudgets,
            Map<Long, BigDecimal> manualSpentByGlobalCategory,
            Map<BudgetExpenseExecutionQueryPort.CategoryParticipantKey, BigDecimal> manualSpentByAssignedCategoryAndParticipant
    ) {
        if (manualSpentByGlobalCategory.isEmpty() && manualSpentByAssignedCategoryAndParticipant.isEmpty()) {
            return Map.of();
        }
        Map<ManualExecutionKey, List<SubBudget>> activeManualByKey = subBudgets.stream()
                .filter(subBudget -> subBudget.status() == SubBudgetStatus.ACTIVE)
                .filter(subBudget -> subBudget.sourceType() == SubBudgetSourceType.MANUAL)
                .filter(subBudget -> subBudget.categoryId() != null)
                .collect(Collectors.groupingBy(subBudget -> new ManualExecutionKey(subBudget.categoryId(), subBudget.participantId())));
        return activeManualByKey.entrySet().stream()
                .flatMap(entry -> allocateCategorySpent(entry.getValue(), spentForManualExecutionKey(entry.getKey(), manualSpentByGlobalCategory, manualSpentByAssignedCategoryAndParticipant)).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private BigDecimal spentForManualExecutionKey(
            ManualExecutionKey key,
            Map<Long, BigDecimal> manualSpentByGlobalCategory,
            Map<BudgetExpenseExecutionQueryPort.CategoryParticipantKey, BigDecimal> manualSpentByAssignedCategoryAndParticipant
    ) {
        if (key.participantId() == null) {
            return manualSpentByGlobalCategory.getOrDefault(key.categoryId(), BigDecimal.ZERO);
        }
        return manualSpentByAssignedCategoryAndParticipant.getOrDefault(
                new BudgetExpenseExecutionQueryPort.CategoryParticipantKey(key.categoryId(), key.participantId()),
                BigDecimal.ZERO
        );
    }

    private Map<Long, BigDecimal> allocateCategorySpent(List<SubBudget> subBudgets, BigDecimal categorySpent) {
        if (subBudgets.isEmpty() || categorySpent.compareTo(BigDecimal.ZERO) == 0) {
            return Map.of();
        }
        List<SubBudget> ordered = new ArrayList<>(subBudgets);
        ordered.sort(Comparator.comparing(SubBudget::id, Comparator.nullsLast(Long::compareTo)));
        if (ordered.size() == 1) {
            return Map.of(ordered.getFirst().id(), categorySpent);
        }
        BigDecimal totalPlanned = ordered.stream()
                .map(subBudget -> subBudget.plannedAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPlanned.compareTo(BigDecimal.ZERO) <= 0) {
            return allocateEvenly(ordered, categorySpent);
        }
        Map<Long, BigDecimal> allocated = new java.util.LinkedHashMap<>();
        BigDecimal assigned = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (int index = 0; index < ordered.size(); index++) {
            SubBudget subBudget = ordered.get(index);
            BigDecimal amount = index == ordered.size() - 1
                    ? categorySpent.subtract(assigned)
                    : categorySpent.multiply(subBudget.plannedAmount().amount()).divide(totalPlanned, 2, RoundingMode.HALF_UP);
            allocated.put(subBudget.id(), amount);
            assigned = assigned.add(amount);
        }
        return allocated;
    }

    private Map<Long, BigDecimal> allocateEvenly(List<SubBudget> subBudgets, BigDecimal categorySpent) {
        Map<Long, BigDecimal> allocated = new java.util.LinkedHashMap<>();
        BigDecimal assigned = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (int index = 0; index < subBudgets.size(); index++) {
            BigDecimal amount = index == subBudgets.size() - 1
                    ? categorySpent.subtract(assigned)
                    : categorySpent.divide(BigDecimal.valueOf(subBudgets.size()), 2, RoundingMode.HALF_UP);
            allocated.put(subBudgets.get(index).id(), amount);
            assigned = assigned.add(amount);
        }
        return allocated;
    }

    private BigDecimal spentAmount(SubBudget subBudget, Map<Long, BigDecimal> manualSpentBySubBudget, Map<Long, BigDecimal> debtPaidBySubBudget) {
        if (subBudget.status() != SubBudgetStatus.ACTIVE) {
            return subBudget.spentAmount().amount();
        }
        if (subBudget.sourceType() == SubBudgetSourceType.DEBT_DERIVED) {
            return debtPaidBySubBudget.getOrDefault(subBudget.id(), BigDecimal.ZERO);
        }
        if (subBudget.sourceType() != SubBudgetSourceType.MANUAL || subBudget.categoryId() == null) {
            return subBudget.spentAmount().amount();
        }
        return manualSpentBySubBudget.getOrDefault(subBudget.id(), BigDecimal.ZERO);
    }

    private record ManualExecutionKey(Long categoryId, Long participantId) {
    }
}
