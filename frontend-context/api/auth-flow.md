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

## Current User

`GET /api/v1/auth/me`

Use this after app boot or refresh to rehydrate auth state. The backend also revalidates active user/participant status for authenticated endpoints.

## Expiration

When the token expires, backend returns `401` with a token-related code such as `TOKEN_EXPIRED`. Frontend should clear auth state and redirect to `/login`.

## 401 And 403 Handling

- `401 Unauthorized`: missing, invalid, or expired token. Redirect to login.
- `403 Forbidden`: authenticated but not allowed, blocked/inactive user, inactive participant, insufficient account role, or business authorization failure. Show an actionable message.

Known hardening codes:

- `USER_BLOCKED`
- `USER_NOT_ACTIVE`
- `PARTICIPANT_NOT_ACTIVE`
- `ACCOUNT_ADMIN_REQUIRED`
- `ACCOUNT_NOT_FOUND` for hidden cross-account access.

