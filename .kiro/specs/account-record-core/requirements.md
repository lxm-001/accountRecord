# 需求文档：AccountRecord 一期核心功能

## 简介

AccountRecord 是一款面向国内用户的 Android 记账应用（Kotlin + Jetpack Compose），一期聚焦五大核心功能：收支记录、分类管理、账本管理、统计报表、预算设置。

产品设计理念：**"极简不极少"**——界面简洁克制，但功能覆盖用户日常记账的完整链路。区别于随手记的功能臃肿和鲨鱼记账的功能单薄，AccountRecord 追求"恰到好处"的平衡。核心创新点包括：支付宝/微信账单导入、智能快捷记账面板、以及"一眼看清"的可视化首页。

### 竞品差异化定位

| 维度 | 钱迹 | 鲨鱼记账 | 随手记 | AccountRecord |
|------|------|---------|-------|---------------|
| 记账速度 | 快 | 极快（3秒） | 一般 | 极快（智能快捷面板） |
| 支付导入 | ❌ | ❌ | ❌ | ✅（支付宝/微信账单CSV） |
| 界面简洁度 | 极简 | 简洁 | 复杂 | 简洁（信息密度适中） |
| 预算管理 | 基础 | 基础 | 完整 | 完整（分类+总额+可视化） |
| 多账本 | ✅ | ❌ | ✅ | ✅（带模板） |
| 广告 | 无 | 有 | 有 | 无 |

## 术语表

- **AccountRecord_App**：AccountRecord 记账应用的 Android 客户端
- **记账面板（Quick_Entry_Panel）**：首页底部弹出的快捷记账输入界面，包含金额键盘、分类选择、日期选择等
- **收支记录（Transaction）**：一条收入或支出的完整记录，包含金额、类型、分类、账本、日期、备注等字段
- **分类（Category）**：对收支记录进行归类的标签，分为系统预设分类和用户自定义分类
- **账本（Ledger）**：用于隔离不同场景收支记录的容器，如"个人日常"、"家庭共用"、"旅行"等
- **预算（Budget）**：用户为指定时间周期（月）或指定分类设定的支出上限金额
- **统计报表（Report）**：以图表形式展示收支数据的可视化页面，包含饼图、折线图、柱状图
- **账单导入（Bill_Import）**：将支付宝或微信导出的 CSV 账单文件解析并批量写入收支记录的功能
- **CSV_Parser**：解析支付宝/微信导出的 CSV 账单文件的解析器
- **CSV_Printer**：将收支记录导出为 CSV 格式文件的格式化器
- **超支提醒（Overspend_Alert）**：当某分类或总预算的实际支出达到或超过预算上限时触发的通知

---

## 需求

### 需求 1：收支记录

**用户故事：** 作为记账用户，我希望能快速记录每一笔收入和支出，以便准确追踪我的资金流向。

#### 验收标准

1. WHEN 用户点击首页的记账按钮, THE Quick_Entry_Panel SHALL 在 300ms 内从底部弹出，展示数字键盘、收入/支出切换、分类选择器、日期选择器和备注输入框
2. THE AccountRecord_App SHALL 为每条 Transaction 存储以下字段：金额（精确到分）、类型（收入/支出）、分类、账本、日期时间、备注（可选）
3. WHEN 用户输入金额并选择分类后点击保存, THE AccountRecord_App SHALL 将该 Transaction 持久化到本地数据库，并在首页流水列表中立即展示该记录
4. WHEN 用户未选择日期, THE AccountRecord_App SHALL 默认使用当前日期时间作为 Transaction 的记录时间
5. WHEN 用户未选择账本, THE AccountRecord_App SHALL 默认将 Transaction 归入用户当前激活的账本
6. WHEN 用户在首页流水列表中点击某条 Transaction, THE AccountRecord_App SHALL 展示该记录的详情页，并支持编辑和删除操作
7. WHEN 用户确认删除一条 Transaction, THE AccountRecord_App SHALL 从数据库中移除该记录，并同步更新首页流水列表和统计数据
8. IF 用户输入的金额为 0 或为空, THEN THE AccountRecord_App SHALL 禁用保存按钮并显示提示文案"请输入金额"
9. IF 用户输入的金额超过 99,999,999.99, THEN THE AccountRecord_App SHALL 禁用保存按钮并显示提示文案"金额超出上限"
10. THE AccountRecord_App SHALL 在首页以时间倒序展示最近的 Transaction 列表，每条记录显示分类图标、分类名称、备注（若有）、金额和日期

### 需求 2：支付宝/微信账单导入

**用户故事：** 作为记账用户，我希望能将支付宝和微信的支付记录批量导入到账本中，以便免去逐条手动录入的麻烦。

#### 验收标准

1. THE AccountRecord_App SHALL 支持导入支付宝导出的 CSV 账单文件和微信支付导出的 CSV 账单文件
2. WHEN 用户选择一个 CSV 账单文件, THE CSV_Parser SHALL 解析该文件并在预览页面展示待导入的 Transaction 列表，包含金额、日期、对方名称和交易分类
3. WHEN CSV 账单中的交易分类与系统预设分类匹配, THE CSV_Parser SHALL 自动映射到对应的 Category
4. WHEN CSV 账单中的交易分类无法匹配系统预设分类, THE AccountRecord_App SHALL 将该 Transaction 标记为"未分类"，并允许用户在预览页手动指定分类
5. WHEN 用户在预览页确认导入, THE AccountRecord_App SHALL 将所有选中的 Transaction 批量写入数据库，并显示导入成功的条数
6. IF CSV 文件格式不符合支付宝或微信的标准导出格式, THEN THE CSV_Parser SHALL 返回描述性错误信息，说明文件格式不正确
7. IF CSV 文件中存在重复交易（金额、日期、对方名称均相同）, THEN THE AccountRecord_App SHALL 在预览页标记疑似重复项，并默认取消勾选这些记录
8. THE CSV_Printer SHALL 将 Transaction 列表格式化为标准 CSV 文件，包含表头行和数据行
9. FOR ALL 有效的 Transaction 列表, 先通过 CSV_Printer 导出为 CSV 再通过 CSV_Parser 解析, SHALL 产生与原始列表等价的 Transaction 集合（往返一致性）

### 需求 3：分类管理

**用户故事：** 作为记账用户，我希望能使用预设分类快速记账，也能自定义分类来适配我的个人消费习惯。

#### 验收标准

1. THE AccountRecord_App SHALL 预设以下支出分类：餐饮、交通、购物、住房、娱乐、医疗、教育、通讯、服饰、日用、社交、宠物、其他；预设以下收入分类：工资、奖金、兼职、投资收益、红包、其他
2. THE AccountRecord_App SHALL 为每个预设分类配置一个可辨识的图标和默认颜色
3. WHEN 用户在分类管理页面点击"添加分类", THE AccountRecord_App SHALL 展示分类创建表单，包含分类名称输入框、图标选择器和颜色选择器
4. WHEN 用户提交分类创建表单且分类名称不为空, THE AccountRecord_App SHALL 将新分类持久化到数据库，并在分类列表和记账面板中立即可用
5. IF 用户提交的分类名称与已有分类名称重复, THEN THE AccountRecord_App SHALL 显示提示文案"该分类名称已存在"并阻止创建
6. WHEN 用户在分类管理页面长按某个自定义分类, THE AccountRecord_App SHALL 展示编辑和删除选项
7. WHEN 用户删除一个自定义分类且该分类下存在关联的 Transaction, THE AccountRecord_App SHALL 弹出确认对话框，提示用户将关联记录迁移到"其他"分类
8. THE AccountRecord_App SHALL 禁止用户删除或重命名系统预设分类
9. WHEN 用户在分类管理页面拖拽分类项, THE AccountRecord_App SHALL 保存新的排列顺序，并在记账面板中按该顺序展示分类

### 需求 4：账本管理

**用户故事：** 作为记账用户，我希望能创建多个账本来分别管理不同场景的收支（如个人、家庭、旅行），以便清晰区分各场景的财务状况。

#### 验收标准

1. THE AccountRecord_App SHALL 在用户首次启动时自动创建一个名为"日常账本"的默认账本，并将该账本设为当前激活账本
2. WHEN 用户在账本管理页面点击"新建账本", THE AccountRecord_App SHALL 展示账本创建表单，包含账本名称输入框、图标选择器和账本模板选择（日常、旅行、家庭、项目、自定义）
3. WHEN 用户选择一个账本模板, THE AccountRecord_App SHALL 根据模板预填充推荐的分类集合（如旅行模板预填：交通、住宿、餐饮、门票、购物）
4. WHEN 用户提交账本创建表单且账本名称不为空, THE AccountRecord_App SHALL 将新账本持久化到数据库
5. IF 用户提交的账本名称与已有账本名称重复, THEN THE AccountRecord_App SHALL 显示提示文案"该账本名称已存在"并阻止创建
6. WHEN 用户在首页切换账本, THE AccountRecord_App SHALL 在 200ms 内刷新首页流水列表和统计摘要，仅展示所选账本的数据
7. WHEN 用户删除一个账本且该账本下存在 Transaction, THE AccountRecord_App SHALL 弹出确认对话框，提示用户选择将关联记录迁移到其他账本或一并删除
8. THE AccountRecord_App SHALL 禁止用户删除最后一个账本，确保至少保留一个账本
9. WHEN 用户编辑账本信息（名称、图标）, THE AccountRecord_App SHALL 持久化修改并在所有引用该账本的页面同步更新显示

### 需求 5：统计报表

**用户故事：** 作为记账用户，我希望通过直观的图表了解我的收支趋势和消费结构，以便做出更合理的财务决策。

#### 验收标准

1. THE Report SHALL 提供三种图表视图：饼图（分类占比）、折线图（收支趋势）、柱状图（分类对比）
2. THE Report SHALL 支持按日、周、月、年四种时间粒度切换数据展示
3. WHEN 用户选择饼图视图, THE Report SHALL 展示当前时间范围内各支出分类的金额占比，每个扇区显示分类名称和百分比
4. WHEN 用户选择折线图视图, THE Report SHALL 展示当前时间范围内收入和支出的趋势变化，收入和支出分别用不同颜色的折线表示
5. WHEN 用户选择柱状图视图, THE Report SHALL 展示当前时间范围内各分类的支出金额对比，柱状图按金额从高到低排列
6. WHEN 用户点击饼图的某个扇区, THE Report SHALL 展示该分类下的 Transaction 明细列表
7. THE Report SHALL 在图表上方展示当前时间范围的收入总额、支出总额和结余（收入总额减去支出总额）
8. WHEN 当前时间范围内无任何 Transaction 数据, THE Report SHALL 展示空状态插画和提示文案"暂无数据，快去记一笔吧"
9. WHILE 用户在报表页面左右滑动, THE Report SHALL 切换到上一个/下一个时间周期（如从3月切换到2月或4月）

### 需求 6：预算设置

**用户故事：** 作为记账用户，我希望能设置月度预算和分类预算上限，并在接近或超出预算时收到提醒，以便控制支出。

#### 验收标准

1. THE AccountRecord_App SHALL 支持设置月度总预算金额和按分类设置独立的月度预算金额
2. WHEN 用户在预算设置页面输入月度总预算金额并保存, THE AccountRecord_App SHALL 持久化该预算配置，并在首页摘要区域展示当月预算使用进度条
3. WHEN 用户为某个支出分类设置预算金额并保存, THE AccountRecord_App SHALL 持久化该分类预算配置
4. THE AccountRecord_App SHALL 在预算页面以进度条形式展示每个已设预算的分类的当月使用情况，进度条颜色根据使用比例变化（低于 80% 为绿色，80%-100% 为橙色，超过 100% 为红色）
5. WHEN 某个分类的当月支出达到该分类预算的 80%, THE Overspend_Alert SHALL 向用户发送一条本地通知，提示"[分类名称] 预算已使用 80%"
6. WHEN 某个分类的当月支出超过该分类预算的 100%, THE Overspend_Alert SHALL 向用户发送一条本地通知，提示"[分类名称] 预算已超支"
7. WHEN 月度总支出超过月度总预算的 100%, THE Overspend_Alert SHALL 向用户发送一条本地通知，提示"本月总预算已超支"
8. WHEN 新的月份开始, THE AccountRecord_App SHALL 自动将所有预算的已使用金额重置为 0，保留预算配置不变
9. IF 用户未设置任何预算, THEN THE AccountRecord_App SHALL 在预算页面展示引导卡片，提示用户设置第一个预算

### 需求 7：首页设计

**用户故事：** 作为记账用户，我希望打开 App 就能一眼看到今日收支概况和最近的记录，以便快速掌握财务状态。

#### 7.1 整体页面结构

首页采用单列纵向滚动布局（LazyColumn），从上到下依次为：顶部栏 → 摘要卡片 → 预算进度条区域 → 流水列表。底部固定浮动记账按钮和导航栏，不随页面滚动。

#### 7.2 顶部栏

##### 验收标准

1. THE AccountRecord_App SHALL 在首页顶部栏左侧展示当前激活账本的名称，点击后弹出账本切换下拉菜单（DropdownMenu），菜单中列出所有账本名称和图标，底部附"管理账本"入口
2. THE AccountRecord_App SHALL 在首页顶部栏右侧展示当前月份文本（格式："2026年4月"），用户可点击左右箭头切换到上一月/下一月，切换后摘要卡片和流水列表同步刷新为对应月份数据
3. WHEN 用户在账本切换菜单中选择另一个账本, THE AccountRecord_App SHALL 在 200ms 内刷新首页所有数据（摘要、预算进度、流水列表），仅展示所选账本的数据

#### 7.3 摘要卡片（Summary Card）

摘要卡片为一个圆角卡片组件（Card with 12dp corner radius），背景使用主题色浅色渐变，内部采用横向三等分布局。

##### 验收标准

4. THE 摘要卡片 SHALL 以横向三列等分布局展示以下信息，每列垂直居中排列：
   - 左列：标签"收入"（灰色小字 12sp）+ 当月收入总额（绿色粗体 20sp，格式：¥12,345.67）
   - 中列：标签"支出"（灰色小字 12sp）+ 当月支出总额（红色粗体 20sp，格式：¥8,901.23）
   - 右列：标签"结余"（灰色小字 12sp）+ 当月结余金额（黑色粗体 20sp，结余为负时显示红色）
5. THE 摘要卡片 SHALL 高度固定为 88dp，左右边距 16dp，上下边距 12dp，卡片内部 padding 为 16dp
6. WHEN 用户点击摘要卡片任意区域, THE AccountRecord_App SHALL 导航到当月统计报表页面（Report）

#### 7.4 预算进度条区域

预算进度条位于摘要卡片下方，仅在用户已设置月度总预算时显示。采用水平长条形进度条（LinearProgressIndicator），而非圆形。

##### 验收标准

7. IF 用户已设置月度总预算, THEN THE AccountRecord_App SHALL 在摘要卡片下方 8dp 处展示预算进度条区域，包含：
   - 顶部行：左侧文本"本月预算"（14sp）+ 右侧文本"¥已花费 / ¥预算总额"（14sp 灰色）
   - 进度条：水平长条形（高度 8dp，圆角 4dp），填充比例 = 当月总支出 / 月度总预算
   - 进度条颜色：使用比例 < 80% 为绿色（#4CAF50），80%-100% 为橙色（#FF9800），> 100% 为红色（#F44336）
8. IF 用户未设置任何预算, THEN THE AccountRecord_App SHALL 隐藏预算进度条区域，不占用首页空间（不显示空白占位）
9. THE 预算进度条区域 SHALL 右上角展示一个齿轮小图标（16dp），用户点击后跳转到预算设置页面
10. THE AccountRecord_App SHALL 不在首页预算区域提供预算开关，预算的启用/禁用统一在"预算设置"页面管理

#### 7.5 流水列表（Transaction List）

流水列表为首页的主体内容区域，采用 LazyColumn 实现，按日期分组展示。

##### 验收标准

11. THE 流水列表 SHALL 按日期分组展示，每个日期分组包含：
    - 分组头（Sticky Header）：左侧显示日期文本（格式："4月7日 星期二"，今天则显示"今天 4月7日"，昨天则显示"昨天 4月6日"），右侧显示该日收支小计（格式："支出 ¥234.50 | 收入 ¥0.00"，灰色 12sp）
    - 分组头背景为浅灰色（#F5F5F5），高度 32dp，左右 padding 16dp
12. THE 流水列表每条 Transaction Item SHALL 采用单行横向布局，高度 56dp，包含以下元素从左到右排列：
    - 分类图标：圆形背景（32dp 直径）内嵌分类对应的 icon（20dp），背景色为该分类的主题色浅色版
    - 分类名称 + 备注区域（垂直排列，占据中间弹性空间）：
      - 第一行：分类名称（黑色 15sp，单行截断）
      - 第二行（可选）：备注文本（灰色 12sp，单行截断，最多显示 20 个字符），若无备注则不显示第二行，分类名称垂直居中
    - 金额（右对齐）：支出显示为"-¥88.00"（红色 16sp），收入显示为"+¥5,000.00"（绿色 16sp）
13. WHEN 用户点击某条 Transaction Item, THE AccountRecord_App SHALL 导航到该记录的详情/编辑页面
14. WHEN 用户在某条 Transaction Item 上向左滑动（Swipe to Dismiss）, THE AccountRecord_App SHALL 露出红色背景的删除按钮区域，用户点击或滑动到底后弹出删除确认对话框
15. WHEN 当前月份无任何 Transaction, THE 流水列表区域 SHALL 展示居中的空状态插画（一个简笔画钱包图标 64dp）和文案"还没有记录，点击下方按钮开始记账吧"（灰色 14sp），插画和文案垂直居中于可用空间

#### 7.6 Quick Entry Panel（快捷记账面板）

Quick_Entry_Panel 为从屏幕底部弹出的半屏 BottomSheet（占屏幕高度约 65%），弹出动画为向上滑入（300ms，DecelerateInterpolator）。

##### 验收标准

16. THE Quick_Entry_Panel SHALL 从上到下包含以下区域：
    - **顶部操作栏**（高度 48dp）：左侧为收入/支出切换 Tab（SegmentedButton 样式，默认选中"支出"，"支出"标签为红色高亮，"收入"标签为绿色高亮），右侧为"保存"按钮（主题色填充圆角按钮）
    - **金额显示区**（高度 64dp）：居中显示当前输入金额（粗体 36sp），默认显示"¥0.00"灰色占位，用户输入时实时更新为黑色，光标闪烁效果
    - **分类选择区**（高度约 160dp）：以两行横向滚动网格（LazyHorizontalGrid，每行 5 个）展示分类图标 + 名称，当前选中分类高亮显示（底部加 2dp 主题色下划线），末尾固定一个"更多"按钮点击后展开全部分类
    - **附加信息栏**（高度 44dp）：横向排列三个可点击 Chip：日期 Chip（显示"今天"或选中日期，点击弹出 DatePicker）、账本 Chip（显示当前账本名称，点击弹出账本选择菜单）、备注 Chip（显示"添加备注"或已输入备注的前 10 个字符，点击弹出文本输入对话框）
    - **数字键盘区**（占据剩余空间）：4列3行标准数字键盘布局，包含 0-9 数字键、小数点键（"."）和退格键（⌫ 图标），每个按键为圆角矩形（8dp radius），按下时有涟漪反馈（Ripple Effect）
17. WHEN 用户在 Quick_Entry_Panel 中点击某个分类图标, THE AccountRecord_App SHALL 高亮该分类（底部主题色下划线）并取消之前选中分类的高亮
18. WHEN 用户点击"保存"按钮且金额有效且已选择分类, THE AccountRecord_App SHALL 保存 Transaction、关闭 Quick_Entry_Panel（向下滑出动画 200ms）、并在首页流水列表顶部插入新记录（带淡入动画 150ms）
19. WHEN 用户在 Quick_Entry_Panel 外部区域点击或向下滑动面板, THE AccountRecord_App SHALL 关闭面板，若已输入数据则弹出"放弃本次记账？"确认对话框
20. THE Quick_Entry_Panel SHALL 记住用户上一次选择的分类，下次打开时默认选中该分类（提升连续记账效率）

#### 7.7 浮动记账按钮

##### 验收标准

21. THE AccountRecord_App SHALL 在首页底部导航栏中央上方固定展示一个圆形浮动按钮（FAB，56dp 直径），图标为"+"号（白色 24dp），背景色为主题色，带 6dp 阴影
22. WHEN 用户点击浮动记账按钮, THE Quick_Entry_Panel SHALL 从底部弹出
23. WHILE Quick_Entry_Panel 处于展开状态, THE 浮动记账按钮 SHALL 隐藏（避免视觉重叠）

#### 7.8 下拉刷新

##### 验收标准

24. WHEN 用户在首页流水列表顶部向下拖拽超过 64dp, THE AccountRecord_App SHALL 触发下拉刷新，展示一个主题色圆形旋转加载指示器（CircularProgressIndicator，直径 36dp），指示器从顶部弹出并在列表上方居中旋转
25. WHILE 下拉刷新进行中, THE 旋转指示器 SHALL 持续旋转动画，直到数据加载完成后自动回弹收起（回弹动画 200ms）
26. WHEN 下拉刷新完成, THE AccountRecord_App SHALL 更新摘要卡片数据、预算进度条和流水列表，若数据无变化则不触发列表滚动

#### 7.9 底部导航栏

##### 验收标准

27. THE AccountRecord_App SHALL 在底部固定展示导航栏（BottomNavigationBar），包含四个入口图标 + 文字标签：首页（房子图标）、报表（柱状图图标）、预算（钱包图标）、我的（人形图标），当前选中项图标和文字为主题色，未选中项为灰色
28. THE 底部导航栏 SHALL 高度为 56dp，中央为浮动记账按钮预留凹陷缺口（notch 样式），使 FAB 自然嵌入导航栏上方
