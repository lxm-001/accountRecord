# 实现计划：AccountRecord 一期核心功能

## 概述

基于 MVVM + Clean Architecture 分层架构，从基础设施（依赖、数据库）到领域层、数据层、UI 层逐步构建。每个阶段确保代码可编译、可测试，最终将所有模块串联集成。

## 任务

- [x] 1. 项目基础设施搭建
  - [x] 1.1 配置 Gradle 依赖
    - 在 `gradle/libs.versions.toml` 中新增 Room、Hilt、Navigation Compose、LiveData、Vico、OpenCSV、WorkManager、MMKV 的版本和依赖声明
    - 在 `settings.gradle.kts` 中添加 Hilt 和 KSP 插件
    - 在 `app/build.gradle.kts` 中应用 `hilt`、`ksp` 插件，添加所有新依赖
    - _需求: 全局基础_

  - [x] 1.2 创建 Application 类和 Hilt 入口
    - 创建 `AccountRecordApp.kt`，添加 `@HiltAndroidApp` 注解
    - 在 `AndroidManifest.xml` 中注册 Application 类
    - 为 `MainActivity` 添加 `@AndroidEntryPoint` 注解
    - _需求: 全局基础_

- [x] 2. 数据层 — Room 数据库与 Entity
  - [x] 2.1 创建 Room Entity 类
    - 在 `data/local/entity/` 下创建 `TransactionEntity`、`CategoryEntity`、`LedgerEntity`、`BudgetEntity`
    - 包含所有字段、外键、索引定义，金额使用 Long（分）存储
    - 创建 `TypeSummary`、`CategorySummary` 查询结果类
    - 创建 `Converters` 类型转换器
    - _需求: 1.2, 3.1, 4.1, 6.1_

  - [x] 2.2 创建 DAO 接口
    - 在 `data/local/db/` 下创建 `TransactionDao`、`CategoryDao`、`LedgerDao`、`BudgetDao`
    - 实现设计文档中定义的所有查询方法，返回 `Flow` 类型
    - `CategoryDao` 包含 `migrateTransactions` 方法用于分类删除时迁移记录
    - `LedgerDao` 包含 `deactivateAll` + `activate` 方法用于账本切换
    - _需求: 1.3, 1.6, 1.7, 2.5, 3.7, 4.6, 4.7, 5.1, 6.2, 6.3_

  - [x] 2.3 创建 Room Database 类
    - 在 `data/local/db/` 下创建 `AccountRecordDatabase`，注册所有 Entity 和 DAO
    - 实现 `RoomDatabase.Callback`，在 `onCreate` 中插入预设分类（13 个支出 + 6 个收入）和默认"日常账本"
    - _需求: 3.1, 3.2, 4.1_

  - [x] 2.4 编写 DAO 单元测试
    - 使用 Room in-memory database 测试 TransactionDao 的 CRUD 和聚合查询
    - 测试 CategoryDao 的排序和迁移逻辑
    - 测试 LedgerDao 的激活/去激活逻辑
    - _需求: 1.3, 3.7, 4.6_

- [x] 3. 数据层 — MMKV 偏好存储与 Repository 实现
  - [x] 3.1 创建 MMKV 偏好存储
    - 在 `data/local/preferences/` 下创建 `AppPreferences` 类
    - 使用 MMKV 存储：上次选择的分类 ID、当前激活账本 ID 等用户偏好
    - 在 `AccountRecordApp.onCreate` 中初始化 MMKV
    - _需求: 7.20_

  - [x] 3.2 创建领域模型和 Repository 接口
    - 在 `domain/model/` 下创建 `Transaction`、`Category`、`Ledger`、`Budget`、`MonthlySummary`、`BudgetProgress`、`TransactionType`、`TransactionSource`、`LedgerTemplate` 等领域模型
    - 在 `domain/repository/` 下创建 `TransactionRepository`、`CategoryRepository`、`LedgerRepository`、`BudgetRepository` 接口
    - _需求: 1.2, 3.1, 4.1, 6.1_

  - [x] 3.3 创建 Entity ↔ Domain 映射器
    - 在 `data/mapper/` 下创建映射扩展函数：`TransactionEntity.toDomain()`、`Transaction.toEntity()` 等
    - 金额转换：Entity Long（分）↔ Domain BigDecimal（元）
    - _需求: 1.2_

  - [x] 3.4 实现 Repository 类
    - 在 `data/repository/` 下实现 `TransactionRepositoryImpl`、`CategoryRepositoryImpl`、`LedgerRepositoryImpl`、`BudgetRepositoryImpl`
    - 所有方法通过 DAO 操作数据库，使用映射器转换数据
    - _需求: 1.3, 1.7, 2.5, 3.4, 4.4, 6.2, 6.3_

  - [x] 3.5 创建 Hilt DI 模块 
    - 在 `di/` 下创建 `DatabaseModule`（提供 Room Database 和 DAO 实例）
    - 创建 `RepositoryModule`（绑定 Repository 接口到实现类）
    - 创建 `PreferencesModule`（提供 AppPreferences 实例）
    - _需求: 全局基础_

- [x] 4. 领域层 — UseCase 实现
  - [x] 4.1 实现收支记录相关 UseCase
    - 创建 `AddTransactionUseCase`：验证金额范围（>0 且 ≤9999999999）、保存记录
    - 创建 `UpdateTransactionUseCase`、`DeleteTransactionUseCase`
    - 创建 `GetTransactionsByMonthUseCase`：按月查询并按日期分组
    - 创建 `GetMonthlySummaryUseCase`：计算月度收入/支出/结余
    - _需求: 1.2, 1.3, 1.4, 1.5, 1.7, 1.8, 1.9, 1.10_

  - [x] 4.2 实现分类管理相关 UseCase
    - 创建 `GetCategoriesUseCase`、`AddCategoryUseCase`（含重名校验）、`DeleteCategoryUseCase`（含记录迁移）、`ReorderCategoriesUseCase`
    - `DeleteCategoryUseCase` 需将关联 Transaction 迁移到"其他"分类
    - 禁止删除/重命名预设分类
    - _需求: 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9_

  - [x] 4.3 实现账本管理相关 UseCase
    - 创建 `GetLedgersUseCase`、`CreateLedgerUseCase`（含重名校验和模板分类预填）、`SwitchLedgerUseCase`、`DeleteLedgerUseCase`（禁止删除最后一个账本，含记录迁移/删除选项）
    - _需求: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7, 4.8, 4.9_

  - [x] 4.4 实现统计报表相关 UseCase
    - 创建 `GetExpenseByCategoryUseCase`：按分类聚合支出
    - 创建 `GetTrendDataUseCase`：按日/周/月/年粒度生成趋势数据点
    - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 4.5 实现预算相关 UseCase
    - 创建 `SetBudgetUseCase`：设置/更新月度总预算和分类预算
    - 创建 `GetBudgetProgressUseCase`：计算预算使用进度和比例
    - 创建 `CheckBudgetAlertUseCase`：检查是否达到 80% 或超支阈值
    - _需求: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_

  - [x] 4.6 编写 UseCase 单元测试
    - 测试 `AddTransactionUseCase` 的金额验证逻辑（0、负数、超上限）
    - 测试 `DeleteCategoryUseCase` 的记录迁移逻辑
    - 测试 `DeleteLedgerUseCase` 禁止删除最后一个账本
    - 测试 `CheckBudgetAlertUseCase` 的阈值判断（80%、100%）
    - _需求: 1.8, 1.9, 3.7, 3.8, 4.8, 6.5, 6.6, 6.7_

- [x] 5. 检查点 — 数据层与领域层验证
  - 确保所有测试通过，如有问题请向用户确认。

- [x] 6. CSV 解析与导出
  - [x] 6.1 实现 CSV 解析器
    - 在 `util/` 下创建 `CsvParser` 接口和 `CsvParserImpl` 实现
    - 使用 OpenCSV 解析支付宝和微信 CSV 格式，处理中文编码（GBK/UTF-8）
    - 实现分类自动映射规则（设计文档中的 `CATEGORY_MAPPING`）
    - 解析结果包含 `ParsedTransaction` 列表和错误信息
    - _需求: 2.1, 2.2, 2.3, 2.4, 2.6_

  - [x] 6.2 实现重复检测与 CSV 导出
    - 实现 `DetectDuplicatesUseCase`：基于金额+日期+对方名称判断重复
    - 创建 `CsvPrinter` 接口和 `CsvPrinterImpl`：将 Transaction 列表导出为标准 CSV
    - _需求: 2.7, 2.8_

  - [x] 6.3 实现账单导入 UseCase
    - 创建 `ImportCsvUseCase`：协调解析、重复检测、批量写入流程
    - 创建 `ExportCsvUseCase`：协调查询和导出流程
    - _需求: 2.5, 2.9_

  - [x] 6.4 编写 CSV 解析器单元测试
    - 测试支付宝 CSV 格式解析正确性
    - 测试微信 CSV 格式解析正确性
    - 测试格式错误时返回描述性错误信息
    - 测试分类自动映射
    - _需求: 2.1, 2.3, 2.6_

  - [x] 6.5 编写 CSV 往返一致性属性测试
    - **属性 1：CSV 往返一致性**
    - 对任意有效 Transaction 列表，先 CsvPrinter 导出再 CsvParser 解析，结果与原始列表等价
    - **验证: 需求 2.9**

- [x] 7. UI 层 — 导航框架与共享组件
  - [x] 7.1 创建导航图和 Screen 路由定义
    - 在 `ui/navigation/` 下创建 `Screen` sealed class 和 `AppNavGraph` Composable
    - 定义所有页面路由：Home、Report、Budget、Profile、CategoryManage、LedgerManage、TransactionDetail、BillImport
    - _需求: 7.27_

  - [x] 7.2 实现底部导航栏和 Scaffold 框架
    - 在 `ui/navigation/` 下创建 `MainScaffold` Composable
    - 实现 `NavigationBar` + 4 个 `NavigationBarItem`（首页、报表、预算、我的）
    - 中央 FAB（56dp 圆形"+"按钮），导航栏高度 56dp
    - 更新 `MainActivity` 使用 `MainScaffold` 作为根布局
    - _需求: 7.21, 7.22, 7.23, 7.27, 7.28_

  - [x] 7.3 创建共享 UI 组件
    - 在 `ui/components/` 下创建：`AmountText`（金额格式化显示，支出红色/收入绿色）、`CategoryIcon`（圆形背景分类图标）、`EmptyStateView`（空状态插画+文案）、`ConfirmDialog`（通用确认对话框）
    - _需求: 7.12, 7.15, 5.8_

- [x] 8. UI 层 — 首页
  - [x] 8.1 实现 HomeViewModel
    - 创建 `HomeViewModel`，注入所需 UseCase
    - 定义 `HomeUiState`（当前账本、账本列表、月份、摘要、预算进度、按日期分组的流水）和 `QuickEntryState`
    - 使用 LiveData 暴露状态，ViewModel 内部使用 Coroutines + Flow 收集数据
    - 实现月份切换、账本切换、删除记录等操作方法
    - _需求: 7.2, 7.3, 7.4, 7.6_

  - [x] 8.2 实现首页顶部栏和摘要卡片
    - 创建 `HomeTopBar` Composable：左侧账本名称 + 下拉菜单，右侧月份切换
    - 创建 `SummaryCard` Composable：三列等分布局（收入/支出/结余），88dp 高度，点击导航到报表
    - _需求: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x] 8.3 实现预算进度条区域
    - 创建 `BudgetProgressBar` Composable：水平进度条 + 颜色阈值（绿/橙/红）
    - 仅在用户已设置月度总预算时显示，右上角齿轮图标跳转预算设置
    - _需求: 7.7, 7.8, 7.9, 7.10_

  - [x] 8.4 实现流水列表
    - 创建 `TransactionList` Composable：LazyColumn + 日期分组 Sticky Header
    - 创建 `TransactionItem` Composable：分类图标 + 名称/备注 + 金额，56dp 高度
    - 实现左滑删除（SwipeToDismiss）+ 删除确认对话框
    - 实现空状态展示
    - _需求: 7.11, 7.12, 7.13, 7.14, 7.15_

  - [x] 8.5 实现 Quick Entry Panel（快捷记账面板）
    - 创建 `QuickEntryPanel` BottomSheet Composable（65% 屏幕高度）
    - 实现收入/支出切换 Tab、金额显示区、分类选择网格（两行横向滚动）、附加信息栏（日期/账本/备注 Chip）、数字键盘
    - 实现保存逻辑：验证金额和分类 → 保存 → 关闭面板 → 刷新列表
    - 实现关闭确认（已输入数据时弹出确认对话框）
    - 记住上次选择的分类（通过 MMKV）
    - _需求: 1.1, 1.3, 1.4, 1.5, 1.8, 1.9, 7.16, 7.17, 7.18, 7.19, 7.20_

  - [x] 8.6 实现下拉刷新
    - 使用 `PullToRefreshBox` 实现下拉刷新，刷新摘要、预算进度和流水列表
    - _需求: 7.24, 7.25, 7.26_

  - [x] 8.7 组装首页 HomeScreen
    - 创建 `HomeScreen` Composable，将顶部栏、摘要卡片、预算进度条、流水列表、Quick Entry Panel、FAB 组装到一起
    - 连接 HomeViewModel，通过 `observeAsState()` 订阅 LiveData
    - _需求: 需求 1 全部, 需求 7 全部_

- [x] 9. 检查点 — 首页功能验证
  - 确保所有测试通过，如有问题请向用户确认。

- [x] 10. UI 层 — 分类管理
  - [x] 10.1 实现 CategoryViewModel
    - 创建 `CategoryViewModel`，管理分类列表状态、添加/编辑/删除/排序操作
    - _需求: 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9_

  - [x] 10.2 实现分类管理页面
    - 创建 `CategoryManageScreen`：展示支出/收入分类列表（Tab 切换）
    - 实现添加分类表单（名称 + 图标选择器 + 颜色选择器）
    - 实现长按编辑/删除，删除时弹出迁移确认对话框
    - 实现拖拽排序（`LazyColumn` + `detectDragGestures`）
    - 禁止删除/重命名预设分类
    - _需求: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9_

- [x] 11. UI 层 — 账本管理
  - [x] 11.1 实现 LedgerViewModel
    - 创建 `LedgerViewModel`，管理账本列表状态、创建/编辑/删除/切换操作
    - _需求: 4.1, 4.2, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [x] 11.2 实现账本管理页面
    - 创建 `LedgerManageScreen`：展示账本列表，支持新建/编辑/删除
    - 实现账本创建表单（名称 + 图标 + 模板选择），选择模板时预填推荐分类
    - 实现删除确认（迁移记录或一并删除），禁止删除最后一个账本
    - _需求: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7, 4.8, 4.9_

- [x] 12. UI 层 — 统计报表
  - [x] 12.1 实现 ReportViewModel
    - 创建 `ReportViewModel`，管理图表类型、时间粒度、数据状态
    - 支持按日/周/月/年切换，支持左右滑动切换时间周期
    - _需求: 5.1, 5.2, 5.7, 5.9_

  - [x] 12.2 实现统计报表页面
    - 创建 `ReportScreen`：顶部收入/支出/结余摘要 + 图表区域 + 图表类型切换
    - 使用 Vico 实现饼图（分类占比）、折线图（收支趋势）、柱状图（分类对比）
    - 实现饼图扇区点击展示分类明细列表
    - 实现左右滑动切换时间周期（HorizontalPager 或手势检测）
    - 实现空状态展示
    - _需求: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

- [x] 13. UI 层 — 预算设置
  - [x] 13.1 实现 BudgetViewModel
    - 创建 `BudgetViewModel`，管理预算列表状态、设置/更新预算操作
    - _需求: 6.1, 6.2, 6.3, 6.4, 6.9_

  - [x] 13.2 实现预算设置页面
    - 创建 `BudgetScreen`：展示月度总预算和分类预算列表
    - 每个预算项显示进度条（颜色阈值：<80% 绿色，80%-100% 橙色，>100% 红色）
    - 实现预算金额输入和保存
    - 未设置预算时展示引导卡片
    - _需求: 6.1, 6.2, 6.3, 6.4, 6.9_

- [x] 14. UI 层 — 账单导入
  - [x] 14.1 实现 ImportViewModel
    - 创建 `ImportViewModel`，管理文件选择、解析状态、预览列表、导入操作
    - _需求: 2.1, 2.2, 2.5_

  - [x] 14.2 实现账单导入页面
    - 创建 `BillImportScreen`：文件选择入口 + 解析预览列表
    - 展示待导入 Transaction 列表（金额、日期、对方、分类），支持勾选/取消
    - 未匹配分类的记录标记为"未分类"，允许手动指定
    - 重复记录标记并默认取消勾选
    - 确认导入后显示成功条数
    - 格式错误时显示描述性错误信息
    - _需求: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [x] 15. UI 层 — 我的页面与交易详情
  - [x] 15.1 实现我的页面
    - 创建 `ProfileScreen`：展示功能入口列表（分类管理、账本管理、账单导入、数据导出等）
    - _需求: 7.27_

  - [x] 15.2 实现交易详情/编辑页面
    - 创建 `TransactionDetailScreen`：展示 Transaction 完整信息
    - 支持编辑所有字段（金额、类型、分类、日期、备注）并保存
    - 支持删除操作 + 确认对话框
    - _需求: 1.6, 1.7_

- [x] 16. 预算提醒 — WorkManager 集成
  - [x] 16.1 实现预算提醒 Worker
    - 创建 `BudgetAlertWorker`（CoroutineWorker），调用 `CheckBudgetAlertUseCase` 检查预算阈值
    - 创建 `NotificationHelper`：配置通知渠道 `budget_alerts`，发送本地通知
    - 在新增 Transaction 后通过 WorkManager 触发预算检查
    - 实现月度预算自动重置逻辑（新月份开始时已使用金额归零，保留配置）
    - _需求: 6.5, 6.6, 6.7, 6.8_

- [x] 17. 全局集成与导航串联
  - [x] 17.1 完善导航图和页面串联
    - 在 `AppNavGraph` 中注册所有页面路由和参数传递
    - 确保首页摘要卡片点击 → 报表、齿轮图标 → 预算设置、流水项点击 → 详情页、我的页面入口 → 各管理页面等导航链路畅通
    - 确保账本切换菜单底部"管理账本"入口正确跳转
    - _需求: 7.1, 7.6, 7.9, 7.13, 7.27_

  - [x] 17.2 完善 MainActivity 和 Application 初始化
    - 确保 `MainActivity` 使用 `MainScaffold` + `AppNavGraph` 作为根布局
    - 确保首次启动时预设数据（分类、默认账本）正确插入
    - 确保 MMKV 初始化正常
    - _需求: 3.1, 4.1, 全局基础_

- [x] 18. 最终检查点 — 全功能验证
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的子任务为可选测试任务，可跳过以加速 MVP 开发
- 每个任务引用了具体的需求编号，确保需求可追溯
- 检查点任务用于阶段性验证，确保增量开发的稳定性
- 属性测试验证 CSV 往返一致性等通用正确性属性
- 所有代码使用 Kotlin 编写，UI 使用 Jetpack Compose
