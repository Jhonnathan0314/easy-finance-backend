package com.easyfinance.debts.entrypoint.rest;

import com.easyfinance.debts.application.port.in.GetDebtPaymentPort;
import com.easyfinance.debts.application.port.in.ListDebtPaymentsPort;
import com.easyfinance.debts.application.port.in.RegisterDebtPaymentPort;
import com.easyfinance.debts.application.query.ListDebtPaymentsQuery;
import com.easyfinance.debts.domain.model.DebtPaymentStatus;
import com.easyfinance.debts.domain.model.DebtPaymentType;
import com.easyfinance.debts.entrypoint.rest.dto.DebtPaymentResponseDto;
import com.easyfinance.debts.entrypoint.rest.dto.DebtPaymentStatusDto;
import com.easyfinance.debts.entrypoint.rest.dto.DebtPaymentTypeDto;
import com.easyfinance.debts.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.debts.entrypoint.rest.dto.RegisterDebtPaymentRequest;
import com.easyfinance.debts.entrypoint.rest.dto.RegisterDebtPaymentResponseDto;
import com.easyfinance.debts.entrypoint.rest.mapper.DebtPaymentRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/debts/{debtId}/payments")
public class DebtPaymentsController {

    private final RegisterDebtPaymentPort registerDebtPaymentPort;
    private final ListDebtPaymentsPort listDebtPaymentsPort;
    private final GetDebtPaymentPort getDebtPaymentPort;

    public DebtPaymentsController(
            RegisterDebtPaymentPort registerDebtPaymentPort,
            ListDebtPaymentsPort listDebtPaymentsPort,
            GetDebtPaymentPort getDebtPaymentPort
    ) {
        this.registerDebtPaymentPort = registerDebtPaymentPort;
        this.listDebtPaymentsPort = listDebtPaymentsPort;
        this.getDebtPaymentPort = getDebtPaymentPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterDebtPaymentResponseDto register(
            @PathVariable Long accountId,
            @PathVariable Long debtId,
            @Valid @RequestBody RegisterDebtPaymentRequest request
    ) {
        return DebtPaymentRestMapper.toDto(registerDebtPaymentPort.registerDebtPayment(DebtPaymentRestMapper.toCommand(accountId, debtId, request)));
    }

    @GetMapping
    public PageResponseDto<DebtPaymentResponseDto> list(
            @PathVariable Long accountId,
            @PathVariable Long debtId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) DebtPaymentTypeDto paymentType,
            @RequestParam(required = false) DebtPaymentStatusDto status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        var query = new ListDebtPaymentsQuery(
                accountId,
                debtId,
                from,
                to,
                paymentType == null ? null : DebtPaymentType.valueOf(paymentType.name()),
                status == null ? null : DebtPaymentStatus.valueOf(status.name()),
                PageQuery.of(page, size),
                sort
        );
        return DebtPaymentRestMapper.toDto(listDebtPaymentsPort.listDebtPayments(query), DebtPaymentRestMapper::toDto);
    }

    @GetMapping("/{paymentId}")
    public DebtPaymentResponseDto get(@PathVariable Long accountId, @PathVariable Long debtId, @PathVariable Long paymentId) {
        return DebtPaymentRestMapper.toDto(getDebtPaymentPort.getDebtPayment(accountId, debtId, paymentId));
    }
}
