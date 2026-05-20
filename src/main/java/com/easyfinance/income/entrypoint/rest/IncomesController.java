package com.easyfinance.income.entrypoint.rest;

import com.easyfinance.income.application.port.in.CancelIncomePort;
import com.easyfinance.income.application.port.in.CreateIncomePort;
import com.easyfinance.income.application.port.in.DuplicateIncomePort;
import com.easyfinance.income.application.port.in.GetIncomePort;
import com.easyfinance.income.application.port.in.ListIncomesPort;
import com.easyfinance.income.application.port.in.UpdateIncomePort;
import com.easyfinance.income.application.query.ListIncomesQuery;
import com.easyfinance.income.domain.model.IncomeStatus;
import com.easyfinance.income.entrypoint.rest.dto.CreateIncomeRequest;
import com.easyfinance.income.entrypoint.rest.dto.DuplicateIncomeRequest;
import com.easyfinance.income.entrypoint.rest.dto.IncomeResponseDto;
import com.easyfinance.income.entrypoint.rest.dto.IncomeStatusDto;
import com.easyfinance.income.entrypoint.rest.dto.PageResponseDto;
import com.easyfinance.income.entrypoint.rest.dto.UpdateIncomeRequest;
import com.easyfinance.income.entrypoint.rest.mapper.IncomeRestMapper;
import com.easyfinance.shared.application.PageQuery;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/incomes")
public class IncomesController {

    private final CreateIncomePort createIncomePort;
    private final ListIncomesPort listIncomesPort;
    private final GetIncomePort getIncomePort;
    private final UpdateIncomePort updateIncomePort;
    private final CancelIncomePort cancelIncomePort;
    private final DuplicateIncomePort duplicateIncomePort;

    public IncomesController(
            CreateIncomePort createIncomePort,
            ListIncomesPort listIncomesPort,
            GetIncomePort getIncomePort,
            UpdateIncomePort updateIncomePort,
            CancelIncomePort cancelIncomePort,
            DuplicateIncomePort duplicateIncomePort
    ) {
        this.createIncomePort = createIncomePort;
        this.listIncomesPort = listIncomesPort;
        this.getIncomePort = getIncomePort;
        this.updateIncomePort = updateIncomePort;
        this.cancelIncomePort = cancelIncomePort;
        this.duplicateIncomePort = duplicateIncomePort;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeResponseDto create(@PathVariable Long accountId, @Valid @RequestBody CreateIncomeRequest request) {
        return IncomeRestMapper.toDto(createIncomePort.createIncome(IncomeRestMapper.toCommand(accountId, request)));
    }

    @GetMapping
    public PageResponseDto<IncomeResponseDto> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) IncomeStatusDto status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        var query = new ListIncomesQuery(
                accountId,
                from,
                to,
                categoryId,
                participantId,
                status == null ? null : IncomeStatus.valueOf(status.name()),
                search,
                PageQuery.of(page, size),
                sort
        );
        return IncomeRestMapper.toDto(listIncomesPort.listIncomes(query), IncomeRestMapper::toDto);
    }

    @GetMapping("/{incomeId}")
    public IncomeResponseDto get(@PathVariable Long accountId, @PathVariable Long incomeId) {
        return IncomeRestMapper.toDto(getIncomePort.getIncome(accountId, incomeId));
    }

    @PutMapping("/{incomeId}")
    public IncomeResponseDto update(
            @PathVariable Long accountId,
            @PathVariable Long incomeId,
            @Valid @RequestBody UpdateIncomeRequest request
    ) {
        return IncomeRestMapper.toDto(updateIncomePort.updateIncome(IncomeRestMapper.toCommand(accountId, incomeId, request)));
    }

    @PatchMapping("/{incomeId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long accountId, @PathVariable Long incomeId) {
        cancelIncomePort.cancelIncome(accountId, incomeId);
    }

    @PostMapping("/{incomeId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeResponseDto duplicate(
            @PathVariable Long accountId,
            @PathVariable Long incomeId,
            @Valid @RequestBody DuplicateIncomeRequest request
    ) {
        return IncomeRestMapper.toDto(duplicateIncomePort.duplicateIncome(IncomeRestMapper.toCommand(accountId, incomeId, request)));
    }
}
