package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.command.PreviewExpenseImportCommand;
import com.easyfinance.imports.application.port.in.ConfirmExpenseImportPort;
import com.easyfinance.imports.application.port.in.GenerateExpenseImportTemplatePort;
import com.easyfinance.imports.application.port.in.GetExpenseImportBatchPort;
import com.easyfinance.imports.application.port.in.PreviewExpenseImportPort;
import com.easyfinance.imports.entrypoint.rest.dto.ExpenseImportBatchResponseDto;
import com.easyfinance.imports.entrypoint.rest.mapper.ExpenseImportRestMapper;
import com.easyfinance.shared.domain.BusinessRuleViolationException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/imports/expenses")
public class ExpenseImportsController {

    private final PreviewExpenseImportPort previewExpenseImportPort;
    private final ConfirmExpenseImportPort confirmExpenseImportPort;
    private final GetExpenseImportBatchPort getExpenseImportBatchPort;
    private final GenerateExpenseImportTemplatePort generateExpenseImportTemplatePort;

    public ExpenseImportsController(
            PreviewExpenseImportPort previewExpenseImportPort,
            ConfirmExpenseImportPort confirmExpenseImportPort,
            GetExpenseImportBatchPort getExpenseImportBatchPort,
            GenerateExpenseImportTemplatePort generateExpenseImportTemplatePort
    ) {
        this.previewExpenseImportPort = previewExpenseImportPort;
        this.confirmExpenseImportPort = confirmExpenseImportPort;
        this.getExpenseImportBatchPort = getExpenseImportBatchPort;
        this.generateExpenseImportTemplatePort = generateExpenseImportTemplatePort;
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template(@PathVariable Long accountId) {
        var template = generateExpenseImportTemplatePort.generate(accountId);
        ByteArrayResource resource = new ByteArrayResource(template.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(template.filename()).build().toString())
                .contentLength(template.content().length)
                .body(resource);
    }

    @PostMapping(path = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExpenseImportBatchResponseDto preview(@PathVariable Long accountId, @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("IMPORT_FILE_REQUIRED", "Import file is required.");
        }
        return ExpenseImportRestMapper.toDto(previewExpenseImportPort.preview(new PreviewExpenseImportCommand(
                accountId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
        )));
    }

    @PostMapping("/{batchId}/confirm")
    public ExpenseImportBatchResponseDto confirm(@PathVariable Long accountId, @PathVariable Long batchId) {
        return ExpenseImportRestMapper.toDto(confirmExpenseImportPort.confirm(accountId, batchId));
    }

    @GetMapping("/{batchId}")
    public ExpenseImportBatchResponseDto get(@PathVariable Long accountId, @PathVariable Long batchId) {
        return ExpenseImportRestMapper.toDto(getExpenseImportBatchPort.get(accountId, batchId));
    }
}
