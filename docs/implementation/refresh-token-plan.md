# Refresh Token - Implementation Plan

## Objective

Today the app issues a single JWT access token (HS256, 1h lifetime via `JWT_EXPIRATION`) with no refresh
mechanism at all - no session/refresh-token table, no `/auth/refresh`, no `/auth/logout` on the backend. When the
access token expires, the frontend clears the whole session and forces a full re-login. This plan adds a
revocable, rotating refresh token so users stay signed in across the access token's short lifetime without
re-entering credentials, while keeping the ability to truly invalidate a session server-side (logout, token-theft
detection).

## Decisions Already Made

Confirmed with the product owner before drafting this plan:

- **Refresh token travels as an httpOnly cookie**, not in the JSON body/`localStorage`. It is never readable by
  JavaScript, mitigating XSS exfiltration (the access token itself remains in `localStorage` as today - only the
  long-lived credential moves to a cookie).
- **Server-side revocation with single-use rotation.** Every refresh call invalidates the presented token and
  issues a new one. Presenting an already-used (revoked) token is treated as token theft: the entire token
  "family" (the chain of rotations since the original login) is revoked, forcing re-login.
- Access token behavior (1h JWT, claims, validation) is **unchanged** - this plan only adds what happens after it
  expires.

## Out Of Scope

- "Logout everywhere" / listing active sessions per user (the data model supports it trivially later via a query
  on `user_id`, but no endpoint is added now).
- Changing access token claims, algorithm, or lifetime.
- Multi-factor authentication, device fingerprinting, or IP-binding of refresh tokens.
- Refresh token lifetime is set to a sensible default (30 days) in this plan; treat as an easy config tweak, not a
  blocking decision.

## Backend Design

Follows the existing placement precedent: `JwtTokenService`, `JwtAuthenticationFilter`, and `SecurityConfig` all
live under `shared/infrastructure/security/` with no domain/application split, because they are cross-cutting
security infrastructure, not a business module. Refresh token issuance/rotation follows the same placement.

### New table

`V21__refresh_tokens_schema.sql`:

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    family_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    replaced_by_token_hash VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
```

- `token_hash`: SHA-256 hex of the raw token (same principle as password hashing - never store the raw secret).
- `family_id`: a fresh random UUID at login/register; preserved across every rotation of that session so a reuse
  can revoke the whole chain in one `UPDATE ... WHERE family_id = ?`.

### `shared/infrastructure/security/RefreshTokenService.java` (new)

Responsibilities (plain service, no port/adapter split needed - mirrors `JwtTokenService`):

- `issue(Long userId): IssuedRefreshToken` - creates a new `family_id`, generates a random 256-bit token (e.g.
  `SecureRandom` + base64url), stores its hash with `expires_at = now + 30 days`, returns the raw token (only time
  it's ever available in plain form) plus its hash for the cookie/response wiring.
- `rotate(String rawToken): IssuedRefreshToken` - hashes the input, looks up by `token_hash`:
  - Not found -> throw `JwtAuthenticationException("INVALID_REFRESH_TOKEN", ...)`.
  - Found but `revoked_at != null` -> **reuse detected**: revoke every row with the same `family_id` that isn't
    already revoked, then throw `JwtAuthenticationException("REFRESH_TOKEN_REUSED", ...)`.
  - Found, not revoked, `expires_at < now` -> throw `JwtAuthenticationException("REFRESH_TOKEN_EXPIRED", ...)`.
  - Otherwise: mark the row revoked (`revoked_at = now`), issue a new row with the same `family_id` (new hash, new
    `expires_at`), set `replaced_by_token_hash` on the old row, return the new raw token.
- `revoke(String rawToken): void` - used by logout; hashes and marks that single row revoked (no family-wide
  revoke needed for a normal logout).

### `AuthController` changes

New endpoints, same package/style as the existing 4:

```java
@PostMapping("/refresh")
public AuthTokenResponseDto refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
) { ... }

@PostMapping("/logout")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
) { ... }
```

- `refresh`: if the cookie is missing -> `UNAUTHENTICATED`. Otherwise calls `refreshTokenService.rotate(...)`,
  issues a fresh access token via the existing `TokenIssuerPort` (same claims logic as login), sets the new
  refresh cookie (`ResponseCookie` builder: `httpOnly(true)`, `secure(true)`, `sameSite("None")` - see CORS note
  below, `path("/api/v1/auth")`, `maxAge(30 days)`), returns the same `AuthTokenResponse` shape used by
  login/register (no new fields needed - the refresh token never appears in the JSON body).
- `logout`: revokes the presented token (best-effort, ignore "not found") and clears the cookie
  (`ResponseCookie` with `maxAge(0)`).
- `login`/`register`: after building the existing response, also call `refreshTokenService.issue(user.id())` and
  set the same cookie shape.

### CORS/cookie verification (implementation-time check, not a design decision)

Cookies require the backend's CORS config to have `allowCredentials(true)` and an explicit allowed origin (not
`*`), and the frontend to send `withCredentials: true`. Verify `shared/infrastructure/security/*CorsConfig*` (or
wherever CORS is configured) meets this before wiring the cookie - if it currently allows `*` origins, it must be
changed to the concrete frontend origin(s) per environment.

### Error codes (new)

| Code | HTTP status | Trigger |
|---|---|---|
| `INVALID_REFRESH_TOKEN` | 401 | Cookie missing, or token not found |
| `REFRESH_TOKEN_EXPIRED` | 401 | Token found but past `expires_at` |
| `REFRESH_TOKEN_REUSED` | 401 | Token found but already revoked (rotation reuse/theft signal) - entire family revoked as a side effect |

## Frontend Design

### API service

`core/auth/auth-api.service.ts` - add:

```ts
refresh(): Observable<AuthTokenResponseDto> {
  return this.api.post<AuthTokenResponseDto, void>('/auth/refresh', undefined, { withCredentials: true });
}

logout(): Observable<void> {
  return this.api.post<void, void>('/auth/logout', undefined, { withCredentials: true });
}
```

Check `ApiClient`'s wrapper signature for how per-call `withCredentials` is passed today (if it isn't supported
yet, add it there first - every other call can keep `withCredentials: false`/default, only these two need it,
though setting it globally is harmless since browsers only attach cookies matching the cookie's own domain/path).

### Error interceptor - refresh-then-retry

`core/http/error.interceptor.ts` - today ANY 401 clears the session. Change to:

- On 401 with `code === 'TOKEN_EXPIRED'` (the access-token-expired case specifically, not `INVALID_TOKEN` or
  other auth failures): attempt a refresh, then retry the original request once.
- Concurrent requests failing at the same time must share a single in-flight refresh call (a `BehaviorSubject`/
  shared observable guard in `AuthStore`, e.g. `refreshInProgress$`), so N parallel 401s don't trigger N refresh
  calls - the standard Angular "refresh queue" pattern.
- If the refresh call itself fails (any of the 3 new error codes) -> fall back to today's behavior: clear session,
  redirect to `/login`.
- Other 401s (`INVALID_TOKEN`, no token at all) keep today's behavior unchanged (immediate clear + redirect).

### `AuthStore.logout()`

Now also calls `authApi.logout()` (fire, don't block navigation on its result) in addition to clearing local
state, so the server-side token is actually revoked instead of just forgotten locally.

### Local dev note

`Secure` cookies require HTTPS; most browsers special-case `localhost` as a secure context, but this must be
verified against the actual dev setup (Angular dev server + Spring Boot on different ports) before considering
this done - if it doesn't work in local HTTP dev, a per-profile cookie `secure` flag (`false` in a `local`/`dev`
profile, `true` in `prod`) is an acceptable adjustment, mirroring how `JwtProperties`/`ProdJwtPropertiesValidator`
already differ per profile.

## Tests To Cover

**Backend**:
- `RefreshTokenServiceTest`: issue creates a row with a fresh `family_id`; rotate succeeds and revokes the old row
  while preserving `family_id`; rotate with an already-revoked token revokes the whole family and throws
  `REFRESH_TOKEN_REUSED`; rotate with an expired token throws `REFRESH_TOKEN_EXPIRED`; rotate with an unknown
  token throws `INVALID_REFRESH_TOKEN`.
- `AuthControllerTest`: `login`/`register` set the refresh cookie; `POST /auth/refresh` returns a new access token
  and rotates the cookie; `POST /auth/logout` revokes and clears the cookie; missing cookie on refresh ->
  `UNAUTHENTICATED`.
- `AuthControllerSecurityTest`: `/auth/refresh` and `/auth/logout` are reachable without a Bearer token (they
  authenticate via the cookie instead).

**Frontend**:
- `auth-api.service.spec.ts`: `refresh`/`logout` call the right endpoints with `withCredentials: true`.
- `error.interceptor.spec.ts`: a `TOKEN_EXPIRED` 401 triggers one refresh call and retries the original request;
  concurrent 401s share a single refresh call; a failed refresh clears session and redirects; non-`TOKEN_EXPIRED`
  401s still redirect immediately without attempting refresh.
- `auth.store.spec.ts`: `logout()` calls `authApi.logout()` in addition to clearing local state.

## Documentation To Update

- `docs/security/security-model.md`: replace the "refresh tokens outside current phase" / "pending decision" notes
  with the actual design (rotation, family revocation, cookie transport).
- `frontend-context/api/auth-flow.md` / mirrored `docs/api/auth-flow.md`: document `POST /auth/refresh` and
  `POST /auth/logout`.
- `docs/implementation/roadmap.md` and `docs/release/release-candidate-notes.md`: remove refresh tokens from the
  "not implemented" lists.

## Suggested Implementation Order

1. Migration + `RefreshTokenService` (+ JPA entity/repository) with unit tests.
2. `AuthController` wiring (login/register set cookie, new refresh/logout endpoints) + CORS verification, with
   controller/security tests.
3. Frontend API service + refresh-queue logic in the error interceptor, with specs.
4. `AuthStore.logout()` wiring.
5. Documentation sync (both repos).
6. Manual verification of the cookie flow in local dev (HTTP) before considering this done, adjusting the
   `secure` cookie flag per profile if needed.

## Open Questions

None blocking. Refresh token lifetime (30 days) and the absence of a "logout everywhere" endpoint are documented
defaults, easy to revisit later without changing the core design.
