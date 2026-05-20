package com.easyfinance.analytics.entrypoint.rest.dto;

import java.time.LocalDate;
import java.util.List;

public record CashflowResponseDto(
        Long accountId,
        LocalDate from,
        LocalDate to,
        String groupBy,
        List<CashflowItemDto> items
) {
}
