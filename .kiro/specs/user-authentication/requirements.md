# 需求文档：AccountRecord 用户登录功能

## 简介

本文档定义 AccountRecord 记账应用的用户登录功能模块。登录模块采用第三方 OAuth 授权登录方式（微信登录、支付宝登录），不设置传统的账号密码体系，符合国内主流 App 的登录趋势和 AccountRecord "极简不极少"的产品理念。

本项目为纯工具类 App，不依赖后端服务器。登录流程完全在客户端完成：通过微信/支付宝 SDK 拉起授权，SDK 回调直接返回用户信息（openid/user_id、昵称、头像），客户端据此判定登录成功。登录状态使用 MMKV 本地持久化，退出登录仅清除本地状态。不涉及 Token 管理、Token 刷新、后端通信等服务端逻辑。

## 术语表

- **AccountRecord_App**：AccountRecord 记账应用的 Android 客户端
- **Login_Page**：登录页面，展示第三方登录入口和用户协议
- **OAuth_Manager**：统一管理微信和支付宝 SDK 授权流程的模块，负责拉起 SDK、接收回调、解析用户信息
- **Auth_Storage**：基于 MMKV 的登录状态持久化模块，存储 isLoggedIn 布尔值和用户信息（openid、昵称、头像、登录方式）
- **Auth_State_Manager**：用户认证状态管理器，维护登录态生命周期（未登录、登录中、已登录）
- **User**：用户模型，包含 openid/user_id、昵称、头像 URL、登录方式等字段
- **Click_Guard**：按钮防重复点击控制器，防止用户在短时间内多次触发登录请求
- **Login_Rate_Limiter**：登录频率限制器，防止短时间内过多登录尝试
- **Network_Checker**：网络状态检测工具，在发起登录前检查网络可用性

---

## 需求

### 需求 1：登录页面展示

**用户故事：** 作为未登录用户，我希望看到一个简洁清晰的登录页面，能快速选择微信或支付宝登录，以便高效进入 App。

#### 验收标准

1. WHEN 未登录用户启动 AccountRecord_App, THE Auth_State_Manager SHALL 从 Auth_Storage 读取 isLoggedIn 标记，若值为 false 则导航到 Login_Page
2. THE Login_Page SHALL 采用纵向居中布局，从上到下依次展示：App Logo（居中，72dp）、App 名称"AccountRecord"（20sp 粗体）、欢迎语"极简记账，从这里开始"（14sp 灰色）、微信登录按钮（绿色圆角按钮，图标 + 文字"微信登录"）、支付宝登录按钮（蓝色圆角按钮，图标 + 文字"支付宝登录"）、底部用户协议勾选区域
3. THE Login_Page SHALL 在底部展示用户协议勾选区域，包含一个未勾选的 Checkbox 和文案"我已阅读并同意《用户协议》和《隐私政策》"，其中《用户协议》和《隐私政策》为可点击的超链接文本（主题色）
4. WHILE 用户未勾选用户协议 Checkbox, THE Login_Page SHALL 将微信登录按钮和支付宝登录按钮设为禁用状态（灰色不可点击）
5. WHEN 用户勾选用户协议 Checkbox, THE Login_Page SHALL 立即启用微信登录按钮和支付宝登录按钮（恢复各自主题色，可点击）
6. WHEN 用户在未勾选协议的情况下点击登录按钮, THE Login_Page SHALL 显示底部 Toast 提示"请先阅读并同意用户协议和隐私政策"，同时 Checkbox 区域执行一次抖动动画（水平位移 ±4dp，持续 300ms）引导用户注意

### 需求 2：微信 OAuth 登录

**用户故事：** 作为用户，我希望通过微信账号快速登录 AccountRecord，以便无需注册即可使用 App。

#### 验收标准

1. WHEN 用户点击微信登录按钮, THE OAuth_Manager SHALL 检测设备是否已安装微信 App
2. WHEN 设备已安装微信 App, THE OAuth_Manager SHALL 调用微信 SDK 拉起微信授权页面，请求 snsapi_userinfo 权限
3. WHEN 设备未安装微信 App, THE Login_Page SHALL 显示 Toast 提示"请先安装微信"，登录流程终止
4. WHEN 微信 SDK 授权成功并回调返回 code 和用户信息（openid、昵称、头像）, THE OAuth_Manager SHALL 将用户信息封装为 User 对象，通知 Auth_State_Manager 将状态切换为"已登录"
5. WHEN Auth_State_Manager 状态切换为"已登录", THE Auth_Storage SHALL 将 isLoggedIn 设为 true，并持久化用户信息（openid、昵称、头像 URL、登录方式"wechat"）到 MMKV
6. WHEN 用户信息持久化完成, THE AccountRecord_App SHALL 关闭 Login_Page 并导航到首页
7. WHEN 用户在微信授权页面点击拒绝授权, THE OAuth_Manager SHALL 返回 Login_Page 并显示 Toast 提示"授权已取消"

### 需求 3：支付宝 OAuth 登录

**用户故事：** 作为用户，我希望通过支付宝账号快速登录 AccountRecord，以便有更多登录方式可选。

#### 验收标准

1. WHEN 用户点击支付宝登录按钮, THE OAuth_Manager SHALL 检测设备是否已安装支付宝 App
2. WHEN 设备已安装支付宝 App, THE OAuth_Manager SHALL 调用支付宝 SDK 拉起支付宝授权页面，请求 auth_user 权限
3. WHEN 设备未安装支付宝 App, THE OAuth_Manager SHALL 降级为支付宝 H5 网页授权方式，在 App 内 WebView 中打开支付宝授权页面
4. WHEN 支付宝 SDK 授权成功并回调返回 auth_code 和用户信息（user_id、昵称、头像）, THE OAuth_Manager SHALL 将用户信息封装为 User 对象，通知 Auth_State_Manager 将状态切换为"已登录"
5. WHEN Auth_State_Manager 状态切换为"已登录", THE Auth_Storage SHALL 将 isLoggedIn 设为 true，并持久化用户信息（user_id、昵称、头像 URL、登录方式"alipay"）到 MMKV
6. WHEN 用户信息持久化完成, THE AccountRecord_App SHALL 关闭 Login_Page 并导航到首页
7. WHEN 用户在支付宝授权页面点击取消授权, THE OAuth_Manager SHALL 返回 Login_Page 并显示 Toast 提示"授权已取消"

### 需求 4：登录态保持

**用户故事：** 作为已登录用户，我希望 App 能记住我的登录状态，不需要每次打开都重新登录，以便获得流畅的使用体验。

#### 验收标准

1. WHEN 已登录用户启动 AccountRecord_App, THE Auth_State_Manager SHALL 从 Auth_Storage 读取 isLoggedIn 标记，若值为 true 则直接导航到首页，跳过 Login_Page
2. THE Auth_Storage SHALL 使用 MMKV 持久化存储 isLoggedIn 布尔值和用户信息（openid/user_id、昵称、头像 URL、登录方式），确保 App 重启后登录状态不丢失
3. WHEN Auth_Storage 中 isLoggedIn 为 true 但用户信息字段缺失或损坏, THE Auth_State_Manager SHALL 将 isLoggedIn 重置为 false，清除所有用户信息，并导航到 Login_Page

### 需求 5：退出登录

**用户故事：** 作为已登录用户，我希望能主动退出登录，以便在共用设备时保护我的账户安全。

#### 验收标准

1. THE AccountRecord_App SHALL 在"我的"页面提供"退出登录"按钮，按钮位于页面底部，使用红色文字样式
2. WHEN 用户点击"退出登录"按钮, THE AccountRecord_App SHALL 弹出确认对话框，标题为"确认退出"，内容为"退出后需要重新登录，本地记账数据不会丢失"，包含"取消"和"确认退出"两个按钮
3. WHEN 用户在确认对话框中点击"确认退出", THE Auth_Storage SHALL 将 isLoggedIn 设为 false 并清除 MMKV 中的所有用户信息，Auth_State_Manager 将状态切换为"未登录"，AccountRecord_App 导航到 Login_Page
4. WHEN 用户在确认对话框中点击"取消", THE AccountRecord_App SHALL 关闭对话框，保持当前页面不变
5. THE AccountRecord_App SHALL 在退出登录后保留本地记账数据（Transaction、Category、Ledger、Budget），仅清除用户认证信息

### 需求 6：安全验证

**用户故事：** 作为用户，我希望登录过程有基本的安全保障，防止恶意频繁登录尝试，以便安心使用 App。

#### 验收标准

1. WHEN 同一设备在 1 分钟内发起超过 5 次登录请求, THE Login_Rate_Limiter SHALL 对该设备实施登录冷却，禁止登录 3 分钟，并在 Login_Page 显示倒计时提示"操作过于频繁，请 [剩余秒数] 秒后重试"
2. THE OAuth_Manager SHALL 在 OAuth 授权请求中携带随机生成的 state 参数（UUID 格式），并在回调时验证 state 参数一致性，防止 CSRF 攻击
3. IF OAuth 回调中的 state 参数与请求时不一致, THEN THE OAuth_Manager SHALL 终止登录流程并显示 Toast 提示"登录异常，请重试"

### 需求 7：防呆验证

**用户故事：** 作为用户，我希望登录过程中即使误操作也不会出现异常，以便获得顺畅无忧的登录体验。

#### 验收标准

1. WHEN 用户点击微信登录按钮或支付宝登录按钮, THE Click_Guard SHALL 在按钮点击后立即禁用该按钮 3 秒，防止重复点击触发多次授权请求
2. WHILE 登录请求正在处理中, THE Login_Page SHALL 将登录按钮替换为加载动画（CircularProgressIndicator，24dp，白色），按钮文字隐藏，按钮保持禁用状态
3. WHILE 登录请求正在处理中, THE Login_Page SHALL 在页面中央展示半透明遮罩层（黑色 30% 透明度）和加载提示文案"正在登录..."（白色 16sp），阻止用户与页面其他元素交互
4. IF 登录过程（从拉起 SDK 到收到回调）在 15 秒内未完成, THEN THE OAuth_Manager SHALL 取消当前登录流程，移除加载状态，恢复按钮可点击状态，并显示 Toast 提示"登录超时，请检查网络后重试"
5. IF 登录过程中检测到网络不可用（无 Wi-Fi 且无移动数据）, THEN THE Login_Page SHALL 在用户点击登录按钮时立即显示 Toast 提示"当前无网络连接，请检查网络设置"，不发起登录请求
6. WHEN OAuth 授权流程因第三方 SDK 异常而失败（如微信 SDK 返回错误码）, THE OAuth_Manager SHALL 捕获异常，在 Login_Page 显示 Toast 提示"登录失败，请稍后重试"，并将错误信息记录到本地日志
7. WHEN 用户在登录加载过程中按下系统返回键, THE AccountRecord_App SHALL 弹出确认对话框"是否取消登录？"，用户确认后取消当前登录流程并恢复 Login_Page 初始状态
8. WHEN 登录失败后用户再次点击登录按钮, THE Login_Page SHALL 正常发起新的登录请求，不受前次失败影响（状态完全重置）

### 需求 8：首次登录引导

**用户故事：** 作为首次使用 AccountRecord 的新用户，我希望登录后有简单的引导，以便快速了解 App 的核心功能。

#### 验收标准

1. WHEN 用户首次登录成功（Auth_Storage 中无该用户 openid/user_id 的历史登录记录）, THE AccountRecord_App SHALL 在导航到首页前展示一个欢迎页面，显示用户头像、昵称和文案"欢迎来到 AccountRecord"
2. THE 欢迎页面 SHALL 展示 2 秒后自动跳转到首页，页面底部显示"跳过"文字按钮，用户点击后立即跳转到首页
3. WHEN 用户非首次登录（Auth_Storage 中已有该用户 openid/user_id 的历史登录记录）, THE AccountRecord_App SHALL 跳过欢迎页面，直接导航到首页
4. THE AccountRecord_App SHALL 在用户首次登录时自动创建默认账本"日常账本"（若该用户尚无任何账本）
