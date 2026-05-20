package com.easyfinance.shared.domain;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredAt();
}

