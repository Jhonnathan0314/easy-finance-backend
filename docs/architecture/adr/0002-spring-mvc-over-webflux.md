# ADR 0002 - Spring MVC over WebFlux

## Status

Accepted.

## Context

The original backend discussion considered WebFlux. Easy Finance has several characteristics that make traditional Spring MVC more appropriate:

- Financial transaction consistency.
- Complex domain rules.
- Audit requirements.
- Excel import processing.
- Report generation.
- JPA persistence.
- Simpler debugging and operations.

## Decision

Use Spring MVC as the primary web stack. Do not use WebFlux as the main application architecture.

## Consequences

Positive:

- Simpler request handling.
- Mature Spring Data JPA integration.
- Easier transaction management.
- Easier debugging for the team.
- Better fit for blocking libraries such as Excel processing and report generation.

Negative:

- Less suitable for extremely high concurrency I/O workloads.
- Thread-per-request model requires conventional capacity planning.

## Alternatives Considered

### Spring WebFlux

Rejected for MVP because it would increase complexity without clear benefit for the current workload.

### Hybrid MVC + WebFlux

Not selected for the baseline. A hybrid approach can be reconsidered later for specific streaming/reporting use cases if there is evidence.

