# ADR 0004 - Identity, Participant, Account Roles

## Status

Accepted.

## Context

Easy Finance separates authentication identity from financial participation.

The documentation establishes that:

- A user authenticates into the system.
- A participant operates in financial accounts.
- A participant belongs to one or many accounts.
- Roles inside accounts are functional and account-scoped.

The final decision is that `User` and `Participant` have a mandatory 1:1 relationship.

## Decision

Use both global roles and account roles.

Global roles:

- `SUPER_ADMIN`
- `USER`

Account roles:

- `ACCOUNT_ADMIN`
- `ACCOUNT_MEMBER`

Financial authorization is primarily based on account membership and account role.

## Consequences

Positive:

- Clear separation between technical security and business authorization.
- Supports multi-account collaboration.
- Prevents global role misuse for account operations.
- Keeps future permission expansion possible.

Negative:

- Every financial operation must validate account membership.
- Authorization logic must be consistently applied across use cases.

## Rules

- Every `User` must have exactly one `Participant`.
- Every financial operation must resolve the authenticated user to a participant.
- Every financial operation must validate that the participant belongs to the target account.
- Account role determines whether the participant can perform a specific business action.
- `SUPER_ADMIN` is a technical role and should not bypass financial business rules unless explicitly designed.

