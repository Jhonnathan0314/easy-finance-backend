package com.easyfinance.income.application.usecase;

import com.easyfinance.accounts.application.service.AccountAccess;
import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.accounts.application.service.AssignedParticipantValidator;
import com.easyfinance.accounts.domain.model.AccountParticipantRole;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.income.application.command.CreateIncomeCommand;
import com.easyfinance.income.application.command.DuplicateIncomeCommand;
import com.easyfinance.income.application.command.UpdateIncomeCommand;
import com.easyfinance.income.application.port.in.CancelIncomePort;
import com.easyfinance.income.application.port.in.CreateIncomePort;
import com.easyfinance.income.application.port.in.DuplicateIncomePort;
import com.easyfinance.income.application.port.in.GetIncomePort;
import com.easyfinance.income.application.port.in.ListIncomesPort;
import com.easyfinance.income.application.port.in.UpdateIncomePort;
import com.easyfinance.income.application.port.out.IncomeRepositoryPort;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.application.response.IncomeResponse;
import com.easyfinance.income.application.response.PageResponse;
import com.easyfinance.income.domain.model.Income;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.ForbiddenOperationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeManagementUseCase implements
        CreateIncomePort,
        ListIncomesPort,
        GetIncomePort,
        UpdateIncomePort,
        CancelIncomePort,
        DuplicateIncomePort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CatalogValidationPort catalogValidationPort;
    private final AssignedParticipantValidator assignedParticipantValidator;
    private final IncomeRepositoryPort incomeRepository;

    public IncomeManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CatalogValidationPort catalogValidationPort,
            AssignedParticipantValidator assignedParticipantValidator,
            IncomeRepositoryPort incomeRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.catalogValidationPort = catalogValidationPort;
        this.assignedParticipantValidator = assignedParticipantValidator;
        this.incomeRepository = incomeRepository;
    }

    @Override
    @Transactional
    public IncomeResponse createIncome(CreateIncomeCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Long assignedParticipantId = assignedParticipantValidator.resolveAssignedParticipantId(access, command.participantId());
        validateCategory(command.accountId(), command.categoryId());
        Income income = Income.create(command.accountId(), command.categoryId(), assignedParticipantId, command.description(), command.amount(), command.incomeDate());
        return toResponse(incomeRepository.save(income));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IncomeResponse> listIncomes(ListIncomesQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        PageResponse<Income> page = incomeRepository.findAll(query);
        return new PageResponse<>(
                page.content().stream().map(this::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeResponse getIncome(Long accountId, Long incomeId) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        return toResponse(findIncome(accountId, incomeId));
    }

    @Override
    @Transactional
    public IncomeResponse updateIncome(UpdateIncomeCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Income income = findIncome(command.accountId(), command.incomeId());
        income.ensureActive();
        ensureCanMutate(income, access, currentUser.participantId(), "INCOME_UPDATE_NOT_ALLOWED");
        validateCategory(command.accountId(), command.categoryId());
        Long assignedParticipantId = command.participantId() == null
                ? income.participantId()
                : assignedParticipantValidator.resolveAssignedParticipantId(access, command.participantId());
        Income updated = income.update(command.categoryId(), assignedParticipantId, command.description(), command.amount(), command.incomeDate());
        return toResponse(incomeRepository.save(updated));
    }

    @Override
    @Transactional
    public IncomeResponse duplicateIncome(DuplicateIncomeCommand command) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(command.accountId(), currentUser.participantId());
        Income source = findIncome(command.accountId(), command.incomeId());
        ensureCanMutate(source, access, currentUser.participantId(), "INCOME_DUPLICATE_NOT_ALLOWED");
        if (source.status() != IncomeStatus.ACTIVE) {
            throw new BusinessRuleViolationException("INCOME_DUPLICATE_NOT_ALLOWED", "Only active incomes can be duplicated.");
        }
        validateCategory(command.accountId(), source.categoryId());
        String description = command.description() == null || command.description().isBlank()
                ? source.description()
                : command.description();
        Income duplicate = Income.create(
                source.accountId(),
                source.categoryId(),
                source.participantId(),
                description,
                command.amount() == null ? source.amount() : command.amount(),
                command.incomeDate()
        );
        return toResponse(incomeRepository.save(duplicate));
    }

    @Override
    @Transactional
    public void cancelIncome(Long accountId, Long incomeId) {
        CurrentUser currentUser = currentUser();
        AccountAccess access = accountAuthorizationService.requireActiveMemberForActiveAccount(accountId, currentUser.participantId());
        Income income = findIncome(accountId, incomeId);
        income.ensureActive();
        ensureCanMutate(income, access, currentUser.participantId(), "INCOME_CANCEL_NOT_ALLOWED");
        incomeRepository.save(income.cancel());
    }

    private CurrentUser currentUser() {
        return currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
    }

    private Long currentParticipantId() {
        return currentUser().participantId();
    }

    private Income findIncome(Long accountId, Long incomeId) {
        return incomeRepository.findByAccountIdAndId(accountId, incomeId)
                .orElseThrow(() -> new NotFoundException("INCOME_NOT_FOUND", "Income was not found."));
    }

    private void ensureCanMutate(Income income, AccountAccess access, Long participantId, String errorCode) {
        if (income.participantId().equals(participantId) || access.membership().role() == AccountParticipantRole.ACCOUNT_ADMIN) {
            return;
        }
        throw new ForbiddenOperationException(errorCode, "Income operation is not allowed.");
    }

    private void validateCategory(Long accountId, Long categoryId) {
        if (categoryId == null) {
            throw new BusinessRuleViolationException("INCOME_CATEGORY_REQUIRED", "Income category is required.");
        }
        CategoryValidationView category = catalogValidationPort.findCategoryForValidation(accountId, categoryId)
                .orElseThrow(() -> new NotFoundException("INCOME_CATEGORY_NOT_FOUND", "Income category was not found."));
        if (category.status() != CatalogStatus.ACTIVE) {
            throw new BusinessRuleViolationException("INCOME_CATEGORY_INACTIVE", "Income category is inactive.");
        }
        if (category.type() != CategoryType.INCOME) {
            throw new BusinessRuleViolationException("INCOME_CATEGORY_INVALID_TYPE", "Income category must be an INCOME category.");
        }
    }

    private IncomeResponse toResponse(Income income) {
        return new IncomeResponse(
                income.id(),
                income.accountId(),
                income.categoryId(),
                income.participantId(),
                income.description(),
                income.amount().amount(),
                income.amount().currency().name(),
                income.incomeDate(),
                income.status().name(),
                income.createdAt(),
                income.updatedAt()
        );
    }
}
