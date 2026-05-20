package com.easyfinance.catalogs.application.usecase;

import com.easyfinance.accounts.application.service.AccountAuthorizationService;
import com.easyfinance.catalogs.application.command.CreateCategoryCommand;
import com.easyfinance.catalogs.application.command.CreatePaymentMethodCommand;
import com.easyfinance.catalogs.application.command.UpdateCategoryCommand;
import com.easyfinance.catalogs.application.command.UpdatePaymentMethodCommand;
import com.easyfinance.catalogs.application.port.in.CreateCategoryPort;
import com.easyfinance.catalogs.application.port.in.CreatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.DeactivateCategoryPort;
import com.easyfinance.catalogs.application.port.in.DeactivatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.CatalogValidationPort;
import com.easyfinance.catalogs.application.port.in.GetCategoryPort;
import com.easyfinance.catalogs.application.port.in.GetPaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.ListCategoriesPort;
import com.easyfinance.catalogs.application.port.in.ListPaymentMethodsPort;
import com.easyfinance.catalogs.application.port.in.UpdateCategoryPort;
import com.easyfinance.catalogs.application.port.in.UpdatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.out.CategoryRepositoryPort;
import com.easyfinance.catalogs.application.port.out.PaymentMethodRepositoryPort;
import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.application.response.CategoryResponse;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.application.response.PaymentMethodResponse;
import com.easyfinance.catalogs.application.validation.CategoryValidationView;
import com.easyfinance.catalogs.application.validation.PaymentMethodValidationView;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.PaymentMethod;
import com.easyfinance.shared.application.CurrentUser;
import com.easyfinance.shared.application.CurrentUserProvider;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import com.easyfinance.shared.domain.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogManagementUseCase implements
        CreateCategoryPort,
        ListCategoriesPort,
        GetCategoryPort,
        UpdateCategoryPort,
        DeactivateCategoryPort,
        CreatePaymentMethodPort,
        ListPaymentMethodsPort,
        GetPaymentMethodPort,
        UpdatePaymentMethodPort,
        DeactivatePaymentMethodPort,
        CatalogValidationPort {

    private final CurrentUserProvider currentUserProvider;
    private final AccountAuthorizationService accountAuthorizationService;
    private final CategoryRepositoryPort categoryRepository;
    private final PaymentMethodRepositoryPort paymentMethodRepository;

    public CatalogManagementUseCase(
            CurrentUserProvider currentUserProvider,
            AccountAuthorizationService accountAuthorizationService,
            CategoryRepositoryPort categoryRepository,
            PaymentMethodRepositoryPort paymentMethodRepository
    ) {
        this.currentUserProvider = currentUserProvider;
        this.accountAuthorizationService = accountAuthorizationService;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Category category = Category.create(command.accountId(), command.name(), command.description(), command.type());
        ensureCategoryNameAvailable(category);
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> listCategories(ListCategoriesQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        PageResponse<Category> page = categoryRepository.findAll(query);
        return new PageResponse<>(
                page.content().stream().map(this::toCategoryResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long accountId, Long categoryId) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        return toCategoryResponse(findCategory(accountId, categoryId));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UpdateCategoryCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        Category category = findCategory(command.accountId(), command.categoryId());
        if (command.type() != null && command.type() != category.type()) {
            throw new BusinessRuleViolationException("CATEGORY_TYPE_CHANGE_NOT_ALLOWED", "Category type cannot be changed.");
        }
        Category updated = category.update(command.name(), command.description());
        ensureCategoryNameAvailableForUpdate(updated);
        return toCategoryResponse(categoryRepository.save(updated));
    }

    @Override
    @Transactional
    public void deactivateCategory(Long accountId, Long categoryId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentParticipantId());
        Category category = findCategory(accountId, categoryId);
        categoryRepository.save(category.deactivate());
    }

    @Override
    @Transactional
    public PaymentMethodResponse createPaymentMethod(CreatePaymentMethodCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        PaymentMethod paymentMethod = PaymentMethod.create(command.accountId(), command.name(), command.description(), command.type());
        ensurePaymentMethodNameAvailable(paymentMethod);
        return toPaymentMethodResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentMethodResponse> listPaymentMethods(ListPaymentMethodsQuery query) {
        accountAuthorizationService.requireActiveMember(query.accountId(), currentParticipantId());
        PageResponse<PaymentMethod> page = paymentMethodRepository.findAll(query);
        return new PageResponse<>(
                page.content().stream().map(this::toPaymentMethodResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentMethodResponse getPaymentMethod(Long accountId, Long paymentMethodId) {
        accountAuthorizationService.requireActiveMember(accountId, currentParticipantId());
        return toPaymentMethodResponse(findPaymentMethod(accountId, paymentMethodId));
    }

    @Override
    @Transactional
    public PaymentMethodResponse updatePaymentMethod(UpdatePaymentMethodCommand command) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(command.accountId(), currentParticipantId());
        PaymentMethod paymentMethod = findPaymentMethod(command.accountId(), command.paymentMethodId());
        if (command.type() != null && command.type() != paymentMethod.type()) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_TYPE_CHANGE_NOT_ALLOWED", "Payment method type cannot be changed.");
        }
        PaymentMethod updated = paymentMethod.update(command.name(), command.description());
        ensurePaymentMethodNameAvailableForUpdate(updated);
        return toPaymentMethodResponse(paymentMethodRepository.save(updated));
    }

    @Override
    @Transactional
    public void deactivatePaymentMethod(Long accountId, Long paymentMethodId) {
        accountAuthorizationService.requireActiveAdminForActiveAccount(accountId, currentParticipantId());
        PaymentMethod paymentMethod = findPaymentMethod(accountId, paymentMethodId);
        paymentMethodRepository.save(paymentMethod.deactivate());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<CategoryValidationView> findCategoryForValidation(Long accountId, Long categoryId) {
        return categoryRepository.findByAccountIdAndId(accountId, categoryId)
                .map(category -> new CategoryValidationView(category.id(), category.accountId(), category.type(), category.status()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<CategoryValidationView> findCategoryForValidation(Long accountId, String normalizedName) {
        return categoryRepository.findByAccountIdAndNormalizedName(accountId, normalizedName)
                .map(category -> new CategoryValidationView(category.id(), category.accountId(), category.type(), category.status()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<PaymentMethodValidationView> findPaymentMethodForValidation(Long accountId, Long paymentMethodId) {
        return paymentMethodRepository.findByAccountIdAndId(accountId, paymentMethodId)
                .map(paymentMethod -> new PaymentMethodValidationView(paymentMethod.id(), paymentMethod.accountId(), paymentMethod.type(), paymentMethod.status()));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<PaymentMethodValidationView> findPaymentMethodForValidation(Long accountId, String normalizedName) {
        return paymentMethodRepository.findByAccountIdAndNormalizedName(accountId, normalizedName)
                .map(paymentMethod -> new PaymentMethodValidationView(paymentMethod.id(), paymentMethod.accountId(), paymentMethod.type(), paymentMethod.status()));
    }

    private Long currentParticipantId() {
        CurrentUser currentUser = currentUserProvider.currentUser()
                .filter(CurrentUser::authenticated)
                .orElseThrow(() -> new UnauthorizedOperationException("UNAUTHENTICATED", "Authentication is required."));
        return currentUser.participantId();
    }

    private Category findCategory(Long accountId, Long categoryId) {
        return categoryRepository.findByAccountIdAndId(accountId, categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category was not found."));
    }

    private PaymentMethod findPaymentMethod(Long accountId, Long paymentMethodId) {
        return paymentMethodRepository.findByAccountIdAndId(accountId, paymentMethodId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_METHOD_NOT_FOUND", "Payment method was not found."));
    }

    private void ensureCategoryNameAvailable(Category category) {
        if (categoryRepository.existsActiveByAccountIdAndTypeAndNormalizedName(category.accountId(), category.type(), category.normalizedName())) {
            throw new BusinessRuleViolationException("CATEGORY_ALREADY_EXISTS", "Category already exists.");
        }
    }

    private void ensureCategoryNameAvailableForUpdate(Category category) {
        if (categoryRepository.existsActiveByAccountIdAndTypeAndNormalizedNameAndIdNot(
                category.accountId(),
                category.type(),
                category.normalizedName(),
                category.id()
        )) {
            throw new BusinessRuleViolationException("CATEGORY_ALREADY_EXISTS", "Category already exists.");
        }
    }

    private void ensurePaymentMethodNameAvailable(PaymentMethod paymentMethod) {
        if (paymentMethodRepository.existsActiveByAccountIdAndNormalizedName(paymentMethod.accountId(), paymentMethod.normalizedName())) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_ALREADY_EXISTS", "Payment method already exists.");
        }
    }

    private void ensurePaymentMethodNameAvailableForUpdate(PaymentMethod paymentMethod) {
        if (paymentMethodRepository.existsActiveByAccountIdAndNormalizedNameAndIdNot(
                paymentMethod.accountId(),
                paymentMethod.normalizedName(),
                paymentMethod.id()
        )) {
            throw new BusinessRuleViolationException("PAYMENT_METHOD_ALREADY_EXISTS", "Payment method already exists.");
        }
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.id(),
                category.accountId(),
                category.name(),
                category.description(),
                category.type().name(),
                category.status().name(),
                category.createdAt(),
                category.updatedAt()
        );
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.id(),
                paymentMethod.accountId(),
                paymentMethod.name(),
                paymentMethod.description(),
                paymentMethod.type().name(),
                paymentMethod.status().name(),
                paymentMethod.createdAt(),
                paymentMethod.updatedAt()
        );
    }
}
