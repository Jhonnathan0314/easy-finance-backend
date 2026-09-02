# Error Contract

All errors use `ApiErrorResponse`.

```ts
interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  correlationId: string | null;
  details: FieldErrorResponse[] | null;
}

interface FieldErrorResponse {
  field: string;
  message: string;
}
```

`error` is `HttpStatus.name()` (`GlobalExceptionHandler`/`RestSecurityExceptionHandler`), so it is always SCREAMING_SNAKE_CASE (`BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `UNPROCESSABLE_ENTITY`, `PAYLOAD_TOO_LARGE`), never the HTTP reason phrase (`"Bad Request"`).

## Validation Error

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "path": "/api/v1/accounts",
  "correlationId": "c-123",
  "details": [
    { "field": "name", "message": "Account name is required." }
  ]
}
```

## Unauthorized

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "code": "INVALID_TOKEN",
  "message": "Invalid authentication token.",
  "path": "/api/v1/accounts",
  "correlationId": "c-123",
  "details": []
}
```

## Forbidden

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 403,
  "error": "FORBIDDEN",
  "code": "ACCOUNT_ADMIN_REQUIRED",
  "message": "Account admin role is required.",
  "path": "/api/v1/accounts/1/categories",
  "correlationId": "c-123",
  "details": []
}
```

## Not Found

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "code": "ACCOUNT_NOT_FOUND",
  "message": "Account was not found.",
  "path": "/api/v1/accounts/99",
  "correlationId": "c-123",
  "details": []
}
```

## Business Rule

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 422,
  "error": "UNPROCESSABLE_ENTITY",
  "code": "DEBT_PAYMENT_EXCEEDS_REMAINING_BALANCE",
  "message": "Debt payment exceeds remaining balance.",
  "path": "/api/v1/accounts/1/debts/1/payments",
  "correlationId": "c-123",
  "details": []
}
```

`BusinessRuleViolationException` always maps to `422 Unprocessable Entity`. The backend does not use `409 Conflict` for business rule violations.

## Malformed JSON

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "code": "MALFORMED_JSON",
  "message": "Malformed JSON request.",
  "path": "/api/v1/accounts",
  "correlationId": "c-123",
  "details": []
}
```

## Import File Too Large

```json
{
  "timestamp": "2026-05-12T17:00:00Z",
  "status": 413,
  "error": "PAYLOAD_TOO_LARGE",
  "code": "IMPORT_FILE_TOO_LARGE",
  "message": "Import file exceeds the configured maximum size.",
  "path": "/api/v1/accounts/1/imports/expenses/preview",
  "correlationId": "c-123",
  "details": []
}
```

