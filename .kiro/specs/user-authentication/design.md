# 技术设计文档：AccountRecord 用户登录功能

## 技术栈

- 语言：Kotlin 2.2
- UI 框架：Jetpack Compose + Material 3
- 架构模式：MVVM + Clean Architecture（单模块，按 package 分层）
- 本地存储：MMKV（登录状态 + 用户信息持久化）、Room（用户历史登录记录）
- 依赖注入：Hilt
- 导航：Jetpack Navigation Compose
- 异步：Kotlin Coroutines + Flow
- 状态通知：Jetpack LiveData（ViewModel → UI）
- 第三方 SDK：微信 OpenSDK、支付宝 SDK

> 本项目为纯工具类 App，不依赖后端服务器。无 Retrofit/OkHttp、无 Token 管理、无 EncryptedSharedPreferences。

---

## 1. 模块架构

```
com.mian.accountrecord
├── data/
│   ├── local/
│   │   ├── preferences/
│   │   │   └── AuthPreferences.kt          # 登录状态 MMKV 存储（isLoggedIn + 用户信息）
│   │   └── entity/
│   │       └── UserEntity.kt               # 用户 Room Entity（历史登录记录）
│   └── repository/
│       └── AuthRepositoryImpl.kt           # 认证 Repository 实现
├── domain/
│   ├── model/
│   │   ├── User.kt                         # 用户领域模型
│   │   ├── AuthState.kt                    # 认证状态枚举
│   │   └── OAuthProvider.kt                # OAuth 提供方枚举
│   ├── repository/
│   │   └── AuthRepository.kt              # 认证 Repository 接口
│   └── usecase/
│       ├── LoginWithWeChatUseCase.kt       # 微信登录用例
│       ├── LoginWithAlipayUseCase.kt       # 支付宝登录用例
│       ├── CheckAuthStateUseCase.kt        # 检查登录状态用例
│       ├── LogoutUseCase.kt                # 退出登录用例
│       └── CheckFirstLoginUseCase.kt       # 首次登录检测用例
├── ui/
│   ├── auth/
│   │   ├── LoginScreen.kt                  # 登录页面
│   │   ├── LoginViewModel.kt               # 登录 ViewModel
│   │   └── WelcomeScreen.kt                # 首次登录欢迎页
│   └── navigation/
│       └── Screen.kt                       # 新增 Login、Welcome 路由
├── di/
│   └── AuthModule.kt                       # 认证相关 Hilt 模块
└── util/
    ├── ClickGuard.kt                       # 按钮防重复点击
    ├── LoginRateLimiter.kt                 # 登录频率限制
    └── NetworkChecker.kt                   # 网络状态检测
```

### 与现有架构的集成点

| 集成点 | 现有模块 | 变更方式 |
|--------|---------|---------|
| 导航 | `AppNavGraph.kt` | 新增 Login、Welcome 路由，修改 startDestination 逻辑 |
| 主框架 | `MainScaffold.kt` | 登录页和欢迎页不显示底部导航栏和 FAB |
| 我的页面 | `ProfileScreen.kt` | 新增"退出登录"按钮和用户信息展示 |
| DI | `RepositoryModule.kt` | 新增 AuthRepository 绑定 |
| Application | `AccountRecordApp.kt` | 初始化微信 SDK |
| 数据库 | `AccountRecordDatabase.kt` | 新增 UserEntity 和 UserDao |

---

## 2. 数据模型设计

### 2.1 用户 Entity（Room，用于历史登录记录判断首次登录）

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val openId: String,          // 微信 openid / 支付宝 user_id
    val nickname: String,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    @ColumnInfo(name = "oauth_provider") val oauthProvider: String,  // "wechat" / "alipay"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_login_at") val lastLoginAt: Long = System.currentTimeMillis()
)
```

### 2.2 UserDao

```kotlin
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE openId = :openId LIMIT 1")
    suspend fun getByOpenId(openId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users WHERE openId = :openId")
    suspend fun hasLoginHistory(openId: String): Int
}
```

### 2.3 AuthPreferences（MMKV 登录状态存储）

```kotlin
@Singleton
class AuthPreferences @Inject constructor() {

    private val mmkv: MMKV = MMKV.mmkvWithID("auth_prefs")

    var isLoggedIn: Boolean
        get() = mmkv.decodeBool(KEY_IS_LOGGED_IN, false)
        set(value) { mmkv.encode(KEY_IS_LOGGED_IN, value) }

    var currentOpenId: String?
        get() = mmkv.decodeString(KEY_CURRENT_OPEN_ID, null)
        set(value) { mmkv.encode(KEY_CURRENT_OPEN_ID, value ?: "") }

    var currentNickname: String?
        get() = mmkv.decodeString(KEY_CURRENT_NICKNAME, null)
        set(value) { mmkv.encode(KEY_CURRENT_NICKNAME, value ?: "") }

    var currentAvatarUrl: String?
        get() = mmkv.decodeString(KEY_CURRENT_AVATAR_URL, null)
        set(value) { mmkv.encode(KEY_CURRENT_AVATAR_URL, value ?: "") }

    var currentProvider: String?
        get() = mmkv.decodeString(KEY_CURRENT_PROVIDER, null)
        set(value) { mmkv.encode(KEY_CURRENT_PROVIDER, value ?: "") }

    fun saveLoginInfo(openId: String, nickname: String, avatarUrl: String?, provider: String) {
        isLoggedIn = true
        currentOpenId = openId
        currentNickname = nickname
        currentAvatarUrl = avatarUrl
        currentProvider = provider
    }

    fun clearAll() {
        mmkv.clearAll()
    }

    fun isLoginInfoValid(): Boolean {
        return isLoggedIn && !currentOpenId.isNullOrEmpty() && !currentNickname.isNullOrEmpty()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_OPEN_ID = "current_open_id"
        private const val KEY_CURRENT_NICKNAME = "current_nickname"
        private const val KEY_CURRENT_AVATAR_URL = "current_avatar_url"
        private const val KEY_CURRENT_PROVIDER = "current_provider"
    }
}
```

---

## 3. 领域模型

```kotlin
data class User(
    val openId: String,             // 微信 openid / 支付宝 user_id
    val nickname: String,
    val avatarUrl: String?,
    val oauthProvider: OAuthProvider
)

enum class OAuthProvider { WECHAT, ALIPAY }

enum class AuthState {
    UNAUTHENTICATED,    // 未登录
    AUTHENTICATING,     // 登录中
    AUTHENTICATED       // 已登录
}

data class OAuthResult(
    val openId: String,             // SDK 回调返回的 openid / user_id
    val nickname: String,
    val avatarUrl: String?,
    val state: String,              // CSRF state 参数
    val provider: OAuthProvider
)

data class LoginResult(
    val user: User,
    val isFirstLogin: Boolean
)
```

---

## 4. Repository 接口与实现

### 4.1 AuthRepository 接口

```kotlin
interface AuthRepository {
    suspend fun saveLoginInfo(user: User): Result<Unit>
    suspend fun clearAuth(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun hasLoginHistory(openId: String): Boolean
    suspend fun recordLogin(user: User)
}
```

### 4.2 AuthRepositoryImpl

```kotlin
class AuthRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun saveLoginInfo(user: User): Result<Unit> = runCatching {
        authPreferences.saveLoginInfo(
            openId = user.openId,
            nickname = user.nickname,
            avatarUrl = user.avatarUrl,
            provider = user.oauthProvider.name.lowercase()
        )
    }

    override suspend fun clearAuth(): Result<Unit> = runCatching {
        authPreferences.clearAll()
    }

    override suspend fun getCurrentUser(): User? {
        if (!authPreferences.isLoginInfoValid()) return null
        val openId = authPreferences.currentOpenId ?: return null
        val nickname = authPreferences.currentNickname ?: return null
        val provider = when (authPreferences.currentProvider) {
            "wechat" -> OAuthProvider.WECHAT
            "alipay" -> OAuthProvider.ALIPAY
            else -> return null
        }
        return User(openId, nickname, authPreferences.currentAvatarUrl, provider)
    }

    override suspend fun hasLoginHistory(openId: String): Boolean {
        return userDao.hasLoginHistory(openId) > 0
    }

    override suspend fun recordLogin(user: User) {
        userDao.upsert(
            UserEntity(
                openId = user.openId,
                nickname = user.nickname,
                avatarUrl = user.avatarUrl,
                oauthProvider = user.oauthProvider.name.lowercase(),
                lastLoginAt = System.currentTimeMillis()
            )
        )
    }
}
```

---

## 5. 核心用例

```kotlin
class LoginWithWeChatUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(oauthResult: OAuthResult): Result<LoginResult> = runCatching {
        val user = User(
            openId = oauthResult.openId,
            nickname = oauthResult.nickname,
            avatarUrl = oauthResult.avatarUrl,
            oauthProvider = OAuthProvider.WECHAT
        )
        val isFirstLogin = !authRepository.hasLoginHistory(user.openId)
        authRepository.saveLoginInfo(user).getOrThrow()
        authRepository.recordLogin(user)
        LoginResult(user, isFirstLogin)
    }
}

class LoginWithAlipayUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(oauthResult: OAuthResult): Result<LoginResult> = runCatching {
        val user = User(
            openId = oauthResult.openId,
            nickname = oauthResult.nickname,
            avatarUrl = oauthResult.avatarUrl,
            oauthProvider = OAuthProvider.ALIPAY
        )
        val isFirstLogin = !authRepository.hasLoginHistory(user.openId)
        authRepository.saveLoginInfo(user).getOrThrow()
        authRepository.recordLogin(user)
        LoginResult(user, isFirstLogin)
    }
}

class CheckAuthStateUseCase @Inject constructor(
    private val authPreferences: AuthPreferences
) {
    operator fun invoke(): AuthState {
        return if (authPreferences.isLoginInfoValid()) {
            AuthState.AUTHENTICATED
        } else {
            AuthState.UNAUTHENTICATED
        }
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.clearAuth()
    }
}

class CheckFirstLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(openId: String): Boolean {
        return !authRepository.hasLoginHistory(openId)
    }
}
```

---

## 6. ViewModel 设计

### 6.1 LoginViewModel

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithWeChat: LoginWithWeChatUseCase,
    private val loginWithAlipay: LoginWithAlipayUseCase,
    private val checkAuthState: CheckAuthStateUseCase,
    private val networkChecker: NetworkChecker,
    private val loginRateLimiter: LoginRateLimiter
) : ViewModel() {

    data class LoginUiState(
        val authState: AuthState = AuthState.UNAUTHENTICATED,
        val isAgreementChecked: Boolean = false,
        val isLoading: Boolean = false,
        val loadingMessage: String = "",
        val errorMessage: String? = null,
        val loginResult: LoginResult? = null,
        val cooldownSeconds: Int = 0,           // 登录冷却倒计时
        val showCancelDialog: Boolean = false    // 取消登录确认对话框
    )

    private val _uiState = MutableLiveData(LoginUiState())
    val uiState: LiveData<LoginUiState> = _uiState

    // 生成 CSRF state 参数
    private var currentOAuthState: String = ""

    fun checkInitialAuthState() {
        val state = checkAuthState()
        _uiState.value = _uiState.value?.copy(authState = state)
    }

    fun onAgreementCheckedChange(checked: Boolean) {
        _uiState.value = _uiState.value?.copy(isAgreementChecked = checked)
    }

    fun onWeChatLoginClick() {
        // 1. 检查网络 → 2. 检查频率限制 → 3. 生成 state → 4. 拉起微信 SDK
        // 15 秒超时处理（withTimeoutOrNull）
    }

    fun onAlipayLoginClick() {
        // 同上，拉起支付宝 SDK
    }

    fun onOAuthCallback(oauthResult: OAuthResult) {
        // 1. 验证 state 一致性 → 2. 调用登录用例 → 3. 更新 UI 状态
        viewModelScope.launch {
            if (oauthResult.state != currentOAuthState) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "登录异常，请重试"
                )
                return@launch
            }
            val useCase = when (oauthResult.provider) {
                OAuthProvider.WECHAT -> loginWithWeChat
                OAuthProvider.ALIPAY -> loginWithAlipay
            }
            useCase(oauthResult).onSuccess { result ->
                _uiState.value = _uiState.value?.copy(
                    authState = AuthState.AUTHENTICATED,
                    isLoading = false,
                    loginResult = result
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = "登录失败，请稍后重试"
                )
            }
        }
    }

    fun onCancelLogin() { /* 取消登录 */ }
    fun onErrorDismissed() { /* 清除错误消息 */ }
}
```

---

## 7. UI 设计

### 7.1 登录页面布局

```kotlin
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: (isFirstLogin: Boolean) -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val uiState by viewModel.uiState.observeAsState(LoginViewModel.LoginUiState())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo（72dp）
            // App 名称 "AccountRecord"（20sp 粗体）
            // 欢迎语 "极简记账，从这里开始"（14sp 灰色）
            Spacer(modifier = Modifier.height(48.dp))
            // 微信登录按钮（绿色圆角，图标 + "微信登录"）
            // 支付宝登录按钮（蓝色圆角，图标 + "支付宝登录"）
            Spacer(modifier = Modifier.weight(1f))
            // 用户协议勾选区域
        }

        // 加载遮罩层（半透明黑色 30%，"正在登录..."）
        if (uiState.isLoading) {
            LoadingOverlay(message = uiState.loadingMessage)
        }
    }

    // 返回键取消登录确认对话框
    BackHandler(enabled = uiState.isLoading) {
        viewModel.onShowCancelDialog()
    }
}
```

### 7.2 欢迎页面

```kotlin
@Composable
fun WelcomeScreen(
    nickname: String,
    avatarUrl: String?,
    onTimeout: () -> Unit,
    onSkip: () -> Unit
) {
    // 2 秒后自动跳转
    LaunchedEffect(Unit) {
        delay(2000)
        onTimeout()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 用户头像（圆形，80dp）
        // 用户昵称（20sp）
        // "欢迎来到 AccountRecord"（16sp 灰色）
    }

    // 底部 "跳过" 按钮
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TextButton(onClick = onSkip, modifier = Modifier.padding(bottom = 32.dp)) {
            Text("跳过")
        }
    }
}
```

---

## 8. 导航设计

### 新增路由

```kotlin
sealed class Screen(val route: String) {
    // ... 现有路由 ...
    object Login : Screen("login")
    object Welcome : Screen("welcome/{nickname}/{avatarUrl}") {
        fun createRoute(nickname: String, avatarUrl: String?) =
            "welcome/$nickname/${avatarUrl ?: "none"}"
    }
}
```

### 导航流程

```
App 启动
  ├── CheckAuthState → AUTHENTICATED → Home
  ├── CheckAuthState → UNAUTHENTICATED → Login
  └── Login 成功
       ├── isFirstLogin = true → Welcome → Home
       └── isFirstLogin = false → Home
```

startDestination 根据 AuthState 动态决定：在 `AppNavGraph` 外层通过 `CheckAuthStateUseCase` 判断初始路由。

---

## 9. 安全与防呆设计

### 9.1 登录频率限制（客户端侧）

```kotlin
@Singleton
class LoginRateLimiter @Inject constructor() {
    private val loginTimestamps = mutableListOf<Long>()
    private var cooldownUntil: Long = 0L

    fun canLogin(): Boolean {
        if (System.currentTimeMillis() < cooldownUntil) return false
        return true
    }

    fun recordLoginAttempt() {
        val now = System.currentTimeMillis()
        loginTimestamps.add(now)
        loginTimestamps.removeAll { now - it > 60_000 }  // 保留 1 分钟内的记录
        if (loginTimestamps.size > 5) {
            cooldownUntil = now + 3 * 60_000  // 冷却 3 分钟
        }
    }

    fun getCooldownRemainingSeconds(): Int {
        val remaining = cooldownUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
}
```

### 9.2 CSRF State 验证

OAuth 请求时生成 `UUID.randomUUID().toString()` 作为 state 参数，回调时验证一致性。不一致则终止登录并提示用户。

### 9.3 按钮防重复点击

```kotlin
class ClickGuard {
    private var lastClickTime = 0L
    private val interval = 3000L  // 3 秒

    fun isClickAllowed(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < interval) return false
        lastClickTime = now
        return true
    }

    fun reset() {
        lastClickTime = 0L
    }
}
```

### 9.4 登录超时处理

在 ViewModel 中使用 `withTimeoutOrNull(15_000)` 包裹登录流程（从拉起 SDK 到收到回调），超时后取消协程、恢复 UI 状态、显示超时提示。

### 9.5 网络检测

```kotlin
class NetworkChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

---

## 10. 新增依赖清单

```toml
# gradle/libs.versions.toml 新增
# 无需新增 Retrofit/OkHttp/security-crypto 依赖
# 微信 SDK 和支付宝 SDK 需要在 build.gradle.kts 中手动添加
```

微信 SDK 和支付宝 SDK 依赖在 `app/build.gradle.kts` 中直接添加：

```kotlin
// 微信 OpenSDK
implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.28")
// 支付宝 SDK（通过本地 aar 或 Maven 仓库）
implementation("com.alipay.sdk:alipaysdk-android:15.8.17")
```

---

## 11. 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 登录状态存储 | MMKV | 项目已使用 MMKV，性能优于 SharedPreferences，无需加密（不存储敏感 Token） |
| 历史登录记录 | Room UserEntity | 复用现有 Room 数据库，用于判断首次登录 |
| 无后端通信 | 纯客户端方案 | 工具类 App，SDK 回调直接返回用户信息，无需后端换 Token |
| 无 Token 管理 | 不使用 access_token/refresh_token | 无后端 API 调用，登录状态仅用 isLoggedIn 布尔值表示 |
| 无 EncryptedSharedPreferences | 使用 MMKV | 不存储敏感 Token，MMKV 足够安全且性能更优 |
| 频率限制 | 纯客户端 LoginRateLimiter | 无后端支持，客户端侧防止频繁登录尝试 |
| 首次登录检测 | Room 查询用户记录 | 本地判断，无需额外网络请求 |
| 用户协议 | 前置勾选 | 符合国内 App 合规要求，未勾选禁用登录按钮 |
| 支付宝降级 | H5 WebView | 未安装支付宝时仍可登录，提升覆盖率 |

---

## 12. 正确性属性

### 12.1 Auth 状态一致性

对于任意 User 对象 u：
- `saveLoginInfo(u)` 后 `isLoginInfoValid()` 返回 true
- `clearAll()` 后 `isLoginInfoValid()` 返回 false
- `saveLoginInfo(u)` 后 `getCurrentUser()` 返回的 User 与 u 的 openId、nickname、avatarUrl、oauthProvider 一致

### 12.2 登录频率限制幂等性

- 连续调用 `canLogin()` 不改变内部状态（只有 `recordLoginAttempt()` 改变状态）
- 5 次 `recordLoginAttempt()` 后 `canLogin()` 返回 false
- 冷却时间过后 `canLogin()` 恢复为 true

### 12.3 ClickGuard 时间窗口

- 首次调用 `isClickAllowed()` 返回 true
- 3 秒内再次调用返回 false
- 3 秒后调用返回 true
- `reset()` 后立即调用返回 true
