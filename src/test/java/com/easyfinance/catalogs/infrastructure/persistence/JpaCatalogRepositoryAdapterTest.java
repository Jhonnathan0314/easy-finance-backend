package com.easyfinance.catalogs.infrastructure.persistence;

import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.domain.model.PaymentMethod;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CategoryJpaEntity;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CategoryTypeJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.CatalogStatusJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.PaymentMethodJpaEntity;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.PaymentMethodTypeJpa;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.SpringDataCategoryRepository;
import com.easyfinance.catalogs.infrastructure.persistence.jpa.SpringDataPaymentMethodRepository;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import com.easyfinance.shared.domain.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaCatalogRepositoryAdapterTest {

    @Test
    void categoryUniqueConstraintIsTranslated() {
        SpringDataCategoryRepository repository = mock(SpringDataCategoryRepository.class);
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(repository);
        when(repository.saveAndFlush(any(CategoryJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates constraint uq_categories_active_account_type_name"));

        assertThatThrownBy(() -> adapter.save(Category.create(1L, "Food", null, CategoryType.EXPENSE)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_ALREADY_EXISTS"));
    }

    @Test
    void categoryUnknownIntegrityErrorIsNotTranslated() {
        SpringDataCategoryRepository repository = mock(SpringDataCategoryRepository.class);
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(repository);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");
        when(repository.saveAndFlush(any(CategoryJpaEntity.class))).thenThrow(exception);

        assertThatThrownBy(() -> adapter.save(Category.create(1L, "Food", null, CategoryType.EXPENSE)))
                .isSameAs(exception);
    }

    @Test
    void categoryUpdateInAnotherAccountReturnsNotFound() {
        SpringDataCategoryRepository repository = mock(SpringDataCategoryRepository.class);
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(repository);
        Category category = Category.restore(99L, 2L, "Food", null, CategoryType.EXPENSE, CatalogStatus.ACTIVE, Instant.now(), Instant.now());
        when(repository.findByAccountIdAndId(2L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(category))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_NOT_FOUND"));
        verify(repository, never()).saveAndFlush(any(CategoryJpaEntity.class));
    }

    @Test
    void paymentMethodUniqueConstraintIsTranslated() {
        SpringDataPaymentMethodRepository repository = mock(SpringDataPaymentMethodRepository.class);
        JpaPaymentMethodRepositoryAdapter adapter = new JpaPaymentMethodRepositoryAdapter(repository);
        when(repository.saveAndFlush(any(PaymentMethodJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates constraint uq_payment_methods_active_account_name"));

        assertThatThrownBy(() -> adapter.save(PaymentMethod.create(1L, "Cash", null, PaymentMethodType.CASH)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_ALREADY_EXISTS"));
    }

    @Test
    void paymentMethodUnknownIntegrityErrorIsNotTranslated() {
        SpringDataPaymentMethodRepository repository = mock(SpringDataPaymentMethodRepository.class);
        JpaPaymentMethodRepositoryAdapter adapter = new JpaPaymentMethodRepositoryAdapter(repository);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");
        when(repository.saveAndFlush(any(PaymentMethodJpaEntity.class))).thenThrow(exception);

        assertThatThrownBy(() -> adapter.save(PaymentMethod.create(1L, "Cash", null, PaymentMethodType.CASH)))
                .isSameAs(exception);
    }

    @Test
    void paymentMethodUpdateInAnotherAccountReturnsNotFound() {
        SpringDataPaymentMethodRepository repository = mock(SpringDataPaymentMethodRepository.class);
        JpaPaymentMethodRepositoryAdapter adapter = new JpaPaymentMethodRepositoryAdapter(repository);
        PaymentMethod paymentMethod = PaymentMethod.restore(99L, 2L, "Cash", null, PaymentMethodType.CASH, CatalogStatus.ACTIVE, Instant.now(), Instant.now());
        when(repository.findByAccountIdAndId(2L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(paymentMethod))
                .isInstanceOfSatisfying(NotFoundException.class, ex -> assertThat(ex.code()).isEqualTo("PAYMENT_METHOD_NOT_FOUND"));
        verify(repository, never()).saveAndFlush(any(PaymentMethodJpaEntity.class));
    }

    @Test
    void findsActiveExpenseCategoriesForTemplate() {
        SpringDataCategoryRepository repository = mock(SpringDataCategoryRepository.class);
        JpaCategoryRepositoryAdapter adapter = new JpaCategoryRepositoryAdapter(repository);
        when(repository.findByAccountIdAndTypeAndStatusOrderByNameAsc(1L, CategoryTypeJpa.EXPENSE, CatalogStatusJpa.ACTIVE))
                .thenReturn(List.of(categoryEntity(10L, "Food", CategoryTypeJpa.EXPENSE, CatalogStatusJpa.ACTIVE)));

        var categories = adapter.findActiveExpenseByAccountId(1L);

        assertThat(categories).extracting("name").containsExactly("Food");
        verify(repository).findByAccountIdAndTypeAndStatusOrderByNameAsc(1L, CategoryTypeJpa.EXPENSE, CatalogStatusJpa.ACTIVE);
    }

    @Test
    void findsActivePaymentMethodsForTemplate() {
        SpringDataPaymentMethodRepository repository = mock(SpringDataPaymentMethodRepository.class);
        JpaPaymentMethodRepositoryAdapter adapter = new JpaPaymentMethodRepositoryAdapter(repository);
        when(repository.findByAccountIdAndStatusOrderByNameAsc(1L, CatalogStatusJpa.ACTIVE))
                .thenReturn(List.of(paymentMethodEntity(20L, "Cash", CatalogStatusJpa.ACTIVE)));

        var paymentMethods = adapter.findActiveByAccountId(1L);

        assertThat(paymentMethods).extracting("name").containsExactly("Cash");
        verify(repository).findByAccountIdAndStatusOrderByNameAsc(1L, CatalogStatusJpa.ACTIVE);
    }

    private static CategoryJpaEntity categoryEntity(Long id, String name, CategoryTypeJpa type, CatalogStatusJpa status) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(id);
        entity.setAccountId(1L);
        entity.setName(name);
        entity.setNormalizedName(name.toLowerCase());
        entity.setType(type);
        entity.setStatus(status);
        return entity;
    }

    private static PaymentMethodJpaEntity paymentMethodEntity(Long id, String name, CatalogStatusJpa status) {
        PaymentMethodJpaEntity entity = new PaymentMethodJpaEntity();
        entity.setId(id);
        entity.setAccountId(1L);
        entity.setName(name);
        entity.setNormalizedName(name.toLowerCase());
        entity.setType(PaymentMethodTypeJpa.CASH);
        entity.setStatus(status);
        return entity;
    }
}
