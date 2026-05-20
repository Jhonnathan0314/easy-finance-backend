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
import java.time.LocalDate;
import java.util.List;

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

    public BudgetManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            BudgetRepositoryPort budgetRepository,
            SubBudgetRepositoryPort subBudgetRepository,
            BudgetImpactRepositoryPort impactRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.catalogValidationPort = catalogValidationPort;
        this.budgetRepository = budgetRepository;
        this.subBudgetRepository = subBudgetRepository;
        this.impactRepository = impactRepository;
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

    private Budget getOrCreateBudget(Long accountId, Integer year, Integer month) {
        return budgetRepository.getOrCreateMonthlyBudget(accountId, year, month, "Budget " + year + "-" + String.format("%02d", month));
    }

    private void validateInstallmentTotals(CreateDebtBudgetImpactsCommand command) {
        if (command.installmentCount() == null || command.installmentAmount() == null || command.totalAmount() == null || command.firstInstallmentDate() == null) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_CREATION_FAILED", "Installment debt data is incomplete.");
        }
        BigDecimal expectedTotal = command.installmentAmount().amount().multiply(BigDecimal.valueOf(command.installmentCount()));
        if (expectedTotal.compareTo(command.totalAmount().amount()) != 0) {
            throw new BusinessRuleViolationException("BUDGET_IMPACT_CREATION_FAILED", "Installment amount multiplied by installment count must match total amount.");
        }
    }

    private BudgetDetailResponse detailResponse(Budget budget) {
        List<SubBudgetResponse> subBudgets = subBudgetRepository.findByAccountIdAndBudgetId(budget.accountId(), budget.id())
                .stream().map(this::toSubBudgetResponse).toList();
        List<BudgetImpactResponse> impacts = impactRepository.findByAccountIdAndBudgetId(budget.accountId(), budget.id())
                .stream().map(this::toImpactResponse).toList();
        return new BudgetDetailResponse(toBudgetResponse(budget), subBudgets, impacts);
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
        return new SubBudgetResponse(
                subBudget.id(),
                subBudget.accountId(),
                subBudget.budgetId(),
                subBudget.categoryId(),
                subBudget.debtId(),
                subBudget.name(),
                subBudget.plannedAmount().amount(),
                subBudget.plannedAmount().currency().name(),
                subBudget.spentAmount().amount(),
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
}
