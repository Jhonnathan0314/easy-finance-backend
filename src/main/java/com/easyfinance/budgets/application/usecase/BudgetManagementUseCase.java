package com.easyfinance.budgets.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.budgets.application.command.ApplyDebtPaymentImpactCommand;
import com.easyfinance.budgets.application.command.CreateDebtBudgetImpactsCommand;
import com.easyfinance.budgets.application.command.CreateSubBudgetCommand;
import com.easyfinance.budgets.application.command.DuplicateBudgetCommand;
import com.easyfinance.budgets.application.command.UpdateSubBudgetCommand;
import com.easyfinance.budgets.application.command.UpsertBudgetCommand;
import com.easyfinance.budgets.application.port.in.BudgetDebtImpactPort;
import com.easyfinance.budgets.application.port.in.CreateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DeactivateSubBudgetPort;
import com.easyfinance.budgets.application.port.in.DuplicateBudgetPort;
import com.easyfinance.budgets.application.port.in.GetBudgetPort;
import com.easyfinance.budgets.application.port.in.ListBudgetsPort;
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
import com.easyfinance.budgets.application.response.PageResponse;
import com.easyfinance.budgets.application.response.SubBudgetResponse;
import com.easyfinance.budgets.domain.model.Budget;
import com.easyfinance.budgets.domain.model.BudgetImpact;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BudgetManagementUseCase implements
        UpsertBudgetPort,
        GetBudgetPort,
        ListBudgetsPort,
        CreateSubBudgetPort,
        UpdateSubBudgetPort,
        DeactivateSubBudgetPort,
        DuplicateBudgetPort,
        BudgetDebtImpactPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CatalogValidationPort catalogValidationPort;
    private final BudgetRepositoryPort budgetRepository;
    private final SubBudgetRepositoryPort subBudgetRepository;
    private final BudgetImpactRepositoryPort impactRepository;
    private final BudgetExpenseExecutionQueryPort expenseExecutionQueryPort;

    public BudgetManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            BudgetRepositoryPort budgetRepository,
            SubBudgetRepositoryPort subBudgetRepository,
            BudgetImpactRepositoryPort impactRepository,
            BudgetExpenseExecutionQueryPort expenseExecutionQueryPort
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
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
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Budget budget = findBudget(command.accountId(), command.budgetId());
        budget.ensureActive();
        validateActiveCategory(command.accountId(), command.categoryId());
        SubBudget subBudget = SubBudget.createManual(command.accountId(), command.budgetId(), command.categoryId(), command.name(), command.plannedAmount());
        return toSubBudgetResponse(subBudgetRepository.save(subBudget));
    }

    @Override
    @Transactional
    public SubBudgetResponse updateSubBudget(UpdateSubBudgetCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Budget budget = findBudget(command.accountId(), command.budgetId());
        budget.ensureActive();
        validateActiveCategory(command.accountId(), command.categoryId());
        SubBudget subBudget = findSubBudget(command.accountId(), command.budgetId(), command.subBudgetId());
        return toSubBudgetResponse(subBudgetRepository.save(subBudget.updateManual(command.categoryId(), command.name(), command.plannedAmount())));
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
    @Transactional
    public void createImpactsForInstallmentDebt(CreateDebtBudgetImpactsCommand command) {
        validateInstallmentTotals(command);
        for (int installmentIndex = 0; installmentIndex < command.installmentCount(); installmentIndex++) {
            LocalDate period = command.firstInstallmentDate().plusMonths(installmentIndex);
            Budget budget = getOrCreateBudget(command.accountId(), period.getYear(), period.getMonthValue());
            String subBudgetName = "Debt: " + command.debtName();
            SubBudget subBudget = subBudgetRepository.findDebtDerivedByAccountIdAndBudgetIdAndDebtId(command.accountId(), budget.id(), command.debtId())
                    .orElseGet(() -> subBudgetRepository.save(SubBudget.createDebtDerived(command.accountId(), budget.id(), command.categoryId(), command.debtId(), subBudgetName, command.installmentAmount())));
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
        Map<Long, BigDecimal> manualSpentBySubBudget = manualSpentBySubBudget(subBudgets, manualSpentByCategory(budget, subBudgets));
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

    private Map<Long, BigDecimal> manualSpentByCategory(Budget budget, List<SubBudget> subBudgets) {
        List<Long> categoryIds = subBudgets.stream()
                .filter(subBudget -> subBudget.status() == SubBudgetStatus.ACTIVE)
                .filter(subBudget -> subBudget.sourceType() == SubBudgetSourceType.MANUAL)
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

    private Map<Long, BigDecimal> manualSpentBySubBudget(List<SubBudget> subBudgets, Map<Long, BigDecimal> manualSpentByCategory) {
        if (manualSpentByCategory.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<SubBudget>> activeManualByCategory = subBudgets.stream()
                .filter(subBudget -> subBudget.status() == SubBudgetStatus.ACTIVE)
                .filter(subBudget -> subBudget.sourceType() == SubBudgetSourceType.MANUAL)
                .filter(subBudget -> subBudget.categoryId() != null)
                .collect(Collectors.groupingBy(SubBudget::categoryId));
        return activeManualByCategory.entrySet().stream()
                .flatMap(entry -> allocateCategorySpent(entry.getValue(), manualSpentByCategory.getOrDefault(entry.getKey(), BigDecimal.ZERO)).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
}
