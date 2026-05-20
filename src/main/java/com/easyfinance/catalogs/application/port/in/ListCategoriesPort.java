package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.application.response.CategoryResponse;
import com.easyfinance.catalogs.application.response.PageResponse;

public interface ListCategoriesPort {
    PageResponse<CategoryResponse> listCategories(ListCategoriesQuery query);
}
