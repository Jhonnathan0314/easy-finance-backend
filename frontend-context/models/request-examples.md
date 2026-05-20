# Request Examples

## Register

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "fullName": "Demo User"
}
```

## Login

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

## Create Account

```json
{
  "name": "Personal",
  "description": "Main personal finance account"
}
```

## Create Category

```json
{
  "name": "Food",
  "description": "Meals and groceries",
  "type": "EXPENSE"
}
```

## Create Payment Method

```json
{
  "name": "Cash",
  "description": "Cash wallet",
  "type": "CASH"
}
```

## Create Expense

```json
{
  "categoryId": 10,
  "paymentMethodId": 20,
  "description": "Lunch",
  "amount": 45000.00,
  "expenseDate": "2026-05-12",
  "paymentState": "PAID"
}
```

## Create Installment Expense

```json
{
  "categoryId": 10,
  "paymentMethodId": 20,
  "description": "Laptop",
  "totalAmount": 1200000.00,
  "expenseDate": "2026-05-12",
  "installmentCount": 12,
  "installmentAmount": 100000.00,
  "firstInstallmentDate": "2026-06-01",
  "debtName": "Laptop installments",
  "notes": "Store purchase"
}
```

## Create Manual Debt

```json
{
  "name": "Family loan",
  "description": "Short-term loan",
  "totalAmount": 900000.00,
  "installmentCount": 3,
  "installmentAmount": 300000.00,
  "startDate": "2026-05-15",
  "dueDate": "2026-08-15",
  "notes": "Pay monthly"
}
```

## Register Debt Payment

```json
{
  "paymentType": "INSTALLMENT",
  "amount": 100000.00,
  "paymentDate": "2026-06-05",
  "notes": "First payment"
}
```

## Upsert Budget

```json
{
  "name": "June 2026 budget",
  "status": "ACTIVE"
}
```

## Create SubBudget

```json
{
  "categoryId": 10,
  "name": "Food cap",
  "plannedAmount": 800000.00
}
```

## Create Income

```json
{
  "categoryId": 11,
  "description": "Monthly salary",
  "amount": 5000000.00,
  "incomeDate": "2026-05-30"
}
```

## Preview Expense Import

Multipart form-data:

- key: `file`
- value: `.xlsx` file

Headers expected in first sheet:

```text
Fecha | Descripción | Monto | Categoría | MedioPago | EstadoPago
```

