package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.command.ImportCategoryCommand;
import com.easyfinance.imports.application.port.in.GenerateCategoryImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportCategoryPort;
import com.easyfinance.imports.application.port.in.PreviewCategoryImportPort;
import com.easyfinance.imports.entrypoint.rest.dto.CategoryImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.mapper.CategoryImportRestMapper;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/imports/categories")
public class CategoryImportsController {

    private final GenerateCategoryImportTemplatePort generateCategoryImportTemplatePort;
    private final ImportCategoryPort importCategoryPort;
    private final PreviewCategoryImportPort previewCategoryImportPort;

    public CategoryImportsController(
            GenerateCategoryImportTemplatePort generateCategoryImportTemplatePort,
            ImportCategoryPort importCategoryPort,
            PreviewCategoryImportPort previewCategoryImportPort
    ) {
        this.generateCategoryImportTemplatePort = generateCategoryImportTemplatePort;
        this.importCategoryPort = importCategoryPort;
        this.previewCategoryImportPort = previewCategoryImportPort;
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template(@PathVariable Long accountId) {
        var template = generateCategoryImportTemplatePort.generate(accountId);
        ByteArrayResource resource = new ByteArrayResource(template.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(template.filename()).build().toString())
                .contentLength(template.content().length)
                .body(resource);
    }

    @PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoryImportResponseDto preview(@PathVariable Long accountId, @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        return CategoryImportRestMapper.toDto(previewCategoryImportPort.previewCategories(new ImportCategoryCommand(
                accountId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        )));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CategoryImportResponseDto importDirect(@PathVariable Long accountId, @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        return CategoryImportRestMapper.toDto(importCategoryPort.importCategories(new ImportCategoryCommand(
                accountId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        )));
    }
}

