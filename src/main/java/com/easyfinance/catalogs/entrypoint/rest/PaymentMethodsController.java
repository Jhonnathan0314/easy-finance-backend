package com.easyfinance.catalogs.entrypoint.rest;

import com.easyfinance.catalogs.application.port.in.CreatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.DeactivatePaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.GetPaymentMethodPort;
import com.easyfinance.catalogs.application.port.in.ListPaymentMethodsPort;
import com.easyfinance.catalogs.application.port.in.UpdatePaymentMethodPort;
import com.easyfinance.catalogs.application.query.ListPaymentMethodsQuery;
import com.easyfinance.catalogs.domain.model.CatalogStatus;
import com.easyfinance.catalogs.domain.model.PaymentMethodType;
import com.easyfinance.catalogs.entrypoint.rest.dto.CatalogStatusDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.CreatePaymentMethodRequest;
import com.easyfinance.catalogs.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.PaymentMethodResponseDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.PaymentMethodTypeDto;
import com.easyfinance.catalogs.entrypoint.rest.dto.UpdatePaymentMethodRequest;
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
@RequestMapping("/api/v1/accounts/{accountId}/payment-methods")
public class PaymentMethodsController {

    private final CreatePaymentMethodPort createPaymentMethodPort;
    private final ListPaymentMethodsPort listPaymentMethodsPort;
    private final GetPaymentMethodPort getPaymentMethodPort;
    private final UpdatePaymentMethodPort updatePaymentMethodPort;
    private final DeactivatePaymentMethodPort deactivatePaymentMethodPort;

    public PaymentMethodsController(
            CreatePaymentMethodPort createPaymentMethodPort,
            ListPaymentMethodsPort listPaymentMethodsPort,
            GetPaymentMethodPort getPaymentMethodPort,
            UpdatePaymentMethodPort updatePaymentMethodPort,
            DeactivatePaymentMethodPort deactivatePaymentMethodPort
    ) {
        this.createPaymentMethodPort = createPaymentMethodPort;
        this.listPaymentMethodsPort = listPaymentMethodsPort;
        this.getPaymentMethodPort = getPaymentMethodPort;
        this.updatePaymentMethodPort = updatePaymentMethodPort;
        this.deactivatePaymentMethodPort = deactivatePaymentMethodPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponseDto create(@PathVariable Long accountId, @Valid @RequestBody CreatePaymentMethodRequest request) {
        return CatalogRestMapper.toDto(createPaymentMethodPort.createPaymentMethod(CatalogRestMapper.toCommand(accountId, request)));
    }

    @GetMapping
    public PageResponseDto<PaymentMethodResponseDto> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) PaymentMethodTypeDto type,
            @RequestParam(required = false) CatalogStatusDto status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        var query = new ListPaymentMethodsQuery(
                accountId,
                type == null ? null : PaymentMethodType.valueOf(type.name()),
                status == null ? null : CatalogStatus.valueOf(status.name()),
                search,
                PageQuery.of(page, size),
                sort
        );
        return CatalogRestMapper.toDto(listPaymentMethodsPort.listPaymentMethods(query), CatalogRestMapper::toDto);
    }

    @GetMapping("/{paymentMethodId}")
    public PaymentMethodResponseDto get(@PathVariable Long accountId, @PathVariable Long paymentMethodId) {
        return CatalogRestMapper.toDto(getPaymentMethodPort.getPaymentMethod(accountId, paymentMethodId));
    }

    @PutMapping("/{paymentMethodId}")
    public PaymentMethodResponseDto update(
            @PathVariable Long accountId,
            @PathVariable Long paymentMethodId,
            @Valid @RequestBody UpdatePaymentMethodRequest request
    ) {
        return CatalogRestMapper.toDto(updatePaymentMethodPort.updatePaymentMethod(CatalogRestMapper.toCommand(accountId, paymentMethodId, request)));
    }

    @DeleteMapping("/{paymentMethodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long accountId, @PathVariable Long paymentMethodId) {
        deactivatePaymentMethodPort.deactivatePaymentMethod(accountId, paymentMethodId);
    }
}
