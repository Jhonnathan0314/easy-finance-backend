# Auth Flow

## Register

`POST /api/v1/auth/register`

Creates a user and participant, assigns global `USER`, and returns an access token.

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "fullName": "Demo User"
}
```

## Login

`POST /api/v1/auth/login`

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Response:

```json
{
  "accessToken": "jwt-value",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "userId": 1,
    "participantId": 1,
    "email": "user@example.com",
    "fullName": "Demo User",
    "globalRoles": ["USER"]
  }
}
```

## Bearer Token

Store the token in a safe client-side auth service. For every protected request:

```http
Authorization: Bearer <accessToken>
```

## Refresh Token

`login` and `register` also set an httpOnly `refreshToken` cookie (scoped to `/api/v1/auth`, `Secure`,
`SameSite=None`) - it is never present in any JSON response body and is not readable by JavaScript. Requests to
the refresh/logout endpoints must be made with `withCredentials: true` so the browser sends/accepts this cookie.

`POST /api/v1/auth/refresh`

No request body; the refresh token travels via the cookie. Returns the same shape as login/register (a new
access token + user) and rotates the cookie to a new refresh token (the old one becomes invalid - it is
single-use). Public endpoint (no `Authorization` header needed).

Error codes: `INVALID_REFRESH_TOKEN` (missing/unknown cookie), `REFRESH_TOKEN_EXPIRED`, `REFRESH_TOKEN_REUSED`
(an already-used token was presented again - treat this the same as any other failed refresh: clear the session
and redirect to login).

Recommended frontend pattern: when a request fails with `401 TOKEN_EXPIRED`, call `POST /auth/refresh` once,
then retry the original request with the new access token. Concurrent 401s should share a single in-flight
refresh call instead of each triggering their own.

`POST /api/v1/auth/logout`

No request body. Revokes the refresh token presented via the cookie (if any) and clears the cookie. Always
succeeds (204), even without a cookie present. Public endpoint.

## Current User

`GET /api/v1/auth/me`

Use this after app boot or refresh to rehydrate auth state. The backend also revalidates active user/participant status for authenticated endpoints.

## Update Profile

`PUT /api/v1/auth/me`

```json
{
  "fullName": "Jane Smith"
}
```

Updates the authenticated user's `fullName` and the linked participant's `displayName` to the same value in one
transaction - they are never allowed to diverge. Returns the same shape as `GET /auth/me`. `fullName` is required
(max 150 characters); a blank value returns `FULL_NAME_REQUIRED` via the standard validation error format. Email
and password are not editable through this endpoint.

## Expiration

When the access token expires, backend returns `401` with `code: TOKEN_EXPIRED`. Frontend should attempt
`POST /auth/refresh` once and retry the original request with the new access token (see "Refresh Token" above).
Only if the refresh itself fails should the frontend clear auth state and redirect to `/login`.

## 401 And 403 Handling

- `401 Unauthorized`: missing or invalid token -> redirect to login immediately. Expired token (`TOKEN_EXPIRED`)
  -> attempt a refresh first (see above); only redirect to login if the refresh also fails.
- `403 Forbidden`: authenticated but not allowed, blocked/inactive user, inactive participant, insufficient account role, or business authorization failure. Show an actionable message.

Known hardening codes:

- `USER_BLOCKED`
- `USER_NOT_ACTIVE`
- `PARTICIPANT_NOT_ACTIVE`
- `ACCOUNT_ADMIN_REQUIRED`
- `ACCOUNT_NOT_FOUND` for hidden cross-account access.

