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

Keep filters route-local:

- expenses filters
- debt filters
- budget year/month
- income filters
- analytics date ranges
- import batch ID/status

Use query params for shareable table filters where helpful.

## Loading And Errors

Use a small global HTTP pending indicator and feature-level loading states.

Map backend errors by `code` first, status second.

