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
- Passwords must contain at least 8 characters, one letter, and one number. This complexity rule is enforced in
  the application layer (`RegisterUserUseCase.validatePassword`), not only through `@Size` on `RegisterRequest`.

## Phase 2 Auth Endpoints

- `POST /api/v1/auth/register`: creates an ACTIVE user, ACTIVE participant, assigns `USER`, returns a Bearer token,
  and sets an httpOnly `refreshToken` cookie.
- `POST /api/v1/auth/login`: validates credentials and ACTIVE status, then returns a Bearer token and sets the
  same refresh cookie.
- `GET /api/v1/auth/me`: returns authenticated user and participant data from the token/database.
- `PUT /api/v1/auth/me`: updates the authenticated user's `fullName` (and the linked participant's `displayName`).
- `POST /api/v1/auth/refresh`: reads the `refreshToken` cookie, rotates it (single-use), and returns a new access
  token plus a new refresh cookie. Public endpoint (no Bearer token required - it authenticates via the cookie).
- `POST /api/v1/auth/logout`: revokes the presented refresh token server-side and clears the cookie. Public
  endpoint; safe to call without a cookie present.

## Refresh Tokens

Implemented (see `docs/implementation/refresh-token-plan.md` for the full design rationale):

- The refresh token is a random 256-bit value, transported only as an httpOnly, `Secure`, `SameSite=None` cookie
  scoped to `/api/v1/auth` - it is never present in any JSON response body and never readable by JavaScript.
- Only its SHA-256 hash is persisted, in the `refresh_tokens` table (`shared/infrastructure/security`).
- **Rotation with reuse detection**: every successful refresh revokes the presented token and issues a new one in
  the same "family" (`family_id`). If an already-revoked token is ever presented again (a signal of token theft or
  a replayed request), the entire family is revoked immediately, forcing re-login.
- Default lifetime is 30 days (`easy-finance.security.refresh-token.expiration`), independent of the access
  token's 1-hour lifetime (`easy-finance.security.jwt.expiration`).
- `POST /auth/logout` revokes only the single presented token (not the whole family) - there is no
  "logout everywhere" endpoint yet; the data model supports adding one later via a query on `user_id`.
- CORS is already configured with `allowCredentials(true)` and explicit allowed origins (never `*`), which is a
  prerequisite for the browser to send/accept this cookie cross-origin.

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
| Create manual debt | Yes | Yes |
| Register debt payment | Yes | Yes |
| Manage monthly budgets | Yes | No |
| Duplicate/create annual budget | Yes | No |
| View budgets | Yes | Yes |
| Import expenses (Excel) | Yes | Yes |
| Import income (Excel) | Yes | Yes |
| Import categories/payment methods (Excel) | Yes | No |
| Import annual budget (Excel) | Yes | No |
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

Security-sensitive actions should create functional audit events once that capability is implemented (see
`docs/audit/audit-strategy.md`); this is not enforced by any code today:

- login failures when implemented
- account role changes
- participant additions/removals
- expense import confirmation
- critical financial writes

## Pending Decisions

- Whether `SUPER_ADMIN` can access account data for support workflows.
- Whether a "logout everywhere" endpoint (revoking every refresh token for a user, not just the presented one) is
  needed.

Resolved: `ACCOUNT_MEMBER` can create manual debts and import expenses/income (`requireActiveMemberForActiveAccount`
in `DebtManagementUseCase`, `ExpenseImportManagementUseCase`, `IncomeImportUseCase`). Category, payment method, and
annual budget Excel imports require `ACCOUNT_ADMIN` (`requireActiveAdminForActiveAccount` in `CategoryImportUseCase`,
`PaymentMethodImportUseCase`, `BudgetImportUseCase`). There are no `@PreAuthorize`/`@Secured` annotations; all of
this is enforced in application-layer use cases via `AccountAuthorizationService`.
