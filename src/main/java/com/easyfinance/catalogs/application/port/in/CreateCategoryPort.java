package com.easyfinance.catalogs.application.port.in;

import com.easyfinance.catalogs.application.command.CreateCategoryCommand;
import com.easyfinance.catalogs.application.response.CategoryResponse;

public interface CreateCategoryPort {
    CategoryResponse createCategory(CreateCategoryCommand command);
}
