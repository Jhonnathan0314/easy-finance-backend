# Response Examples

## Auth Token

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

## Account

```json
{
  "id": 1,
  "name": "Personal",
  "description": "Main personal finance account",
  "status": "ACTIVE",
  "currentUserRole": "ACCOUNT_ADMIN",
  "createdAt": "2026-05-12T17:00:00Z",
  "updatedAt": "2026-05-12T17:00:00Z"
}
```

## Paginated Response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## Expense

```json
{
  "id": 100,
  "accountId": 1,
  "categoryId": 10,
  "paymentMethodId": 20,
  "participantId": 1,
  "description": "Lunch",
  "amount": 45000.00,
  "currency": "COP",
  "expenseDate": "2026-05-12",
  "paymentState": "PAID",
  "status": "ACTIVE",
  "expenseType": "SIMPLE",
  "sourceType": "MANUAL",
  "sourceDebtPaymentId": null,
  "createdAt": "2026-05-12T17:00:00Z",
  "updatedAt": "2026-05-12T17:00:00Z"
}
```

## Debt

```json
{
  "id": 50,
  "accountId": 1,
  "participantId": 1,
  "originExpenseId": 101,
  "sourceType": "INSTALLMENT_EXPENSE",
  "name": "Laptop installments",
  "description": "Laptop",
  "totalAmount": 1200000.00,
  "totalCurrency": "COP",
  "remainingAmount": 1100000.00,
  "remainingCurrency": "COP",
  "installmentCount": 12,
  "installmentAmount": 100000.00,
  "installmentCurrency": "COP",
  "startDate": "2026-06-01",
  "endDate": "2027-06-01",
  "state": "ACTIVE",
  "notes": "Store purchase",
  "createdAt": "2026-05-12T17:00:00Z",
  "updatedAt": "2026-05-12T17:10:00Z"
}
```

## Budget Detail

```json
{
  "budget": {
    "id": 70,
    "accountId": 1,
    "year": 2026,
    "month": 6,
    "name": "June 2026 budget",
    "status": "ACTIVE",
    "createdAt": "2026-05-12T17:00:00Z",
    "updatedAt": "2026-05-12T17:00:00Z"
  },
  "subBudgets": [
    {
      "id": 80,
      "accountId": 1,
      "budgetId": 70,
      "categoryId": 10,
      "debtId": 50,
      "name": "Debt: Laptop installments",
      "plannedAmount": 100000.00,
      "plannedCurrency": "COP",
      "spentAmount": 100000.00,
      "spentCurrency": "COP",
      "status": "ACTIVE",
      "sourceType": "DEBT_DERIVED",
      "createdAt": "2026-05-12T17:00:00Z",
      "updatedAt": "2026-05-12T17:00:00Z"
    }
  ],
  "impacts": [
    {
      "id": 90,
      "accountId": 1,
      "budgetId": 70,
      "subBudgetId": 80,
      "debtId": 50,
      "expenseId": 101,
      "periodYear": 2026,
      "periodMonth": 6,
      "expectedAmount": 100000.00,
      "expectedCurrency": "COP",
      "paidAmount": 0.00,
      "paidCurrency": "COP",
      "status": "ACTIVE",
      "sourceType": "DEBT_INSTALLMENT",
      "createdAt": "2026-05-12T17:00:00Z",
      "updatedAt": "2026-05-12T17:00:00Z"
    }
  ]
}
```

For manual sub-budgets, `spentAmount` is calculated dynamically from active simple expenses in the same month/category. For debt-derived sub-budgets, it reflects paid budget impacts.

## Analytics Monthly Summary

```json
{
  "accountId": 1,
  "year": 2026,
  "month": 5,
  "totalIncome": 5000000.00,
  "totalExpenses": 1245000.00,
  "netBalance": 3755000.00,
  "totalDebtRemaining": 1100000.00,
  "totalDebtPaidInMonth": 100000.00,
  "activeDebtsCount": 1,
  "paidDebtsCount": 0,
  "budgetExpected": 100000.00,
  "budgetPaid": 100000.00,
  "budgetPending": 0.00,
  "generatedAt": "2026-05-12T17:00:00Z"
}
```

## Import Preview

```json
{
  "batchId": 200,
  "accountId": 1,
  "participantId": 1,
  "originalFilename": "expenses.xlsx",
  "status": "PREVIEW",
  "totalRows": 2,
  "validRows": 1,
  "invalidRows": 1,
  "confirmedAt": null,
  "rows": [
    {
      "id": 201,
      "rowNumber": 2,
      "expenseDate": "2026-05-12",
      "description": "Lunch",
      "amount": 45000.00,
      "currency": "COP",
      "categoryName": "Food",
      "categoryId": 10,
      "paymentMethodName": "Cash",
      "paymentMethodId": 20,
      "paymentState": "PAID",
      "appliesDebtPayment": false,
      "debtId": null,
      "debtLabel": null,
      "debtPaymentType": null,
      "debtPaymentNotes": null,
      "valid": true,
      "errors": [],
      "createdExpenseId": null,
      "createdDebtPaymentId": null
    }
  ]
}
```
