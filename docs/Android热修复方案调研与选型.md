# Android 热修复方案调研与选型（免费方案）

## 一、背景

本项目（AccountRecord 记账 App）当前技术栈：

| 项目 | 版本 |
|------|------|
| Kotlin | 2.1.20 |
| AGP | 8.10.1 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| UI 框架 | Jetpack Compose |
| DI | Hilt 2.54.1 (KSP) |
| 构建工具 | Gradle Kotlin DSL |

热修复的核心诉求：线上版本出现紧急 Bug 时，无需用户重新安装即可下发补丁修复，缩短故障响应时间。**要求方案完全免费、开源。**

---

## 二、免费热修复方案概览

### 1. Tinker（腾讯 / 微信）

- GitHub：[Tencent/tinker](https://github.com/Tencent/tinker)  ⭐ 17k+
- 最新版本：**v1.9.15.2**（持续维护中，已适配 Android 15、新 Dex 格式 037-039）
- 原理：全量 DEX 差分 + BsDiff 资源差分，冷启动时合成新 DEX 加载
- 修复范围：**代码（DEX）+ 资源 + SO 库**
- 生效方式：**重启生效**
- 开源协议：BSD
- 费用：**完全免费开源**

**优势：**
- 修复范围最广，DEX / 资源 / SO 全覆盖
- 微信亿级用户验证，稳定性经过大规模考验
- 完全开源，可自行定制补丁管理和分发逻辑
- 补丁包体积小（差分算法优秀）
- **维护活跃**：v1.9.15.2 已修复 Android 15 资源注入问题、兼容 Android 14 动态加载只读要求
- 社区生态好，文档和踩坑经验丰富

**劣势：**
- 需要重启才能生效
- 需要侵入 Application（继承 TinkerApplication），与 Hilt 的 `@HiltAndroidApp` 需要额外适配
- 补丁合成过程占用额外存储和 CPU
- 不支持修改 AndroidManifest（无法新增四大组件）
- 需要自建补丁分发后台（或使用 Bugly 免费平台）

### 2. Robust（美团）

- GitHub：[Meituan-Dianping/Robust](https://github.com/Meituan-Dianping/Robust)  ⭐ 4.3k
- 最新版本：0.4.99（**最后活跃约 2022 年**）
- 原理：编译期插桩（Instant Run 思路），每个方法前插入 changeQuickRedirect 判断
- 修复范围：**仅代码**
- 生效方式：**即时生效，无需重启**
- 开源协议：Apache 2.0
- 费用：**完全免费开源**

**优势：**
- 即时生效，无需重启
- 兼容性极高，不依赖 Android 底层实现
- 不侵入 Application，对现有架构影响最小
- 原理简单透明

**劣势：**
- **项目已基本停止维护**，GitHub Issues 大量未回复
- 仅支持代码修复，不支持资源和 SO 库
- 编译期插桩导致包体积增大
- 对运行时性能有轻微影响（每次方法调用多一次 null 判断）
- **对 AGP 8.x 存在已知兼容问题**（Issue #434），需要自行 fork 修复
- 对 Kotlin 2.x / Compose 函数的插桩行为未经验证

### 3. Bugly 热更新（腾讯）

- 文档：[Bugly Android 热更新](https://bugly.qq.com/docs/user-guide/instruction-manual-android-hotfix/)
- 原理：底层基于 Tinker，上层封装了补丁管理和分发平台
- 修复范围：同 Tinker（DEX / 资源 / SO）
- 生效方式：**重启生效**
- 费用：**免费**（含补丁管理后台）

**优势：**
- 基于 Tinker，修复能力完整
- **免费提供补丁管理后台**（含灰度下发、版本管理、回滚）
- 集成了崩溃上报，可联动定位问题
- 省去自建补丁分发后台的成本

**劣势：**
- 本质是 Tinker 的封装，继承了 Tinker 的所有限制（Application 侵入、重启生效）
- 平台更新节奏较慢，SDK 版本可能落后于 Tinker 官方
- 闭源的管理后台，依赖腾讯服务可用性
- 免费服务无 SLA 保障

### 4. QQ 空间方案（QZone）

- 原理：Multidex + 类加载顺序控制
- 修复范围：仅代码
- 生效方式：重启生效
- 费用：免费（但无官方开源实现）

**优势：** 原理简单，实现成本低

**劣势：** 无官方维护的开源实现，需完全自研；存在 CLASS_ISPREVERIFIED 问题；补丁包大；对 ART 有性能影响。**不推荐在新项目中使用。**

---

## 三、免费方案对比总结

| 维度 | Tinker | Robust | Bugly（Tinker 封装） |
|------|--------|--------|---------------------|
| 代码修复 | ✅ | ✅ | ✅ |
| 资源修复 | ✅ | ❌ | ✅ |
| SO 修复 | ✅ | ❌ | ✅ |
| 即时生效 | ❌（需重启） | ✅ | ❌（需重启） |
| 补丁大小 | 小（差分） | 较大（插桩） | 小（差分） |
| Application 侵入 | **是** | 否 | **是** |
| 开源程度 | 完全开源 | 完全开源 | SDK 部分开源 |
| 费用 | 免费 | 免费 | 免费 |
| 补丁管理后台 | 需自建 | 需自建 | **免费提供** |
| 维护活跃度 | **活跃**（2024 年仍有更新） | **停滞**（~2022） | 较慢 |
| AGP 8.x 兼容 | ✅（v1.9.15 系列） | ⚠️ 存在问题 | ⚠️ 需验证 |
| Android 15 兼容 | ✅（v1.9.15.1 修复） | ❌ 未适配 | ⚠️ 取决于 SDK 版本 |

---

## 四、推荐方案：Tinker + Bugly

### 推荐理由

综合评估后，推荐 **Tinker（核心引擎）+ Bugly（补丁管理平台）** 的组合方案：

1. **Tinker 是唯一仍在活跃维护的免费方案**：v1.9.15.2 已适配 Android 14/15，支持新 Dex 格式
2. **修复范围最全**：代码 + 资源 + SO 全覆盖，记账 App 的各类 Bug 都能修
3. **Bugly 提供免费的补丁管理后台**：省去自建分发系统的成本，支持灰度、回滚
4. **微信亿级验证**：稳定性有保障
5. **Robust 已停止维护**：对 AGP 8.x 存在兼容问题，不适合新项目

### 需要解决的关键问题：Hilt 兼容

本项目使用 `@HiltAndroidApp` 注解 Application，而 Tinker 要求 Application 继承 `TinkerApplication`。两者存在冲突，但有成熟的解决方案：

**方案：使用 Tinker Annotation + Hilt 的 attachBaseContext 初始化**

```kotlin
// ============================================================
// 第一步：创建 TinkerApplication 壳（不包含任何业务逻辑）
// ============================================================

// AccountRecordApp.kt — 仅作为 Tinker 的入口壳
class AccountRecordApp : TinkerApplication(
    ShareConstants.TINKER_ENABLE_ALL,           // 开启全部修复能力
    "com.mian.accountrecord.AccountRecordAppLike", // 真正的 Application 逻辑类
    "com.tencent.tinker.loader.TinkerLoader",
    false
)
```

```kotlin
// ============================================================
// 第二步：将原有 Application 逻辑迁移到 ApplicationLike
// ============================================================

// AccountRecordAppLike.kt — 承载所有业务初始化逻辑
@DefaultLifeCycle(
    application = "com.mian.accountrecord.AccountRecordApp",
    flags = ShareConstants.TINKER_ENABLE_ALL
)
class AccountRecordAppLike(
    application: Application,
    tinkerFlags: Int,
    tinkerLoadVerifyFlag: Boolean,
    applicationStartElapsedTime: Long,
    applicationStartMillisTime: Long,
    tinkerResultIntent: Intent
) : DefaultApplicationLike(
    application, tinkerFlags, tinkerLoadVerifyFlag,
    applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent
) {

    override fun onCreate() {
        super.onCreate()
        // 原有的初始化逻辑
        MMKV.initialize(application)
        NotificationHelper.createChannel(application)

        // WeChat SDK
        val wxApi = WXAPIFactory.createWXAPI(application, WECHAT_APP_ID, true)
        wxApi.registerApp(WECHAT_APP_ID)
    }
}
```

```kotlin
// ============================================================
// 第三步：Hilt 兼容 — 通过 ContentProvider 自动初始化
// ============================================================

// HiltInitializer.kt — 利用 ContentProvider 在 Application.onCreate 之前初始化 Hilt
// 注意：Hilt 2.x 已内置对非标准 Application 的支持，
// 可通过 @EarlyEntryPoint 或 @EntryPoint 在 ApplicationLike 中获取依赖。
// 具体方案需根据 Hilt 版本验证。
```

> **重要提示**：以上代码为架构思路示意。Hilt 2.54 + Tinker 的具体兼容方式需要在 demo 工程中实际验证，核心思路是将 Hilt 的组件注入与 Tinker 的 Application 壳分离。

---

## 五、接入步骤概要

### 5.1 添加依赖

```kotlin
// 根 build.gradle.kts
buildscript {
    dependencies {
        classpath("com.tencent.tinker:tinker-patch-gradle-plugin:1.9.15.2")
    }
}

// app/build.gradle.kts
plugins {
    // ... 现有插件
}
apply(plugin = "com.tencent.tinker.patch")

dependencies {
    // Tinker 核心库
    implementation("com.tencent.tinker:tinker-android-lib:1.9.15.2")
    compileOnly("com.tencent.tinker:tinker-android-anno:1.9.15.2")
    annotationProcessor("com.tencent.tinker:tinker-android-anno:1.9.15.2")

    // Bugly 热更新（可选，提供补丁管理后台）
    implementation("com.tencent.bugly:crashreport_upgrade:1.6.1")
    implementation("com.tencent.tinker:tinker-android-lib:1.9.15.2")
}
```

### 5.2 改造 Application

按照第四节的方案，将 `AccountRecordApp` 拆分为 Tinker 壳 + ApplicationLike。

### 5.3 配置 Tinker Gradle 插件

```kotlin
// app/build.gradle.kts 末尾
tinkerPatch {
    oldApk = "${buildDir}/bakApk/app-release-old.apk"  // 基准包路径
    ignoreWarning = false
    useSign = true

    buildConfig {
        applyMapping = null
        applyResourceMapping = null
        tinkerId = "base-${project.version}"
    }

    dex {
        dexMode = "jar"
        pattern = listOf("classes*.dex", "assets/secondary-dex-?.jar")
        loader = listOf("com.mian.accountrecord.AccountRecordApp")
    }

    lib {
        pattern = listOf("lib/*/*.so")
    }

    res {
        pattern = listOf("res/*", "assets/*", "resources.arsc", "AndroidManifest.xml")
        largeModSize = 100
    }
}
```

### 5.4 补丁生成与分发流程

```
1. 发布基准包 → 保存 bakApk（基准 APK + mapping + R 文件）
2. 修复 Bug → 修改代码
3. 执行 tinkerPatchRelease → 生成补丁包 (patch_signed_7zip.apk)
4. 上传补丁到 Bugly 后台 → 配置灰度策略 → 下发
5. 用户重启 App → 补丁自动加载生效
```

---

## 六、风险与注意事项

| 风险项 | 说明 | 应对措施 |
|--------|------|---------|
| Hilt 兼容性 | Tinker 的 Application 壳与 Hilt 注解冲突 | 接入前在 demo 工程验证；必要时使用 @EntryPoint 替代 |
| AGP 8.10 兼容 | Tinker Gradle 插件对最新 AGP 的适配 | 使用 v1.9.15.2；关注 GitHub Issues |
| Kotlin 2.1 编译产物 | K2 编译器产出的字节码差异 | 补丁生成后在真机上充分测试 |
| Compose 函数修复 | Compose 编译器生成的代码结构特殊 | 修复 Compose 相关 Bug 时需额外验证补丁有效性 |
| Google Play 合规 | Google Play 禁止非 Play 渠道动态更新代码 | 仅在国内渠道使用热修复；海外版使用 In-App Updates |
| 补丁回滚 | 补丁可能引入新问题 | Bugly 后台配置回滚策略；保留多版本基准包 |

---

## 七、总结

在免费方案中，**Tinker 是当前唯一仍在活跃维护、且经过大规模验证的选择**。搭配 Bugly 免费的补丁管理后台，可以以零成本获得完整的热修复能力。

主要的接入成本在于 Application 改造（适配 Hilt），建议：
1. 先创建一个最小化 demo 工程，验证 Tinker v1.9.15.2 + Hilt 2.54 + AGP 8.10 的兼容性
2. 验证通过后再在主项目中接入
3. 重点测试 Compose UI 相关代码的补丁修复效果
