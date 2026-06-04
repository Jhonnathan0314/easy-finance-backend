package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.command.ImportIncomeCommand;
import com.easyfinance.imports.application.port.in.GenerateIncomeImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportIncomePort;
import com.easyfinance.imports.application.port.in.PreviewIncomeImportPort;
import com.easyfinance.imports.entrypoint.rest.dto.IncomeImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.mapper.IncomeImportRestMapper;
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
@RequestMapping("/api/v1/accounts/{accountId}/imports/incomes")
public class IncomeImportsController {

    private final GenerateIncomeImportTemplatePort generateIncomeImportTemplatePort;
    private final ImportIncomePort importIncomePort;
    private final PreviewIncomeImportPort previewIncomeImportPort;

    public IncomeImportsController(
            GenerateIncomeImportTemplatePort generateIncomeImportTemplatePort,
            ImportIncomePort importIncomePort,
            PreviewIncomeImportPort previewIncomeImportPort
    ) {
        this.generateIncomeImportTemplatePort = generateIncomeImportTemplatePort;
        this.importIncomePort = importIncomePort;
        this.previewIncomeImportPort = previewIncomeImportPort;
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template(@PathVariable Long accountId) {
        var template = generateIncomeImportTemplatePort.generate(accountId);
        ByteArrayResource resource = new ByteArrayResource(template.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(template.filename()).build().toString())
                .contentLength(template.content().length)
                .body(resource);
    }

    @PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IncomeImportResponseDto preview(@PathVariable Long accountId, @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        return IncomeImportRestMapper.toDto(previewIncomeImportPort.previewIncomes(new ImportIncomeCommand(
                accountId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        )));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IncomeImportResponseDto importDirect(@PathVariable Long accountId, @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        return IncomeImportRestMapper.toDto(importIncomePort.importIncomes(new ImportIncomeCommand(
                accountId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        )));
    }
}

