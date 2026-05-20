# Security Model

## Overview

Easy Finance uses Spring Security with JWT.

Security has two authorization levels:

- Global technical authorization.
- Account-scoped functional authorization.

Financial operations are primarily authorized by account membership and account role.

## Authentication

The client authenticates with credentials and receives a JWT.

JWT should include:

- subject: user id or email.
- global roles.
- expiration.
- issued at.
- token id when needed.

Recommended claims:

```json
{
  "sub": "123",
  "userId": 123,
  "participantId": 456,
  "email": "user@example.com",
  "globalRoles": ["USER"],
  "iat": 1778240000,
  "exp": 1778243600
}
```

Do not include all account memberships in the JWT for the MVP. Account authorization should be loaded from the database to avoid stale role decisions.

## Passwords

- Store only password hashes.
- Use BCrypt through Spring Security.
- Never log passwords or raw tokens.
- Passwords must contain at least 8 characters, one letter, and one number.

## Phase 2 Auth Endpoints

- `POST /api/v1/auth/register`: creates an ACTIVE user, ACTIVE participant, assigns `USER`, and returns a Bearer token.
- `POST /api/v1/auth/login`: validates credentials and ACTIVE status, then returns a Bearer token.
- `GET /api/v1/auth/me`: returns authenticated user and participant data from the token/database.

Refresh tokens are outside the current phase.

## Global Roles

### SUPER_ADMIN

Technical administration role.

Allowed examples:

- manage global roles
- inspect system health if exposed
- support administrative workflows

Important rule:

`SUPER_ADMIN` should not automatically bypass account financial authorization unless a use case explicitly says so.

### USER

Default authenticated user role.

Allowed examples:

- access owned/account-member financial resources
- create accounts
- operate inside accounts where membership exists

## Account Roles

### ACCOUNT_ADMIN

Account-level administrator.

Recommended permissions:

- manage account metadata
- invite/add participants
- change participant roles
- manage categories
- manage payment methods
- create and update budgets
- register expenses
- create debts
- register debt payments
- import expenses
- view reports

### ACCOUNT_MEMBER

Account-level operational member.

Recommended permissions:

- view account financial data
- register expenses
- create debts if allowed by business policy
- register debt payments
- view reports

Restricted examples:

- change account roles
- remove participants
- manage sensitive account configuration

## Account Membership Authorization

Every account-scoped operation must validate:

1. JWT is valid.
2. User is active.
3. User has an active participant.
4. Participant has active membership in the account.
5. Membership has the required account role.

Use application-layer authorization ports.

Example:

```text
AccountAuthorizationPort.requireRole(accountId, participantId, ACCOUNT_ADMIN)
```

For operations that allow either role:

```text
AccountAuthorizationPort.requireAnyRole(accountId, participantId, ACCOUNT_ADMIN, ACCOUNT_MEMBER)
```

## Suggested Permission Matrix

| Operation | ACCOUNT_ADMIN | ACCOUNT_MEMBER |
| --- | --- | --- |
| View account | Yes | Yes |
| Update account | Yes | No |
| Add participant | Yes | No |
| Change participant role | Yes | No |
| Manage categories | Yes | No |
| Manage payment methods | Yes | No |
| Create expense | Yes | Yes |
| Create installment expense | Yes | Yes |
| Create manual debt | Yes | Yes, pending confirmation |
| Register debt payment | Yes | Yes |
| Manage monthly budgets | Yes | No |
| View budgets | Yes | Yes |
| Import Excel expenses | Yes | Pending confirmation |
| View analytics/reports | Yes | Yes |

## Endpoint Security

Use method-level authorization only for coarse checks.

Business/account authorization should be enforced in use cases to keep it testable and consistent.

Example:

```text
Controller authenticates request
Use case resolves current participant
Use case checks account membership and role
Use case executes domain operation
```

## Data Isolation

Queries for account-scoped data must always include `account_id`.

Do not fetch financial data only by entity id if account context is available. Prefer:

```text
findByIdAndAccountId(entityId, accountId)
```

## Audit and Security

Security-sensitive actions must create functional audit events:

- login failures when implemented
- account role changes
- participant additions/removals
- expense import confirmation
- critical financial writes

## Pending Decisions

- JWT expiration and refresh-token strategy.
- Whether `ACCOUNT_MEMBER` can create manual debts and imports.
- Whether `SUPER_ADMIN` can access account data for support workflows.
