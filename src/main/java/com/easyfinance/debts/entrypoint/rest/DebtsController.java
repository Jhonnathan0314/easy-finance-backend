package com.easyfinance.debts.entrypoint.rest;

import com.easyfinance.debts.application.port.in.CancelDebtPort;
import com.easyfinance.debts.application.port.in.CreateManualDebtPort;
import com.easyfinance.debts.application.port.in.GetDebtPort;
import com.easyfinance.debts.application.port.in.ListDebtsPort;
import com.easyfinance.debts.application.query.ListDebtsQuery;
import com.easyfinance.debts.domain.model.DebtSourceType;
import com.easyfinance.debts.domain.model.DebtState;
import com.easyfinance.debts.entrypoint.rest.dto.CreateManualDebtRequest;
import com.easyfinance.debts.entrypoint.rest.dto.DebtResponseDto;
import com.easyfinance.debts.entrypoint.rest.dto.DebtSourceTypeDto;
import com.easyfinance.debts.entrypoint.rest.dto.DebtStateDto;
import com.easyfinance.debts.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.debts.entrypoint.rest.mapper.DebtRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/debts")
public class DebtsController {

    private final CreateManualDebtPort createManualDebtPort;
    private final ListDebtsPort listDebtsPort;
    private final GetDebtPort getDebtPort;
    private final CancelDebtPort cancelDebtPort;

    public DebtsController(
            CreateManualDebtPort createManualDebtPort,
            ListDebtsPort listDebtsPort,
            GetDebtPort getDebtPort,
            CancelDebtPort cancelDebtPort
    ) {
        this.createManualDebtPort = createManualDebtPort;
        this.listDebtsPort = listDebtsPort;
        this.getDebtPort = getDebtPort;
        this.cancelDebtPort = cancelDebtPort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebtResponseDto create(@PathVariable Long accountId, @Valid @RequestBody CreateManualDebtRequest request) {
        return DebtRestMapper.toDto(createManualDebtPort.createManualDebt(DebtRestMapper.toCommand(accountId, request)));
    }

    @GetMapping
    public PageResponseDto<DebtResponseDto> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) DebtStateDto state,
            @RequestParam(required = false) DebtSourceTypeDto sourceType,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        var query = new ListDebtsQuery(
                accountId,
                state == null ? null : DebtState.valueOf(state.name()),
                sourceType == null ? null : DebtSourceType.valueOf(sourceType.name()),
                participantId,
                from,
                to,
                PageQuery.of(page, size),
                sort
        );
        return DebtRestMapper.toDto(listDebtsPort.listDebts(query), DebtRestMapper::toDto);
    }

    @GetMapping("/{debtId}")
    public DebtResponseDto get(@PathVariable Long accountId, @PathVariable Long debtId) {
        return DebtRestMapper.toDto(getDebtPort.getDebt(accountId, debtId));
    }

    @PatchMapping("/{debtId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long accountId, @PathVariable Long debtId) {
        cancelDebtPort.cancelDebt(accountId, debtId);
    }
}
