# accountRecord

> 一款基于 Jetpack Compose + Hilt + Room 的个人记账 Android 应用,支持多账本、预算、账单导入导出、热修复。

---

## 简介

accountRecord 是一个面向个人用户的本地记账应用,核心目标是**离线优先、数据自留**:所有交易、分类、预算数据都保存在本地 Room 数据库中,不强依赖云服务。第三方登录(微信 / 支付宝)只用于身份标识,记账数据不离开设备。

---

## 用途

- 📒 **日常记账**:快速录入收入 / 支出,支持多账本(家庭、个人、出差等场景隔离)
- 📊 **报表与趋势**:按月份汇总、按分类拆分、收支趋势图(基于 Vico)
- 💰 **预算管理**:按分类设置月度预算,达到阈值时通过 WorkManager 推送提醒
- 🔄 **账单导入 / 导出**:支持 CSV 格式与第三方账单导入,数据可迁移
- 🔐 **OAuth 登录**:接入微信 OpenSDK / 支付宝 SDK,首次登录后本地持久化
- 🔥 **热修复**:接入腾讯 Tinker,线上 bug 可通过补丁修复,避免每次发新版

---

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 / 工具 | Kotlin 2.1.20, JDK 17, Gradle 8.x + Kotlin DSL, Version Catalog |
| 构建 | AGP 8.10.1, KSP 2.1.20-1.0.32, minSdk 24, targetSdk/compileSdk 35 |
| UI | Jetpack Compose + Material3, Navigation Compose, Compose LiveData |
| 架构 | Clean Architecture (data / domain / ui) + MVVM + UseCase |
| 依赖注入 | Hilt 2.54, Hilt Navigation Compose, Hilt Work |
| 持久化 | Room 2.6.1, MMKV |
| 异步 | Kotlin Coroutines, WorkManager |
| 图表 | Vico 2.0.0-beta.1 |
| 工具 | OpenCSV, Coil(图片), Lottie(动画) |
| 第三方 | 微信 OpenSDK 6.8.28, 支付宝 SDK 15.8.17 |
| 热修复 | Tencent Tinker 1.9.14.27 |
| 测试 | JUnit4, MockK, kotlinx-coroutines-test, Room testing |

---

## 项目结构

```
accountRecord/
├── app/
│   ├── build.gradle.kts                # 模块构建脚本(含 Tinker 配置)
│   ├── proguard-rules.pro
│   ├── tinker/old-apk/                 # 打补丁时归档的基线 apk/mapping/R.txt
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mian/accountrecord/
│       │   ├── AccountRecordApp.kt          # @HiltAndroidApp + Tinker 安装入口
│       │   ├── MainActivity.kt
│       │   ├── data/                        # 数据层
│       │   │   ├── local/
│       │   │   │   ├── db/                  # Room DAO + Database
│       │   │   │   ├── entity/              # Room Entity / 投影对象 / 类型转换
│       │   │   │   └── preferences/         # MMKV 偏好封装
│       │   │   ├── mapper/                  # entity ↔ domain 映射
│       │   │   └── repository/              # Repository 实现
│       │   ├── domain/                      # 领域层
│       │   │   ├── model/                   # 业务模型(纯 Kotlin)
│       │   │   ├── repository/              # Repository 接口
│       │   │   └── usecase/                 # 用例(每个功能一个 UseCase 类)
│       │   ├── di/                          # Hilt 模块
│       │   ├── ui/                          # UI 层(Compose)
│       │   │   ├── auth/                    # 登录 / 欢迎 / WebView
│       │   │   ├── billimport/              # 账单导入
│       │   │   ├── budget/                  # 预算
│       │   │   ├── category/                # 分类管理
│       │   │   ├── components/              # 通用 Composable
│       │   │   ├── detail/                  # 交易详情
│       │   │   ├── home/                    # 首页
│       │   │   ├── ledger/                  # 账本管理
│       │   │   ├── navigation/              # 路由 / Scaffold
│       │   │   ├── profile/                 # 我的 / 设置
│       │   │   ├── report/                  # 报表
│       │   │   └── theme/                   # 主题 / 配色 / Typography
│       │   ├── tinker/                      # Tinker 热修复
│       │   │   ├── SampleApplicationLike.kt
│       │   │   └── TinkerManager.kt         # install / applyPatch / cleanPatch
│       │   ├── util/                        # 工具类(CSV、网络、限流、通知...)
│       │   ├── worker/                      # WorkManager 任务(预算告警)
│       │   └── wxapi/                       # 微信回调 Activity
│       └── res/                             # 资源
├── docs/
│   ├── Android开发艺术探索-面试知识点梳理.md
│   ├── Android热修复方案调研与选型.md
│   ├── Tinker热修复操作手册.md              # 出基线 / 出补丁 / 下发流程
│   ├── lottie资源说明.md
│   ├── 记账App市场调研报告.md
│   └── 预算页面优化方案.md
├── gradle/
│   └── libs.versions.toml                   # Version Catalog
├── build.gradle.kts                         # 根构建(含 Tinker 插件 classpath)
├── settings.gradle.kts                      # 仓库配置(含小米私服 + 镜像)
└── gradle.properties                        # tinkerEnabled 等开关
```

> 完整目录树可在 IDE 内浏览,本图只列出关键节点。

---

## 编译方式

### 环境要求

- **JDK 17**(项目锁定 source/target 17)
- **Android Studio** Iguana / Koala 或更新(支持 AGP 8.10)
- **Android SDK Platform 35** + Build-Tools 35
- 网络可访问以下 Maven 仓库(任一即可):
  - 小米内部 `pkgs.d.xiaomi.net`(在公司网内时优先命中)
  - 清华 / 阿里云镜像(已在 `settings.gradle.kts` 配置好)
  - Google + Maven Central

### 拉代码

```bash
git clone git@github.com:lxm-001/accountRecord.git
cd accountRecord
```

### 命令行构建

```bash
# Debug 包(开发调试)
gradlew assembleDebug

# Release 包(默认 minify 关闭,见 app/build.gradle.kts)
gradlew assembleRelease

# 安装到当前连接的设备
gradlew installDebug

# 跑单元测试
gradlew test

# 跑 Android 仪器测试(需连接设备)
gradlew connectedAndroidTest
```

### 在 Android Studio 中

1. `File` → `Open` 选择项目根目录
2. 等待 Gradle Sync 完成(首次需要下载较多依赖)
3. 选择 `app` 配置 → `Run` 或 `Debug`

### local.properties 关键字段

```properties
sdk.dir=/path/to/Android/Sdk
```

如果使用了 OAuth 真实环境,请把 `AccountRecordApp.WECHAT_APP_ID` 的占位值替换为真实 AppID,并按微信开放平台要求配置签名。

---

## 如何使用

### 首次启动

1. 安装 APK 后启动应用,首次进入欢迎页(Welcome)
2. 登录入口提供 **微信 / 支付宝 OAuth**;未配置真实 AppID 时会停留在登录态校验失败
3. 登录成功后进入主界面,默认创建一个默认账本

### 日常记账

- **首页**:展示当月收支汇总、预算进度、最近交易,顶部可快速切换月份
- **快速录入**:首页 `+` 按钮 → 选择类型(收入 / 支出)→ 选分类 → 输金额 → 备注保存
- **账本切换**:左上角下拉切换账本,支持新增 / 删除 / 重命名(`ui/ledger`)

### 分类与预算

- **分类管理**:`我的` → `分类` → 增删 / 排序 / 改图标(`ui/category`)
- **预算**:`预算` Tab → 按分类设月度预算,接近阈值时 `BudgetAlertWorker` 触发系统通知

### 报表与趋势

- `报表` Tab → 月度收支堆叠图、分类饼图、近 N 月趋势(Vico Compose)

### 账单导入 / 导出

- `我的` → `导入账单`:支持 CSV 文件,内置去重(`DetectDuplicatesUseCase`)
- `我的` → `导出 CSV`:把当前账本所有交易导出为 CSV

### 热修复(运维侧)

线上发现 bug 时不需要发新版,出补丁即可:

```bash
# 出补丁(TINKER_ID 必须与基线包一致)
gradlew tinkerPatchRelease -PtinkerEnabled=true -PTINKER_ID=v1.0.0-base-20260508
```

App 内调用:

```kotlin
TinkerManager.applyPatch(context, downloadedPatchPath)  // 下载到本地后传路径
TinkerManager.cleanPatch(context)                       // 出问题时清除补丁
```

完整流程(包括如何归档基线、CI 接入、常见报错速查)见 [`docs/Tinker热修复操作手册.md`](docs/Tinker热修复操作手册.md)。

---

## 模块化与扩展

- **新增功能**:在 `domain/usecase` 写用例 → `data/repository` 写实现 → `ui/<feature>` 写 Screen + ViewModel → 在 `di/RepositoryModule.kt` 绑定
- **新增数据库表**:`data/local/entity` 加 Entity → `data/local/db` 加 DAO → 升级 `AccountRecordDatabase.version` + 写 Migration
- **新增依赖**:统一加到 `gradle/libs.versions.toml`,然后在 `app/build.gradle.kts` 通过 `libs.xxx` 引用,不要硬编码版本号

---

## 文档目录

| 文档 | 内容 |
|---|---|
| [Tinker热修复操作手册](docs/Tinker热修复操作手册.md) | 出基线、出补丁、下发、CI、常见报错 |
| [Android热修复方案调研与选型](docs/Android热修复方案调研与选型.md) | Tinker / Robust / Sophix / Bugly 对比 |
| [预算页面优化方案](docs/预算页面优化方案.md) | 预算模块设计与改进 |
| [记账App市场调研报告](docs/记账App市场调研报告.md) | 竞品分析 |
| [lottie资源说明](docs/lottie资源说明.md) | 动画资源使用约定 |
| [Android开发艺术探索-面试知识点梳理](docs/Android开发艺术探索-面试知识点梳理.md) | 知识点梳理 |

---

## License

私有项目,暂未开源许可。
