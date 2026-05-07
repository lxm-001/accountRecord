# 技术设计文档：AccountRecord 一期核心功能

## 技术栈

- 语言：Kotlin 2.2
- UI 框架：Jetpack Compose + Material 3
- 架构模式：MVVM + Clean Architecture（单模块，按 package 分层）
- 本地数据库：Room（SQLite）
- 依赖注入：Hilt
- 导航：Jetpack Navigation Compose
- 异步：Kotlin Coroutines + Flow
- 状态通知：Jetpack LiveData（ViewModel → UI）
- 图表：Vico（Compose-native 图表库）
- CSV 解析：OpenCSV
- 最低 SDK：24，目标 SDK：36

---

## 1. 整体架构

```
com.mian.accountrecord
├── data/                    # 数据层
│   ├── local/
│   │   ├── db/              # Room Database, DAOs
│   │   ├── entity/          # Room Entity 类
│   │   └── preferences/     # MMKV 键值存储
│   ├── mapper/              # Entity ↔ Domain 映射
│   └── repository/          # Repository 实现
├── domain/                  # 领域层
│   ├── model/               # 领域模型（Transaction, Category, Ledger, Budget）
│   ├── repository/          # Repository 接口
│   └── usecase/             # 用例（业务逻辑）
├── ui/                      # 表现层
│   ├── home/                # 首页（摘要卡片、流水列表、Quick Entry Panel）
│   ├── report/              # 统计报表
│   ├── budget/              # 预算设置
│   ├── category/            # 分类管理
│   ├── ledger/              # 账本管理
│   ├── import/              # 账单导入
│   ├── profile/             # 我的页面
│   ├── components/          # 共享 UI 组件
│   ├── navigation/          # 导航图定义
│   └── theme/               # 主题、颜色、字体
├── di/                      # Hilt 模块
├── util/                    # 工具类（CSV Parser, 格式化等）
└── AccountRecordApp.kt      # Application 类
```

### 分层职责

| 层 | 职责 | 依赖方向 |
|----|------|---------|
| ui | Composable 函数 + ViewModel，处理 UI 状态和用户交互 | → domain |
| domain | 用例（UseCase）、领域模型、Repository 接口，纯 Kotlin 无 Android 依赖 | 无外部依赖 |
| data | Room DAO、Entity、Repository 实现、CSV 解析器 | → domain |

---

## 2. 数据模型设计

### 2.1 Room Database

```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        LedgerEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AccountRecordDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun budgetDao(): BudgetDao
}
```

### 2.2 Entity 定义

```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"]),
        ForeignKey(entity = LedgerEntity::class, parentColumns = ["id"], childColumns = ["ledger_id"])
    ],
    indices = [Index("category_id"), Index("ledger_id"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,              // 金额，单位：分（避免浮点精度问题）
    val type: Int,                 // 0=支出, 1=收入
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "ledger_id") val ledgerId: Long,
    val date: Long,                // 时间戳（毫秒）
    val note: String? = null,
    @ColumnInfo(name = "source") val source: String = "manual", // manual / alipay / wechat
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,              // Material Icon 名称
    val color: String,             // Hex 颜色值 "#FF5722"
    val type: Int,                 // 0=支出, 1=收入
    @ColumnInfo(name = "is_preset") val isPreset: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)

@Entity(tableName = "ledgers")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val template: String = "custom", // daily / travel / family / project / custom
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"])
    ],
    indices = [Index("category_id")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null, // null = 月度总预算
    val amount: Long,              // 预算金额，单位：分
    @ColumnInfo(name = "year_month") val yearMonth: String // 格式 "2026-04"
)
```

### 2.3 DAO 接口

```kotlin
@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions 
        WHERE ledger_id = :ledgerId 
        AND date >= :startTime AND date < :endTime 
        ORDER BY date DESC
    """)
    fun getByLedgerAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT type, SUM(amount) as total 
        FROM transactions 
        WHERE ledger_id = :ledgerId 
        AND date >= :startTime AND date < :endTime 
        GROUP BY type
    """)
    fun getSummaryByLedgerAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<TypeSummary>>

    @Query("""
        SELECT category_id, SUM(amount) as total 
        FROM transactions 
        WHERE ledger_id = :ledgerId AND type = 0
        AND date >= :startTime AND date < :endTime 
        GROUP BY category_id ORDER BY total DESC
    """)
    fun getExpenseByCategoryAndDateRange(ledgerId: Long, startTime: Long, endTime: Long): Flow<List<CategorySummary>>

    @Insert fun insert(transaction: TransactionEntity): Long
    @Insert fun insertAll(transactions: List<TransactionEntity>): List<Long>
    @Update fun update(transaction: TransactionEntity)
    @Delete fun delete(transaction: TransactionEntity)
}

data class TypeSummary(val type: Int, val total: Long)
data class CategorySummary(@ColumnInfo(name = "category_id") val categoryId: Long, val total: Long)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sort_order ASC")
    fun getByType(type: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Insert fun insert(category: CategoryEntity): Long
    @Update fun update(category: CategoryEntity)
    @Delete fun delete(category: CategoryEntity)

    @Query("UPDATE categories SET sort_order = :order WHERE id = :id")
    fun updateSortOrder(id: Long, order: Int)

    @Query("UPDATE transactions SET category_id = :targetId WHERE category_id = :sourceId")
    fun migrateTransactions(sourceId: Long, targetId: Long)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledgers ORDER BY created_at ASC")
    fun getAll(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers WHERE is_active = 1 LIMIT 1")
    fun getActive(): Flow<LedgerEntity?>

    @Insert fun insert(ledger: LedgerEntity): Long
    @Update fun update(ledger: LedgerEntity)
    @Delete fun delete(ledger: LedgerEntity)

    @Query("UPDATE ledgers SET is_active = 0")
    fun deactivateAll()

    @Query("UPDATE ledgers SET is_active = 1 WHERE id = :id")
    fun activate(id: Long)

    @Query("SELECT COUNT(*) FROM ledgers")
    fun count(): Int
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE year_month = :yearMonth")
    fun getByMonth(yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE year_month = :yearMonth AND category_id IS NULL LIMIT 1")
    fun getTotalBudget(yearMonth: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(budget: BudgetEntity)
    @Delete fun delete(budget: BudgetEntity)
}
```

---

## 3. 领域模型

```kotlin
data class Transaction(
    val id: Long = 0,
    val amount: BigDecimal,        // 元为单位，精确到分
    val type: TransactionType,
    val category: Category,
    val ledgerId: Long,
    val date: LocalDateTime,
    val note: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL
)

enum class TransactionType { EXPENSE, INCOME }
enum class TransactionSource { MANUAL, ALIPAY, WECHAT }

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val type: TransactionType,
    val isPreset: Boolean = false,
    val sortOrder: Int = 0
)

data class Ledger(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val template: LedgerTemplate = LedgerTemplate.CUSTOM,
    val isActive: Boolean = false
)

enum class LedgerTemplate { DAILY, TRAVEL, FAMILY, PROJECT, CUSTOM }

data class Budget(
    val id: Long = 0,
    val categoryId: Long? = null,  // null = 月度总预算
    val amount: BigDecimal,
    val yearMonth: YearMonth
)

data class MonthlySummary(
    val income: BigDecimal,
    val expense: BigDecimal,
    val balance: BigDecimal         // income - expense
)

data class BudgetProgress(
    val budget: Budget,
    val spent: BigDecimal,
    val ratio: Float                // spent / budget.amount
)
```

---

## 4. 核心用例

```kotlin
// 收支记录
class AddTransactionUseCase(private val repo: TransactionRepository)
class UpdateTransactionUseCase(private val repo: TransactionRepository)
class DeleteTransactionUseCase(private val repo: TransactionRepository)
class GetTransactionsByMonthUseCase(private val repo: TransactionRepository)
class GetMonthlySummaryUseCase(private val repo: TransactionRepository)

// 账单导入
class ImportCsvUseCase(private val csvParser: CsvParser, private val repo: TransactionRepository)
class ExportCsvUseCase(private val csvPrinter: CsvPrinter, private val repo: TransactionRepository)
class DetectDuplicatesUseCase(private val repo: TransactionRepository)

// 分类管理
class GetCategoriesUseCase(private val repo: CategoryRepository)
class AddCategoryUseCase(private val repo: CategoryRepository)
class DeleteCategoryUseCase(private val repo: CategoryRepository)
class ReorderCategoriesUseCase(private val repo: CategoryRepository)

// 账本管理
class GetLedgersUseCase(private val repo: LedgerRepository)
class CreateLedgerUseCase(private val repo: LedgerRepository, private val categoryRepo: CategoryRepository)
class SwitchLedgerUseCase(private val repo: LedgerRepository)
class DeleteLedgerUseCase(private val repo: LedgerRepository, private val transactionRepo: TransactionRepository)

// 统计报表
class GetExpenseByCategoryUseCase(private val repo: TransactionRepository)
class GetTrendDataUseCase(private val repo: TransactionRepository)

// 预算
class SetBudgetUseCase(private val repo: BudgetRepository)
class GetBudgetProgressUseCase(private val budgetRepo: BudgetRepository, private val transactionRepo: TransactionRepository)
class CheckBudgetAlertUseCase(private val budgetRepo: BudgetRepository, private val transactionRepo: TransactionRepository)
```

---

## 5. ViewModel 设计

### 5.1 HomeViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactions: GetTransactionsByMonthUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val getLedgers: GetLedgersUseCase,
    private val switchLedger: SwitchLedgerUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    data class HomeUiState(
        val currentLedger: Ledger? = null,
        val ledgers: List<Ledger> = emptyList(),
        val currentYearMonth: YearMonth = YearMonth.now(),
        val summary: MonthlySummary = MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
        val budgetProgress: BudgetProgress? = null,
        val transactionsByDate: Map<LocalDate, List<Transaction>> = emptyMap(),
        val isRefreshing: Boolean = false
    )

    private val _uiState = MutableLiveData(HomeUiState())
    val uiState: LiveData<HomeUiState> = _uiState

    // Quick Entry Panel 状态
    data class QuickEntryState(
        val type: TransactionType = TransactionType.EXPENSE,
        val amount: String = "",
        val selectedCategory: Category? = null,
        val selectedDate: LocalDate = LocalDate.now(),
        val note: String = "",
        val isVisible: Boolean = false,
        val lastUsedCategoryId: Long? = null
    )

    private val _quickEntryState = MutableLiveData(QuickEntryState())
    val quickEntryState: LiveData<QuickEntryState> = _quickEntryState
}
```

### 5.2 ReportViewModel

```kotlin
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getExpenseByCategory: GetExpenseByCategoryUseCase,
    private val getTrendData: GetTrendDataUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase
) : ViewModel() {

    data class ReportUiState(
        val chartType: ChartType = ChartType.PIE,
        val timeGranularity: TimeGranularity = TimeGranularity.MONTH,
        val currentPeriod: YearMonth = YearMonth.now(),
        val summary: MonthlySummary = MonthlySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
        val categoryData: List<CategoryExpense> = emptyList(),
        val trendData: List<TrendPoint> = emptyList(),
        val isEmpty: Boolean = true
    )

    enum class ChartType { PIE, LINE, BAR }
    enum class TimeGranularity { DAY, WEEK, MONTH, YEAR }
}
```

### 5.3 BudgetViewModel

```kotlin
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val setBudget: SetBudgetUseCase,
    private val getBudgetProgress: GetBudgetProgressUseCase,
    private val getCategories: GetCategoriesUseCase
) : ViewModel() {

    data class BudgetUiState(
        val totalBudget: BudgetProgress? = null,
        val categoryBudgets: List<BudgetProgress> = emptyList(),
        val categories: List<Category> = emptyList(),
        val hasAnyBudget: Boolean = false
    )
}
```

---

## 6. CSV 解析器设计

```kotlin
interface CsvParser {
    fun parse(inputStream: InputStream, source: CsvSource): CsvParseResult
}

enum class CsvSource { ALIPAY, WECHAT }

data class CsvParseResult(
    val transactions: List<ParsedTransaction>,
    val errors: List<String>
)

data class ParsedTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val date: LocalDateTime,
    val counterparty: String,
    val originalCategory: String,
    val mappedCategoryId: Long?,   // null = 未匹配
    val isDuplicate: Boolean = false,
    val isSelected: Boolean = true
)

interface CsvPrinter {
    fun print(transactions: List<Transaction>, outputStream: OutputStream)
}
```

### 支付宝 CSV 格式映射

| 支付宝字段 | 映射目标 |
|-----------|---------|
| 交易时间 | date |
| 交易分类 | originalCategory → mappedCategoryId |
| 交易对方 | counterparty |
| 金额（元） | amount |
| 收/支 | type（支出/收入/不计收支） |

### 微信 CSV 格式映射

| 微信字段 | 映射目标 |
|---------|---------|
| 交易时间 | date |
| 交易类型 | originalCategory → mappedCategoryId |
| 交易对方 | counterparty |
| 金额(元) | amount（去除 ¥ 前缀） |
| 收/支 | type |

### 分类自动映射规则

```kotlin
val CATEGORY_MAPPING = mapOf(
    // 支付宝分类 → 系统分类
    "餐饮美食" to "餐饮", "交通出行" to "交通", "充值缴费" to "通讯",
    "日用百货" to "日用", "服饰装扮" to "服饰", "医疗健康" to "医疗",
    "文化休闲" to "娱乐", "教育培训" to "教育", "住房缴费" to "住房",
    // 微信分类 → 系统分类
    "商户消费" to "购物", "转账" to "社交", "红包" to "红包",
    "群收款" to "社交"
)
```

---

## 7. 导航设计

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Report : Screen("report")
    object Budget : Screen("budget")
    object Profile : Screen("profile")
    object CategoryManage : Screen("category_manage")
    object LedgerManage : Screen("ledger_manage")
    object TransactionDetail : Screen("transaction/{id}") {
        fun createRoute(id: Long) = "transaction/$id"
    }
    object BillImport : Screen("bill_import")
}
```

底部导航栏使用 `NavigationBar` + `NavigationBarItem`，中央 FAB 使用 `FloatingActionButton` 配合 `Scaffold`。

---

## 8. 预算提醒设计

```kotlin
class BudgetAlertWorker(
    context: Context,
    params: WorkerParameters,
    private val checkBudgetAlert: CheckBudgetAlertUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val alerts = checkBudgetAlert()
        alerts.forEach { alert ->
            NotificationHelper.showBudgetAlert(applicationContext, alert)
        }
        return Result.success()
    }
}
```

- 使用 WorkManager 在每次新增 Transaction 后触发预算检查
- 本地通知通过 `NotificationCompat.Builder` 发送
- 通知渠道：`budget_alerts`，重要性 `IMPORTANCE_DEFAULT`

---

## 9. 新增依赖清单

```toml
# gradle/libs.versions.toml 新增
[versions]
room = "2.6.1"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
navigationCompose = "2.8.0"
livedata = "1.7.0"
vico = "2.0.0-beta.1"
opencsv = "5.9"
workManager = "2.9.1"
# MMKV
mmkv = "1.3.9"

[libraries]
# MMKV
mmkv = { group = "com.tencent", name = "mmkv", version.ref = "mmkv" }
# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }

# LiveData Compose
runtime-livedata = { group = "androidx.compose.runtime", name = "runtime-livedata", version.ref = "livedata" }

# Charts
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }

# CSV
opencsv = { group = "com.opencsv", name = "opencsv", version.ref = "opencsv" }

# WorkManager
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.2.10-1.0.31" }
```

---

## 10. 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 金额存储 | Long（分） | 避免浮点精度问题，BigDecimal 仅在领域层使用 |
| 图表库 | Vico | Compose-native，无需 View 互操作，API 简洁 |
| DI 框架 | Hilt | Android 官方推荐，与 ViewModel 集成好 |
| CSV 解析 | OpenCSV | 成熟稳定，处理中文编码和特殊字符好 |
| 预算检查 | WorkManager | 可靠的后台任务调度，支持约束条件 |
| 状态管理 | LiveData | 生命周期感知，Compose 中通过 `observeAsState()` 订阅 |
| 导航 | Navigation Compose | 官方方案，类型安全路由 |
| 预设数据 | Room prepopulate | 首次启动通过 `RoomDatabase.Callback` 插入预设分类和默认账本 |
| K-V 存储 | MMKV | 替代 SharedPreferences，性能更优（mmap），支持多进程，腾讯开源成熟方案 |
