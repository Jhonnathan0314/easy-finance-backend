package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.response.CategoryResponse;

public interface GetCategoryPort {
    CategoryResponse getCategory(Long accountId, Long categoryId);
}
