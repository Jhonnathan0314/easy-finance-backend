package com.easyfinance.imports.entrypoint.rest;

import com.easyfinance.imports.application.ExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/exports")
public class ExportsController {
    private final ExportService service;
    public ExportsController(ExportService service) { this.service = service; }
    @GetMapping("/{module}")
    public ResponseEntity<ByteArrayResource> export(@PathVariable Long accountId, @PathVariable String module, @RequestParam(required=false) Integer year, @RequestParam(required=false) Integer month) {
        var f=service.export(accountId,module,year,month); return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.filename()).build().toString()).contentLength(f.content().length).body(new ByteArrayResource(f.content()));
    }
}
