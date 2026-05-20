package com.easyfinance.catalogs.entrypoint.rest;

import com.easyfinance.catalogs.application.port.in.CreateCategoryPort;
import com.easyfinance.catalogs.application.port.in.DeactivateCategoryPort;
import com.easyfinance.catalogs.application.port.in.GetCategoryPort;
import com.easyfinance.catalogs.application.port.in.ListCategoriesPort;
import com.easyfinance.catalogs.application.port.in.UpdateCategoryPort;
import com.easyfinance.catalogs.application.query.ListCategoriesQuery;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.CategoryType;
import com.easyfinance.catalogs.entrypoint.rest.dto.CatalogStatusDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.CategoryResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.CategoryTypeDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.CreateCategoryRequest;
import com.easyfinance.catalogs.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.UpdateCategoryRequest;
import com.easyfinance.catalogs.entrypoint.rest.mapper.CatalogRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/categories")
public class CategoriesController {

    private final CreateCategoryPort createCategoryPort;
    private final ListCategoriesPort listCategoriesPort;
    private final GetCategoryPort getCategoryPort;
    private final UpdateCategoryPort updateCategoryPort;
    private final DeactivateCategoryPort deactivateCategoryPort;

    public CategoriesController(
            CreateCategoryPort createCategoryPort,
            ListCategoriesPort listCategoriesPort,
            GetCategoryPort getCategoryPort,
            UpdateCategoryPort updateCategoryPort,
            DeactivateCategoryPort deactivateCategoryPort
    ) {
        this.createCategoryPort = createCategoryPort;
        this.listCategoriesPort = listCategoriesPort;
        this.getCategoryPort = getCategoryPort;
        this.updateCategoryPort = updateCategoryPort;
        this.deactivateCategoryPort = deactivateCategoryPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponseDto create(@PathVariable Long accountId, @Valid @RequestBody CreateCategoryRequest request) {
        return CatalogRestMapper.toDto(createCategoryPort.createCategory(CatalogRestMapper.toCommand(accountId, request)));
    }

    @GetMapping
    public PageResponseDto<CategoryResponseDto> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) CategoryTypeDto type,
            @RequestParam(required = false) CatalogStatusDto status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        var query = new ListCategoriesQuery(
                accountId,
                type == null ? null : CategoryType.valueOf(type.name()),
                status == null ? null : CatalogStatus.valueOf(status.name()),
                search,
                PageQuery.of(page, size),
                sort
        );
        return CatalogRestMapper.toDto(listCategoriesPort.listCategories(query), CatalogRestMapper::toDto);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponseDto get(@PathVariable Long accountId, @PathVariable Long categoryId) {
        return CatalogRestMapper.toDto(getCategoryPort.getCategory(accountId, categoryId));
    }

    @PutMapping("/{categoryId}")
    public CategoryResponseDto update(
            @PathVariable Long accountId,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return CatalogRestMapper.toDto(updateCategoryPort.updateCategory(CatalogRestMapper.toCommand(accountId, categoryId, request)));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long accountId, @PathVariable Long categoryId) {
        deactivateCategoryPort.deactivateCategory(accountId, categoryId);
    }
}
