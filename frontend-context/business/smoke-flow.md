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

Optional check: send `createExpense=true` with `categoryId`, `paymentMethodId`, and `expenseDescription`.
Expected: response includes `createdExpenseId`; the associated expense has `sourceType = DEBT_PAYMENT`
and cashflow still counts the payment only once.

## 11. Get Budget Summary

`GET /api/v1/accounts/{accountId}/analytics/budget-summary?year=2026&month=6`

Expected: expected amount includes manual sub-budget planned amounts and generated impacts; paid/spent
amount includes manual execution from active simple expenses plus paid debt impacts without double counting
`DEBT_PAYMENT` expenses.

## 12. Create Income

`POST /api/v1/accounts/{accountId}/incomes`

Expected: income `ACTIVE`.

## 13. Get Analytics

`GET /api/v1/accounts/{accountId}/analytics/monthly-summary?year=2026&month=5`

Expected: income, expenses, net balance, and debt metrics.

Additional dashboard checks:

`GET /api/v1/accounts/{accountId}/analytics/budget-vs-expenses-by-category?year=2026&month=5`

Expected: categories with budget only, expense only, or both are returned.

## 14. Preview Import Excel

Download the dynamic template first:

`GET /api/v1/accounts/{accountId}/imports/expenses/template`

Expected: workbook contains expense headers, catalog dropdowns, and optional debt-payment columns.

`POST /api/v1/accounts/{accountId}/imports/expenses/preview`

Multipart field: `file`

Expected: batch `PREVIEW`, row validation summary. Rows with `AplicaPagoDeuda = SI` resolve an active
debt through backend validation.

## 15. Confirm Import Excel

`POST /api/v1/accounts/{accountId}/imports/expenses/{batchId}/confirm`

Expected: batch `CONFIRMED`, valid rows have `createdExpenseId`; debt-payment rows also have
`createdDebtPaymentId`.
