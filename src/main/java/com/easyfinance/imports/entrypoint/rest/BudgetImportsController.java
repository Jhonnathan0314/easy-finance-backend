package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.command.ImportAnnualBudgetCommand;
import com.easyfinance.imports.application.port.in.GenerateAnnualBudgetImportTemplatePort;
import com.easyfinance.imports.application.port.in.ImportAnnualBudgetPort;
import com.easyfinance.imports.entrypoint.rest.dto.AnnualBudgetImportResponseDto;
import com.easyfinance.imports.entrypoint.rest.mapper.BudgetImportRestMapper;
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
@RequestMapping("/api/v1/accounts/{accountId}/imports/budgets/annual")
public class BudgetImportsController {

    private final GenerateAnnualBudgetImportTemplatePort generateTemplatePort;
    private final ImportAnnualBudgetPort importPort;

    public BudgetImportsController(
            GenerateAnnualBudgetImportTemplatePort generateTemplatePort,
            ImportAnnualBudgetPort importPort
    ) {
        this.generateTemplatePort = generateTemplatePort;
        this.importPort = importPort;
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template(@PathVariable Long accountId) {
        var template = generateTemplatePort.generate(accountId);
        ByteArrayResource resource = new ByteArrayResource(template.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(template.filename()).build().toString())
                .contentLength(template.content().length)
                .body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnnualBudgetImportResponseDto importDirect(@PathVariable Long accountId, @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        return BudgetImportRestMapper.toDto(importPort.importAnnualBudget(new ImportAnnualBudgetCommand(
                accountId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        )));
    }
}

