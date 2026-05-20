package com.easyfinance.catalogs.domain.model;

import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createsActiveCategory() {
        Category category = Category.create(1L, " Food ", " Meals ", CategoryType.EXPENSE);

        assertThat(category.name()).isEqualTo("Food");
        assertThat(category.normalizedName()).isEqualTo("food");
        assertThat(category.description()).isEqualTo("Meals");
        assertThat(category.status()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Category.create(1L, " ", null, CategoryType.EXPENSE))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_NAME_REQUIRED"));
    }

    @Test
    void inactiveCategoryCannotBeUpdated() {
        Category inactive = Category.restore(1L, 1L, "Food", null, CategoryType.EXPENSE, CatalogStatus.INACTIVE, null, null);

        assertThatThrownBy(() -> inactive.update("Food 2", null))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class, ex -> assertThat(ex.code()).isEqualTo("CATEGORY_INACTIVE"));
    }
}
