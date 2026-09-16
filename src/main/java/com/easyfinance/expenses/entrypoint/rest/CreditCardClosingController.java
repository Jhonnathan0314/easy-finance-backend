package com.easyfinance.expenses.entrypoint.rest;

import com.easyfinance.expenses.application.port.in.ConfirmCreditCardClosingPort;
import com.easyfinance.expenses.application.port.in.PreviewCreditCardClosingPort;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingPreviewResponseDto;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingRequest;
import com.easyfinance.expenses.entrypoint.rest.dto.CreditCardClosingResultResponseDto;
import com.easyfinance.expenses.entrypoint.rest.mapper.CreditCardClosingRestMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/expenses/credit-card-closing")
public class CreditCardClosingController {

    private final PreviewCreditCardClosingPort previewCreditCardClosingPort;
    private final ConfirmCreditCardClosingPort confirmCreditCardClosingPort;

    public CreditCardClosingController(
            PreviewCreditCardClosingPort previewCreditCardClosingPort,
            ConfirmCreditCardClosingPort confirmCreditCardClosingPort
    ) {
        this.previewCreditCardClosingPort = previewCreditCardClosingPort;
        this.confirmCreditCardClosingPort = confirmCreditCardClosingPort;
    }

    @GetMapping("/preview")
    public CreditCardClosingPreviewResponseDto preview(
            @PathVariable Long accountId,
            @RequestParam Long paymentMethodId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return CreditCardClosingRestMapper.toDto(
                previewCreditCardClosingPort.previewClosing(CreditCardClosingRestMapper.toCommand(accountId, paymentMethodId, from, to))
        );
    }

    @PostMapping("/confirm")
    public CreditCardClosingResultResponseDto confirm(
            @PathVariable Long accountId,
            @Valid @RequestBody CreditCardClosingRequest request
    ) {
        return CreditCardClosingRestMapper.toDto(
                confirmCreditCardClosingPort.confirmClosing(CreditCardClosingRestMapper.toCommand(accountId, request))
        );
    }
}
