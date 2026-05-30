package com.easyfinance.catalogs.application.port.out;

import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.response.PageResponse;
import com.easyfinance.catalogs.domain.model.Category;
import com.easyfinance.catalogs.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {

    Category save(Category category);

    Optional<Category> findByAccountIdAndId(Long accountId, Long categoryId);

    Optional<Category> findByAccountIdAndNormalizedName(Long accountId, String normalizedName);

    Optional<Category> findByAccountIdAndTypeAndNormalizedName(Long accountId, CategoryType type, String normalizedName);

    boolean existsActiveByAccountIdAndTypeAndNormalizedName(Long accountId, CategoryType type, String normalizedName);

    boolean existsActiveByAccountIdAndTypeAndNormalizedNameAndIdNot(Long accountId, CategoryType type, String normalizedName, Long id);

    PageResponse<Category> findAll(ListCategoriesQuery query);

    List<Category> findActiveExpenseByAccountId(Long accountId);

    List<Category> findActiveIncomeByAccountId(Long accountId);
}
