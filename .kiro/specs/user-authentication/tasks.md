# 实现计划：AccountRecord 用户登录功能

## 概述

基于现有 MVVM + Clean Architecture 架构，从基础设施（MMKV 登录存储、Room 用户记录）到领域层（认证模型、用例）、数据层（Repository）、UI 层（登录页、欢迎页、导航集成）逐步构建纯客户端用户登录功能。无后端服务器通信、无 Token 管理、无网络层依赖。每个阶段确保代码可编译、可测试，最终将认证模块与现有 App 完整串联。

## 任务

- [x] 1. 基础设施 — 存储层
  - [x] 1.1 创建 AuthPreferences（MMKV 登录状态存储）
    - 在 `data/local/preferences/` 下创建 `AuthPreferences.kt`
    - 使用 `MMKV.mmkvWithID("auth_prefs")` 独立实例存储登录状态
    - 实现字段：isLoggedIn（布尔值）、currentOpenId、currentNickname、currentAvatarUrl、currentProvider
    - 实现 `saveLoginInfo()`、`clearAll()`、`isLoginInfoValid()` 方法
    - _需求: 2.5, 3.5, 4.2, 4.3, 5.3_

  - [x] 1.2 创建 UserEntity 和 UserDao
    - 在 `data/local/entity/` 下创建 `UserEntity.kt`，包含 openId（主键）、nickname、avatarUrl、oauthProvider、createdAt、lastLoginAt 字段
    - 在 `data/local/db/` 下创建 `UserDao.kt`，实现 `getByOpenId`、`upsert`、`hasLoginHistory` 方法
    - 在 `AccountRecordDatabase` 中注册 UserEntity 和 UserDao，升级数据库版本
    - _需求: 8.1, 8.3_

- [x] 2. 领域层 — 认证模型与 Repository 接口
  - [x] 2.1 创建认证领域模型
    - 在 `domain/model/` 下创建 `User.kt`（openId、nickname、avatarUrl、oauthProvider）
    - 创建 `AuthState.kt` 枚举（UNAUTHENTICATED、AUTHENTICATING、AUTHENTICATED）
    - 创建 `OAuthProvider.kt` 枚举（WECHAT、ALIPAY）
    - 创建 `OAuthResult.kt`（openId、nickname、avatarUrl、state、provider）和 `LoginResult.kt`（user、isFirstLogin）
    - _需求: 2.4, 3.4_

  - [x] 2.2 创建 AuthRepository 接口
    - 在 `domain/repository/` 下创建 `AuthRepository.kt`
    - 定义 `saveLoginInfo`、`clearAuth`、`getCurrentUser`、`hasLoginHistory`、`recordLogin` 方法
    - _需求: 2.5, 3.5, 4.1, 5.3_

- [x] 3. 数据层 — Repository 实现与 DI
  - [x] 3.1 实现 AuthRepositoryImpl
    - 在 `data/repository/` 下创建 `AuthRepositoryImpl.kt`
    - 注入 AuthPreferences、UserDao
    - 实现 `saveLoginInfo`：调用 AuthPreferences.saveLoginInfo() 持久化用户信息
    - 实现 `clearAuth`：调用 AuthPreferences.clearAll() 清除登录状态
    - 实现 `getCurrentUser`：从 AuthPreferences 读取用户信息，校验完整性
    - 实现 `hasLoginHistory`/`recordLogin`：通过 UserDao 操作 Room 数据库
    - _需求: 2.4, 2.5, 3.4, 3.5, 4.1, 4.2, 4.3, 5.3, 5.5, 8.1, 8.3_

  - [x] 3.2 创建 AuthModule Hilt 模块
    - 在 `di/` 下创建 `AuthModule.kt`
    - 提供 AuthPreferences 实例（单例）
    - 提供 UserDao 实例（从 AccountRecordDatabase 获取）
    - 在 `RepositoryModule` 中新增 AuthRepository → AuthRepositoryImpl 绑定
    - _需求: 全局基础_

- [x] 4. 领域层 — 认证用例实现
  - [x] 4.1 实现 LoginWithWeChatUseCase 和 LoginWithAlipayUseCase
    - 创建 `LoginWithWeChatUseCase`：接收 OAuthResult，构造 User，判断首次登录，保存登录信息，记录登录历史
    - 创建 `LoginWithAlipayUseCase`：同上，provider 为 ALIPAY
    - _需求: 2.4, 2.5, 3.4, 3.5_

  - [x] 4.2 实现 CheckAuthStateUseCase
    - 创建 `CheckAuthStateUseCase`：从 AuthPreferences 读取 isLoggedIn 和用户信息完整性，返回 AuthState
    - _需求: 1.1, 4.1, 4.3_

  - [x] 4.3 实现 LogoutUseCase
    - 创建 `LogoutUseCase`：调用 AuthRepository.clearAuth() 清除 MMKV 中的登录状态
    - _需求: 5.3, 5.5_

  - [x] 4.4 实现 CheckFirstLoginUseCase
    - 创建 `CheckFirstLoginUseCase`：通过 AuthRepository.hasLoginHistory 判断是否首次登录
    - _需求: 8.1, 8.3_

- [x] 5. 工具类 — 安全与防呆
  - [x] 5.1 实现 LoginRateLimiter
    - 在 `util/` 下创建 `LoginRateLimiter.kt`
    - 记录 1 分钟内登录次数，超过 5 次触发 3 分钟冷却
    - 提供 `canLogin()`、`recordLoginAttempt()`、`getCooldownRemainingSeconds()` 方法
    - _需求: 6.1_

  - [x] 5.2 实现 ClickGuard 和 NetworkChecker
    - 在 `util/` 下创建 `ClickGuard.kt`：按钮点击后 3 秒内禁用重复点击
    - 在 `util/` 下创建 `NetworkChecker.kt`：通过 ConnectivityManager 检测网络可用性
    - _需求: 7.1, 7.5_

- [x] 6. 检查点 — 数据层与领域层验证
  - 确保项目可编译，所有新增类无语法错误。如有问题请向用户确认。

- [x] 7. UI 层 — 登录页面
  - [x] 7.1 实现 LoginViewModel
    - 创建 `ui/auth/LoginViewModel.kt`
    - 定义 `LoginUiState`（authState、isAgreementChecked、isLoading、loadingMessage、errorMessage、loginResult、cooldownSeconds、showCancelDialog）
    - 注入 LoginWithWeChatUseCase、LoginWithAlipayUseCase、CheckAuthStateUseCase、NetworkChecker、LoginRateLimiter
    - 实现 `checkInitialAuthState()`：启动时检查登录态
    - 实现 `onWeChatLoginClick()`/`onAlipayLoginClick()`：网络检测 → 频率检测 → 生成 CSRF state → 拉起 SDK → 15 秒超时处理
    - 实现 `onOAuthCallback()`：验证 state 一致性 → 调用登录用例 → 更新状态
    - 实现 `onAgreementCheckedChange()`、`onCancelLogin()`、`onErrorDismissed()`
    - 实现冷却倒计时（coroutine delay 循环）
    - _需求: 1.1, 2.1-2.7, 3.1-3.7, 6.1, 6.2, 6.3, 7.1-7.8_

  - [x] 7.2 实现 LoginScreen 登录页面
    - 创建 `ui/auth/LoginScreen.kt`
    - 纵向居中布局：App Logo（72dp）→ App 名称（20sp 粗体）→ 欢迎语（14sp 灰色）→ 微信登录按钮（绿色圆角）→ 支付宝登录按钮（蓝色圆角）→ 用户协议勾选区域
    - 用户协议区域：Checkbox + "我已阅读并同意《用户协议》和《隐私政策》"，超链接可点击
    - 未勾选协议时按钮禁用（灰色），点击时 Toast 提示 + Checkbox 抖动动画（±4dp，300ms）
    - 登录中状态：按钮替换为 CircularProgressIndicator（24dp 白色），半透明遮罩层 + "正在登录..."
    - 冷却状态：显示倒计时提示"操作过于频繁，请 [秒数] 秒后重试"
    - 返回键处理：登录中弹出"是否取消登录？"确认对话框
    - _需求: 1.2, 1.3, 1.4, 1.5, 1.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8_

  - [x] 7.3 实现 WelcomeScreen 欢迎页面
    - 创建 `ui/auth/WelcomeScreen.kt`
    - 居中展示用户头像（圆形 80dp）、昵称（20sp）、"欢迎来到 AccountRecord"（16sp 灰色）
    - 2 秒后自动跳转首页（LaunchedEffect + delay）
    - 底部"跳过"文字按钮，点击立即跳转
    - _需求: 8.1, 8.2_

- [x] 8. UI 层 — 导航集成与退出登录
  - [x] 8.1 更新导航图
    - 在 `Screen` sealed class 中新增 `Login` 和 `Welcome` 路由
    - 在 `AppNavGraph` 中注册 LoginScreen 和 WelcomeScreen 路由
    - 修改 startDestination 逻辑：根据 CheckAuthStateUseCase 结果动态决定初始路由（Login 或 Home）
    - 登录成功后根据 isFirstLogin 导航到 Welcome 或 Home
    - 登录页和欢迎页不显示底部导航栏和 FAB（在 MainScaffold 中根据当前路由控制）
    - _需求: 1.1, 2.6, 3.6, 8.1, 8.3_

  - [x] 8.2 更新 ProfileScreen 添加退出登录
    - 在 `ProfileScreen` 顶部新增用户信息展示区域（头像 + 昵称 + 登录方式标签）
    - 在 `ProfileScreen` 底部新增"退出登录"按钮（红色文字）
    - 点击退出登录弹出确认对话框：标题"确认退出"，内容"退出后需要重新登录，本地记账数据不会丢失"，"取消"和"确认退出"按钮
    - 确认退出后调用 LogoutUseCase，导航到 LoginScreen
    - _需求: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 8.3 首次登录创建默认账本
    - 首次登录时检查用户是否有账本，若无则自动创建默认账本"日常账本"
    - _需求: 8.4_

- [x] 9. 微信 SDK 与支付宝 SDK 集成
  - [x] 9.1 集成微信 OpenSDK
    - 在 `app/build.gradle.kts` 中添加微信 OpenSDK 依赖
    - 在 `AccountRecordApp.onCreate()` 中注册微信 SDK（appId）
    - 创建 `wxapi/WXEntryActivity.kt` 处理微信 OAuth 回调
    - 实现微信 App 安装检测逻辑
    - 回调中解析 code 和用户信息，构造 OAuthResult 传递给 LoginViewModel
    - _需求: 2.1, 2.2, 2.3, 2.4, 2.7_

  - [x] 9.2 集成支付宝 SDK
    - 在 `app/build.gradle.kts` 中添加支付宝 SDK 依赖
    - 实现支付宝 App 授权调用和 H5 降级逻辑（未安装支付宝时使用 WebView）
    - 处理支付宝 OAuth 回调，解析 auth_code 和用户信息
    - 构造 OAuthResult 传递给 LoginViewModel
    - _需求: 3.1, 3.2, 3.3, 3.4, 3.7_

- [x] 10. 检查点 — 全功能验证
  - 确保项目可编译，所有页面可正常导航。如有问题请向用户确认。

- [ ]* 11. 单元测试
  - [ ] 11.1 编写认证用例单元测试
    - 测试 CheckAuthStateUseCase：isLoginInfoValid 为 true → AUTHENTICATED、为 false → UNAUTHENTICATED
    - 测试 LogoutUseCase：验证 clearAuth 被调用
    - 测试 LoginWithWeChatUseCase/LoginWithAlipayUseCase：验证 saveLoginInfo 和 recordLogin 被调用，首次登录判断正确
    - _需求: 2.4, 2.5, 3.4, 3.5, 4.1, 5.3_

  - [ ]* 11.2 编写安全工具类单元测试
    - 测试 LoginRateLimiter：5 次内允许登录、超过 5 次触发冷却、冷却结束后恢复
    - 测试 ClickGuard：3 秒内重复点击被拦截、3 秒后允许点击
    - _需求: 6.1, 7.1_

  - [ ]* 11.3 编写 AuthPreferences 单元测试
    - 测试 saveLoginInfo 后 isLoginInfoValid 返回 true
    - 测试 clearAll 后 isLoginInfoValid 返回 false
    - 测试 saveLoginInfo 后各字段值正确
    - _需求: 4.2, 4.3_

- [x] 12. 最终检查点 — 全功能验证
  - 确保所有测试通过，如有问题请向用户确认。

## 备注

- 标记 `*` 的子任务为可选测试任务，可跳过以加速 MVP 开发
- 每个任务引用了具体的需求编号，确保需求可追溯
- 检查点任务用于阶段性验证，确保增量开发的稳定性
- 本项目为纯工具类 App，不依赖后端服务器，无 Retrofit/OkHttp/Token 管理相关代码
- 登录状态使用 MMKV 持久化（isLoggedIn 布尔值 + 用户信息），退出登录仅清除 MMKV 数据
- 微信/支付宝 SDK 集成需要对应平台的开发者账号和 AppId 配置
- 退出登录仅清除认证信息，保留本地记账数据（Transaction、Category、Ledger、Budget）
