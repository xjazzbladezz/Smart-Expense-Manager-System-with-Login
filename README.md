# SmartSpend — Smart Personal Expense & Budget Management System

**Technologies:** Swing · JDBC · Excel data (Apache POI) · MySQL · Maven

## Run (3 steps)
1. Start MySQL. Edit `src/main/resources/db.properties` (user / password).
   The app creates the database and tables by itself (`sql/schema.sql` is for reference).
2. `mvn clean package`
3. `java -jar target/SmartExpenseManager.jar`   (or `mvn exec:java`)

Login: **admin / admin123** (or create your own account).
First launch: open **Excel Data → Generate demo data** to fill 6 months of sample transactions.
Needs JDK 17+.

## Features
- Dark, animated dashboard: KPI cards, income-vs-expense bars, category donut, daily line chart, health-score gauge (all custom Java2D, no chart library)
- Transactions: search + type/category/month filters, sortable table, add / edit / delete, right-click menu
- Budgets: per-category monthly limits with animated progress and over-budget alerts
- Analytics: auto-generated insights, weekday pattern, payment-mode split, 12-month savings trend
- Excel: import `.xlsx` (validated, row-wise error report), export styled workbook with live `SUMIFS` summary, downloadable template
- Undo / Redo (Ctrl+Z / Ctrl+Y), Ctrl+N new transaction, Alt+1..5 switch pages

## Data structures used
| Structure | Where | Why |
|---|---|---|
| Trie | `ds/Trie` | instant description auto-suggest (+ remembers category) |
| Two stacks (Command pattern) | `ds/UndoRedoManager` | undo / redo of add, edit, delete |
| Min-heap (PriorityQueue) | `ds/TopN` | top-N biggest expenses in O(M log N) |
| LRU cache (LinkedHashMap) | `ds/LRUCache` | caches analytics results |
| HashMap / LinkedHashMap | `ExpenseService` | grouping by category / payment mode |

## Structure
```
com.smartexpense
 ├─ App                 entry point
 ├─ model/              Transaction, User, Categories, TransactionType
 ├─ db/                 DBConnection, DatabaseInitializer, UserDAO, TransactionDAO, BudgetDAO  (JDBC)
 ├─ ds/                 Trie, UndoRedoManager, TopN, LRUCache
 ├─ service/            ExpenseService, BudgetService, ExcelService, DataSeeder, AppContext
 └─ ui/                 LoginFrame, MainFrame, Dashboard/Transactions/Budget/Analytics/Data panels
     ├─ charts/         BarChart, DonutChart, LineChart, GaugeChart
     └─ components/     FancyButton, StatCard, Card, Toast, NavButton, ...
```
"# Smart-Expense-Manager-System-with-Login" 
