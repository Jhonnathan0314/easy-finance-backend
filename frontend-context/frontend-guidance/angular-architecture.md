# Angular Architecture

Use the latest stable Angular version available in the target environment.

## Recommended Shape

- Standalone components.
- Lazy-loaded feature routes.
- `HttpClient` for API access.
- Auth interceptor for Bearer JWT.
- Error interceptor for `ApiErrorResponse`.
- Route guards for authenticated app routes.
- Feature services by API area.
- TypeScript interfaces generated from OpenAPI or maintained manually from `models/dto-reference.md`.
- Reactive forms.
- Signals for local UI state; RxJS for async API streams and cross-component flows.

## Suggested Folder Layout

```text
src/app/
  core/
    auth/
    http/
    guards/
    layout/
  shared/
    ui/
    forms/
    models/
    utils/
  features/
    auth/
    accounts/
    catalogs/
    expenses/
    debts/
    budgets/
    income/
    analytics/
    imports/
```

## HTTP Layer

- `AuthInterceptor`: attaches `Authorization: Bearer <token>`.
- `ErrorInterceptor`: normalizes backend `ApiErrorResponse`.
- `CorrelationIdInterceptor` optional: read and display correlation ID for support.

## State

Keep state lean:

- Auth user/token in `AuthStore`.
- Selected account in `AccountStore`.
- Catalog cache per account.
- Current filters in each feature route.

Avoid global state for every table row; refetch on route changes or after mutations.

