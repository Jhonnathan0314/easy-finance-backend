# Roles And Permissions

| Action | ACCOUNT_ADMIN | ACCOUNT_MEMBER | Owner | Special Rules |
|---|---:|---:|---:|---|
| List own accounts | Yes | Yes | N/A | Only active memberships. |
| View account | Yes | Yes | N/A | Non-member sees `ACCOUNT_NOT_FOUND`. |
| Update account | Yes | No | N/A | Account must be `ACTIVE`. |
| Archive account | Yes | No | N/A | Account becomes read-mostly. |
| List members | Yes | Yes | N/A | Current member required. |
| Add member | Yes | No | N/A | Target user/participant must be active. |
| Change member role | Yes | No | N/A | Cannot demote last active admin. |
| Remove member | Yes | No | N/A | Cannot remove last active admin. Self-removal allowed only if another admin remains. |
| List categories/payment methods | Yes | Yes | N/A | Current member required. |
| Create/update/deactivate category | Yes | No | N/A | Account must be `ACTIVE`. |
| Create/update/deactivate payment method | Yes | No | N/A | Account must be `ACTIVE`. |
| Create simple expense | Yes | Yes | N/A | Category/payment method must be active and same account. |
| List/view expenses | Yes | Yes | N/A | Current member required. |
| Update simple expense | Yes | No | Yes | `INSTALLMENT` expense update is blocked. |
| Cancel simple expense | Yes | No | Yes | `INSTALLMENT` expense cancel is blocked. |
| Create installment expense | Yes | Yes | N/A | Creates debt and budget impacts transactionally. |
| Create manual debt | Yes | Yes | N/A | Account must be `ACTIVE`. |
| List/view debts | Yes | Yes | N/A | Current member required. |
| Cancel manual debt | Yes | No | Yes | Derived debt cancellation is blocked in MVP. |
| Register debt payment | Yes | Yes | N/A | Debt must be `ACTIVE`; no overpayment. |
| List/view debt payments | Yes | Yes | N/A | Current member required. |
| Upsert budget | Yes | No | N/A | Account must be `ACTIVE`; year 2000-2100, month 1-12. |
| View/list budgets | Yes | Yes | N/A | Current member required. |
| Create/update/deactivate manual sub-budget | Yes | No | N/A | Derived sub-budgets are not editable manually. |
| Create income | Yes | Yes | N/A | Category must be `INCOME`, active, same account. |
| Update/cancel income | Yes | No | Yes | Cancelled income cannot be updated. |
| View analytics | Yes | Yes | N/A | Read-only. |
| Preview expense import | Yes | Yes | N/A | Account must be `ACTIVE`. |
| Confirm expense import | Yes | Yes | N/A | Batch must be `PREVIEW`; valid rows only. |
| Get import batch | Yes | Yes | N/A | Batch must belong to account. |

