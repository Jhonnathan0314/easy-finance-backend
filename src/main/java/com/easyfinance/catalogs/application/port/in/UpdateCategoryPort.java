package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.command.UpdateCategoryCommand;
import com.easyfinance.catalogs.application.response.CategoryResponse;

public interface UpdateCategoryPort {
    CategoryResponse updateCategory(UpdateCategoryCommand command);
}
