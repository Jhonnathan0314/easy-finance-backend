package com.easyfinance.catalogs.application.port.in;

public interface DeactivateCategoryPort {
    void deactivateCategory(Long accountId, Long categoryId);
}
