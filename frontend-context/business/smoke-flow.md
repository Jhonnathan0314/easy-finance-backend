# Smoke Flow

Use this flow to validate the frontend wiring against a local backend.

## 1. Register

`POST /api/v1/auth/register`

Expected: `201`, JWT and user with `participantId`.

## 2. Login

`POST /api/v1/auth/login`

Expected: `200`, JWT. Store token and attach as Bearer.

## 3. Create Account

`POST /api/v1/accounts`

Expected: account `ACTIVE`, `currentUserRole = ACCOUNT_ADMIN`.

## 4. Create EXPENSE Category

`POST /api/v1/accounts/{accountId}/categories`

Body type: `EXPENSE`

Expected: category `ACTIVE`.

## 5. Create INCOME Category

`POST /api/v1/accounts/{accountId}/categories`

Body type: `INCOME`

Expected: category `ACTIVE`.

## 6. Create Payment Method

`POST /api/v1/accounts/{accountId}/payment-methods`

Expected: payment method `ACTIVE`.

## 7. Create Simple Expense

`POST /api/v1/accounts/{accountId}/expenses`

Expected: expense `ACTIVE`, `expenseType = SIMPLE`.

## 8. Create Installment Expense

`POST /api/v1/accounts/{accountId}/expenses/installments`

Expected: expense `ACTIVE`, `expenseType = INSTALLMENT`.

## 9. Get Debts

`GET /api/v1/accounts/{accountId}/debts`

Expected: one `INSTALLMENT_EXPENSE` debt for the installment expense.

## 10. Register Debt Payment

`POST /api/v1/accounts/{accountId}/debts/{debtId}/payments`

Expected: payment created; debt remaining amount reduced.

## 11. Get Budget Summary

`GET /api/v1/accounts/{accountId}/analytics/budget-summary?year=2026&month=6`

Expected: expected amount from generated impact; paid amount reflects payment.

## 12. Create Income

`POST /api/v1/accounts/{accountId}/incomes`

Expected: income `ACTIVE`.

## 13. Get Analytics

`GET /api/v1/accounts/{accountId}/analytics/monthly-summary?year=2026&month=5`

Expected: income, expenses, net balance, and debt metrics.

## 14. Preview Import Excel

`POST /api/v1/accounts/{accountId}/imports/expenses/preview`

Multipart field: `file`

Expected: batch `PREVIEW`, row validation summary.

## 15. Confirm Import Excel

`POST /api/v1/accounts/{accountId}/imports/expenses/{batchId}/confirm`

Expected: batch `CONFIRMED`, valid rows have `createdExpenseId`.

