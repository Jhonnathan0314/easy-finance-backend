# State Management Plan

## Auth State

Fields:

- `accessToken`
- `expiresAt`
- `user`
- `isAuthenticated`

Actions:

- register
- login
- logout
- load current user
- handle token expired

## Selected Account State

Fields:

- `accounts`
- `selectedAccountId`
- `selectedAccount`
- `currentUserRole`

Persist selected account ID in local storage only as a convenience. Always validate by refetching account/list after login.

## Catalog Cache

Cache active categories and payment methods by account:

- expense categories
- income categories
- payment methods

Invalidate after create/update/deactivate.

## Feature Filter State

Filters are persisted per feature and per account in `localStorage` through a shared `FeatureFilterStorageService`
(`getFilters`/`setFilters`/`clearFilters`), under the key `easyFinance.filters.<feature>.<accountId>`:

- expenses filters
- debt filters
- budget year/month
- income filters
- analytics date ranges
- import batch ID/status

This makes filters durable across reloads/navigation for the same browser, but they are not currently shareable
through the URL (no query params are read or written for these filters). Using query params for shareable table
filters remains a possible future improvement, not the current behavior.

## Loading And Errors

Use a small global HTTP pending indicator and feature-level loading states.

Map backend errors by `code` first, status second.

