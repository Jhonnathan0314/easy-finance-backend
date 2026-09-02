# DTO Reference

Types use JSON conventions:

- `Long` and `Integer`: number
- `BigDecimal`: number, render with decimal formatting
- `Instant`: ISO timestamp string
- `LocalDate`: `YYYY-MM-DD`
- Money currency: always `COP` in MVP

## Common Page

```ts
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

## Auth

```ts
interface RegisterRequest { email: string; password: string; fullName: string; }
interface LoginRequest { email: string; password: string; }
interface UpdateProfileRequest { fullName: string; }
interface AuthenticatedUser { userId: number; participantId: number; email: string; fullName: string; globalRoles: string[]; }
interface AuthTokenResponse { accessToken: string; tokenType: "Bearer"; expiresIn: number; user: AuthenticatedUser; }
```

Validation:

- `email`: required, valid email
- `password`: required; register min length 8
- `fullName`: required, max 150 (both on register and on `UpdateProfileRequest`)

`PUT /auth/me` (`UpdateProfileRequest`) returns `AuthenticatedUser` (same shape as `GET /auth/me`), reflecting the
updated `fullName`.

## Accounts

```ts
interface CreateAccountRequest { name: string; description?: string | null; }
interface UpdateAccountRequest { name: string; description?: string | null; }
interface AccountResponse { id: number; name: string; description?: string | null; status: string; currentUserRole: string; createdAt: string; updatedAt: string; }
interface AddAccountMemberRequest { email: string; role: "ACCOUNT_ADMIN" | "ACCOUNT_MEMBER"; }
interface ChangeAccountMemberRoleRequest { role: "ACCOUNT_ADMIN" | "ACCOUNT_MEMBER"; }
interface AccountMemberResponse { participantId: number; email: string; displayName: string; role: string; status: string; joinedAt: string; }
```

Validation:

- account `name`: required, max 120
- account `description`: max 500
- member `email`: required, valid email

## Catalogs

```ts
interface CreateCategoryRequest { name: string; description?: string | null; type: "EXPENSE" | "INCOME"; }
interface UpdateCategoryRequest { name: string; description?: string | null; type?: "EXPENSE" | "INCOME" | null; }
interface CategoryResponse { id: number; accountId: number; name: string; description?: string | null; type: string; status: string; createdAt: string; updatedAt: string; }

interface CreatePaymentMethodRequest { name: string; description?: string | null; type: PaymentMethodType; }
interface UpdatePaymentMethodRequest { name: string; description?: string | null; type?: PaymentMethodType | null; }
interface PaymentMethodResponse { id: number; accountId: number; name: string; description?: string | null; type: string; status: string; createdAt: string; updatedAt: string; }
```

Note: type changes are not allowed by backend business rules even if update DTO includes `type`.

## Expenses

```ts
interface CreateExpenseRequest { categoryId: number; participantId?: number | null; paymentMethodId: number; description: string; amount: number; expenseDate: string; paymentState?: "PENDING" | "PARTIAL" | "PAID" | null; }
interface UpdateExpenseRequest { categoryId: number; participantId?: number | null; paymentMethodId: number; description: string; amount: number; expenseDate: string; paymentState: "PENDING" | "PARTIAL" | "PAID"; }
interface DuplicateExpenseRequest { expenseDate: string; amount?: number | null; description?: string | null; paymentState?: "PENDING" | "PARTIAL" | "PAID" | null; }
interface CreateInstallmentExpenseRequest { categoryId: number; participantId?: number | null; paymentMethodId: number; description: string; totalAmount: number; expenseDate: string; installmentCount: number; installmentAmount: number; firstInstallmentDate: string; debtName?: string | null; notes?: string | null; }
interface ExpenseResponse { id: number; accountId: number; categoryId: number; paymentMethodId: number; participantId: number; description: string; amount: number; currency: "COP"; expenseDate: string; paymentState: string; status: string; expenseType: string; sourceType: "MANUAL" | "IMPORT" | "DEBT_PAYMENT"; sourceDebtPaymentId?: number | null; sourceDebtId?: number | null; createdAt: string; updatedAt: string; }
```

Validation:

- amount fields: required and `>= 0.01`
- description: required, max 500
- installment count: min 1
- installment rule: `installmentAmount * installmentCount >= totalAmount`
- `totalAmount` is the original purchase/advance amount; `installmentAmount * installmentCount` is the financed debt total to pay.
- `sourceDebtId` is only set when `sourceType = "DEBT_PAYMENT"`, and only for expenses created after this field was introduced; expenses created before it exist with `sourceDebtId = null` even though `sourceType = "DEBT_PAYMENT"`. Resolve the related debt through this field when present; do not assume it is always populated for `DEBT_PAYMENT` expenses.
- `PUT .../expenses/{expenseId}` and `PATCH .../expenses/{expenseId}/cancel` reject expenses with `sourceType = "DEBT_PAYMENT"` (`EXPENSE_DEBT_PAYMENT_UPDATE_NOT_ALLOWED` / `EXPENSE_DEBT_PAYMENT_CANCEL_NOT_ALLOWED`). `POST .../expenses/{expenseId}/duplicate` still allows it and always creates the duplicate as `sourceType = "MANUAL"` with `sourceDebtPaymentId = null` and `sourceDebtId = null`.
- `GET .../expenses` accepts an optional `debtPaymentOrigin` boolean query parameter: `true` returns only `sourceType = "DEBT_PAYMENT"`; `false` returns everything else; omitted applies no filter on origin.

## Debts And Payments

```ts
interface CreateManualDebtRequest { name: string; participantId?: number | null; description?: string | null; totalAmount: number; installmentCount?: number | null; installmentAmount?: number | null; startDate: string; dueDate?: string | null; notes?: string | null; }
interface RegisterDebtPaymentRequest { paymentType: "INSTALLMENT" | "CAPITAL_PAYMENT"; amount: number; paymentDate: string; notes?: string | null; createExpense?: boolean | null; categoryId?: number | null; paymentMethodId?: number | null; expenseDescription?: string | null; }
interface DebtResponse { id: number; accountId: number; participantId: number; originExpenseId?: number | null; sourceType: string; name: string; description?: string | null; totalAmount: number; scheduledTotalAmount: number; totalCurrency: "COP"; remainingAmount: number; remainingCurrency: "COP"; installmentCount?: number | null; installmentAmount?: number | null; installmentCurrency?: "COP" | null; startDate: string; endDate?: string | null; state: string; notes?: string | null; createdAt: string; updatedAt: string; }
interface DebtPaymentResponse { id: number; accountId: number; debtId: number; participantId: number; paymentType: string; amount: number; currency: "COP"; paymentDate: string; notes?: string | null; status: string; createdAt: string; updatedAt: string; }
interface RegisterDebtPaymentResponse { payment: DebtPaymentResponse; debt: DebtResponse; createdExpenseId?: number | null; }
```

## Budgets

```ts
interface UpsertBudgetRequest { name?: string | null; status?: "ACTIVE" | "CLOSED" | "ARCHIVED" | null; }
interface DuplicateBudgetRequest { targetYear: number; targetMonth: number; name?: string | null; }
interface CreateAnnualSubBudgetBaseRequest { name: string; categoryId?: number | null; plannedAmount: number; }
interface CreateAnnualBudgetRequest { year: number; name?: string | null; status?: "ACTIVE" | "CLOSED" | "ARCHIVED" | null; subBudgets: CreateAnnualSubBudgetBaseRequest[]; }
interface AnnualBudgetResponse { accountId: number; year: number; createdBudgets: BudgetResponse[]; }
interface CreateSubBudgetRequest { categoryId?: number | null; participantId?: number | null; name: string; plannedAmount: number; }
interface UpdateSubBudgetRequest { categoryId?: number | null; participantId?: number | null; name: string; plannedAmount: number; }
interface BudgetResponse { id: number; accountId: number; year: number; month: number; name?: string | null; status: string; createdAt: string; updatedAt: string; }
interface SubBudgetResponse { id: number; accountId: number; budgetId: number; categoryId?: number | null; participantId?: number | null; debtId?: number | null; name: string; plannedAmount: number; plannedCurrency: "COP"; spentAmount: number; spentCurrency: "COP"; status: string; sourceType: string; createdAt: string; updatedAt: string; }
interface BudgetImpactResponse { id: number; accountId: number; budgetId: number; subBudgetId: number; debtId: number; expenseId?: number | null; periodYear: number; periodMonth: number; expectedAmount: number; expectedCurrency: "COP"; paidAmount: number; paidCurrency: "COP"; status: string; sourceType: string; createdAt: string; updatedAt: string; }
interface BudgetDetailResponse { budget: BudgetResponse; subBudgets: SubBudgetResponse[]; impacts: BudgetImpactResponse[]; }
```

Budget note: manual sub-budget `spentAmount` is a read-time calculation from active simple expenses in the same month/category with `sourceType = "MANUAL" | "IMPORT"`. `sourceType = "DEBT_PAYMENT"` is excluded to avoid double counting debt payments. Debt-derived sub-budgets use budget impacts.

`DuplicateBudgetRequest` (`POST /budgets/{sourceYear}/{sourceMonth}/duplicate`) copies active `MANUAL` sub-budgets from the source month into `targetYear`/`targetMonth`; fails with `BUDGET_TARGET_ALREADY_EXISTS` if the target month already exists. `ACCOUNT_ADMIN` only.

`CreateAnnualBudgetRequest` (`POST /budgets/annual`) creates all 12 monthly budgets for `year` directly from the request body (not from an uploaded file); fails with `ANNUAL_BUDGET_MONTH_ALREADY_EXISTS` if any month of that year already exists. `ACCOUNT_ADMIN` only. Do not confuse with the Excel-based `POST /imports/budgets/annual`.

## Income

```ts
interface CreateIncomeRequest { categoryId: number; participantId?: number | null; description: string; amount: number; incomeDate: string; }
interface UpdateIncomeRequest { categoryId: number; participantId?: number | null; description: string; amount: number; incomeDate: string; }
interface DuplicateIncomeRequest { incomeDate: string; amount?: number | null; description?: string | null; }
interface IncomeResponse { id: number; accountId: number; categoryId: number; participantId: number; description: string; amount: number; currency: "COP"; incomeDate: string; status: string; createdAt: string; updatedAt: string; }
```

## Analytics

```ts
interface MonthlySummaryResponse { accountId: number; year: number; month: number; totalIncome: number; totalExpenses: number; netBalance: number; totalDebtRemaining: number; totalDebtPaidInMonth: number; activeDebtsCount: number; paidDebtsCount: number; budgetExpected: number; budgetPaid: number; budgetPending: number; generatedAt: string; }
interface CashflowSummaryResponse { accountId: number; from: string; to: string; totalIncome: number; totalSimpleExpenseOutflow: number; totalDebtPaymentOutflow: number; totalOutflow: number; netCashflow: number; generatedAt: string; }
interface ExpenseSummaryResponse { accountId: number; from: string; to: string; totalSimpleExpenses: number; totalInstallmentPurchases: number; totalExpensesConceptual: number; expensesCount: number; generatedAt: string; }
interface CashflowItem { period: string; totalIncome: number; simpleExpenseOutflow: number; debtPaymentOutflow: number; totalOutflow: number; netCashflow: number; }
interface CashflowResponse { accountId: number; from: string; to: string; groupBy: "DAY" | "WEEK" | "MONTH"; items: CashflowItem[]; }
interface CategoryAmountItem { categoryId: number; categoryName: string; amount: number; count: number; }
interface CategoryBreakdownResponse { accountId: number; from: string; to: string; items: CategoryAmountItem[]; }
interface PaymentMethodAmountItem { paymentMethodId?: number | null; paymentMethodName: string; amount: number; count: number; }
interface PaymentMethodBreakdownResponse { accountId: number; from: string; to: string; items: PaymentMethodAmountItem[]; }
interface PaymentMethodTypeAmountItem { paymentMethodType: "CASH" | "BANK_ACCOUNT" | "CREDIT_CARD" | "DEBIT_CARD" | "DIGITAL_WALLET" | "OTHER"; amount: number; count: number; }
interface PaymentMethodTypeBreakdownResponse { accountId: number; from: string; to: string; items: PaymentMethodTypeAmountItem[]; }
interface DebtSummaryResponse { accountId: number; activeDebtsCount: number; paidDebtsCount: number; cancelledDebtsCount: number; totalDebtAmount: number; totalRemainingBalance: number; totalPaidAmount: number; manualDebtsCount: number; installmentExpenseDebtsCount: number; }
interface BudgetSummaryResponse { accountId: number; year: number; month: number; budgetId?: number | null; expectedAmount: number; paidAmount: number; pendingAmount: number; impactsCount: number; paidImpactsCount: number; activeImpactsCount: number; subBudgetsCount: number; }
```

Analytics cashflow represents real money only: active incomes, active simple expenses with `paymentState = "PAID"`, and active debt payments for non-cancelled debts. Expenses with `sourceType = "DEBT_PAYMENT"` are excluded from simple cashflow outflow because the debt payment already counts the real cash movement. Conceptual expense analytics can include full `INSTALLMENT` purchase amounts and debt-payment associated expenses, and should not be displayed as cash outflow.

Budget summary combines manual planned amounts, dynamic manual expense execution, and debt impact expected/paid amounts.

## Expense Imports

```ts
interface ImportRowError { column: string; code: string; message: string; }
interface ExpenseImportRowResponse { id: number; rowNumber: number; expenseDate?: string | null; description?: string | null; amount?: number | null; currency?: "COP" | null; categoryName?: string | null; categoryId?: number | null; paymentMethodName?: string | null; paymentMethodId?: number | null; paymentState?: string | null; participantLabel?: string | null; participantId?: number | null; appliesDebtPayment: boolean; debtId?: number | null; debtLabel?: string | null; debtPaymentType?: "INSTALLMENT" | "CAPITAL_PAYMENT" | null; debtPaymentNotes?: string | null; valid: boolean; errors: ImportRowError[]; createdExpenseId?: number | null; createdDebtPaymentId?: number | null; }
interface ExpenseImportBatchResponse { batchId: number; accountId: number; participantId: number; originalFilename: string; status: string; totalRows: number; validRows: number; invalidRows: number; confirmedAt?: string | null; rows: ExpenseImportRowResponse[]; }
```

## Direct Imports

```ts
interface IncomeImportRowResponse { rowNumber: number; incomeDate?: string | null; description?: string | null; categoryName?: string | null; categoryId?: number | null; participantLabel?: string | null; participantId?: number | null; amount?: number | null; valid: boolean; createdIncomeId?: number | null; errors: string[]; }
interface IncomeImportResponse { createdCount: number; rows: IncomeImportRowResponse[]; }

interface CategoryImportRowResponse { rowNumber: number; name?: string | null; description?: string | null; type?: "EXPENSE" | "INCOME" | null; valid: boolean; createdCategoryId?: number | null; errors: string[]; }
interface CategoryImportResponse { createdCount: number; rows: CategoryImportRowResponse[]; }

interface PaymentMethodImportRowResponse { rowNumber: number; name?: string | null; description?: string | null; type?: PaymentMethodType | null; valid: boolean; createdPaymentMethodId?: number | null; errors: string[]; }
interface PaymentMethodImportResponse { createdCount: number; rows: PaymentMethodImportRowResponse[]; }

interface AnnualBudgetImportRowResponse { rowNumber: number; year?: number | null; month?: string | null; budgetName?: string | null; categoryName?: string | null; categoryId?: number | null; subBudgetName?: string | null; plannedAmount?: number | null; participantLabel?: string | null; participantId?: number | null; valid: boolean; appliedMonths: number[]; errors: string[]; }
interface AnnualBudgetImportResponse { year?: number | null; createdBudgetsCount: number; createdSubBudgetsCount: number; rows: AnnualBudgetImportRowResponse[]; }
```
