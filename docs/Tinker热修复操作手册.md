# Tinker 热修复操作手册

> 适用于 accountRecord 项目  
> Tinker 版本:`1.9.14.27`  
> 接入模式:`DefaultApplicationLike` + 手动 `TinkerInstaller.install()`(兼容 Hilt)  
> patch 插件:`com.tencent.tinker.patch`,默认 **关闭**,按需开启

---

## 1. 整体流程一览

```
┌──────────────┐   ① 发版前      ┌──────────────────────┐
│  出基线包     │ ─────────────►│ 归档 old.apk /       │
│ (release apk) │                │ mapping.txt / R.txt  │
└──────────────┘                 └──────────────────────┘
                                            │
                                            │  ② 线上发现 bug,改代码
                                            ▼
┌──────────────┐                 ┌──────────────────────┐
│   出补丁包    │◄────────────── │ tinkerPatchRelease   │
│  (patch.apk) │                 │ 任务读取 old/* 与新包 │
└──────────────┘                 └──────────────────────┘
        │
        │  ③ 下发到 App(自建后台 / Bugly / 静态资源)
        ▼
┌──────────────────────┐
│ TinkerManager        │
│   .applyPatch(path)  │ ─► 校验 ─► 进程重启 ─► 生效
└──────────────────────┘
```

**核心约束:基线包和补丁包的 `TINKER_ID` 必须完全一致**;否则 Tinker 拒绝加载。

---

## 2. 准备工作(一次性)

### 2.1 检查全局开关

`gradle.properties`:

```properties
tinkerEnabled=false
```

日常构建保持 `false`,只在打补丁时通过 CLI `-PtinkerEnabled=true` 临时开启。

### 2.2 准备基线归档目录

```
app/
└── tinker/
    └── old-apk/
        ├── old.apk         # 上一次发版的 release apk
        ├── mapping.txt     # 上一次 R8/ProGuard 输出的 mapping
        └── R.txt           # 上一次的资源 ID 表
```

> 这三个文件**必须严格对应同一次基线构建**,任一不匹配都会导致补丁加载失败或类映射错乱。

---

## 3. 出基线包

### 3.1 命令

```bash
gradlew clean assembleRelease ^
        -PtinkerEnabled=true ^
        -PTINKER_ID=v1.0.0-base-20260508
```

> Windows 用 `^` 续行,macOS / Linux 用 `\`。  
> `TINKER_ID` 自定义但必须可追溯,推荐格式 `versionName-base-yyyyMMdd[-HHmm]`。

### 3.2 归档产物

构建完成后,把以下文件拷到 `app/tinker/old-apk/`:

| 来源路径 | 重命名为 |
|---|---|
| `app/build/outputs/apk/release/app-release.apk` | `old.apk` |
| `app/build/outputs/mapping/release/mapping.txt` | `mapping.txt` |
| `app/build/intermediates/runtime_symbol_list/release/R.txt` 或 `app/build/intermediates/symbol_list/release/R.txt` | `R.txt` |

> AGP 不同版本下 `R.txt` 路径有差异,搜索 `app/build` 下的 `R.txt`,挑 release 变体即可。

### 3.3 同步发版

把 `TINKER_ID` 写进版本归档(Git tag / 内部发版表),后续打补丁必须复用同一个值。

**强烈建议**:把 `app/tinker/old-apk/` 下的三个文件压成 zip,以 `TINKER_ID` 命名,上传到内网 OSS / Artifactory,避免本地丢失就再也无法补这个版本。

---

## 4. 出补丁包

### 4.1 还原基线归档

把上一次发版归档的 `old.apk / mapping.txt / R.txt` 放回 `app/tinker/old-apk/`。

### 4.2 修复代码

修 bug,但要避开 Tinker 的 dex 不可热替换列表 ——

- `AccountRecordApp`(应用主 Application)
- `tinker.SampleApplicationLike`(Tinker 装载入口)
- `com.tencent.tinker.loader.*`

它们已在 `app/build.gradle.kts` 的 `dex.loader` 中声明,**改这些类不会被打进补丁,补丁不生效**。需要修这些类必须发新版本。

> 经验:Application、ContentProvider、所有静态初始化在补丁加载前就跑过的类,都建议加入 loader 列表。

### 4.3 命令

```bash
gradlew tinkerPatchRelease ^
        -PtinkerEnabled=true ^
        -PTINKER_ID=v1.0.0-base-20260508
```

`TINKER_ID` **必须** 与基线包完全一致。

### 4.4 产物位置

```
app/build/outputs/apk/tinkerPatch/release/
├── patch_signed_7zip.apk     # ★ 这是要下发的补丁文件
├── patch_signed.apk
└── patch_unsigned.apk
```

下发使用 **`patch_signed_7zip.apk`**(经过 7zip 二次压缩,体积最小)。

### 4.5 验证补丁

打开 `app/build/outputs/apk/tinkerPatch/release/log.txt`,确认:

- `tinker patch result: success`
- `dex` / `res` / `lib` 段有列出实际 diff 的文件
- 没有 `unable to find xxx in old apk` 类的告警

---

## 5. 下发与生效

### 5.1 客户端调用

```kotlin
import com.mian.accountrecord.tinker.TinkerManager

// 把下载到本地的 patch_signed_7zip.apk 路径传进来
TinkerManager.applyPatch(context, downloadedPatchPath)
```

`applyPatch` 内部:

1. 拷贝补丁到 Tinker 工作目录
2. 启动 `:patch` 进程做校验和合成
3. 校验通过后通知 `DefaultTinkerResultService`,默认行为是**杀掉主进程,下次启动加载补丁**
4. 用户下次冷启动 App → 补丁生效

### 5.2 清除补丁

线上回滚或检测到补丁导致 crash 时:

```kotlin
TinkerManager.cleanPatch(context)
```

会移除已安装补丁,下次启动加载基线代码。

### 5.3 下发渠道选项

- **自建**:服务端比对 `BuildConfig.TINKER_ID` + 当前已应用补丁版本号,返回补丁下载 URL
- **Bugly 热更新** :SDK 自带分发,但官方已停止维护,新项目不建议
- **简单方式**:静态资源放 OSS,App 内拉取版本清单 JSON,匹配则下载

---

## 6. CI 集成示例(参考)

```yaml
# 出基线
- name: build-baseline
  run: |
    gradlew assembleRelease -PtinkerEnabled=true -PTINKER_ID=$RELEASE_TAG
  artifacts:
    - app/build/outputs/apk/release/app-release.apk
    - app/build/outputs/mapping/release/mapping.txt
    - app/build/intermediates/runtime_symbol_list/release/R.txt

# 出补丁(单独 job,从制品仓库拉对应基线)
- name: build-patch
  needs: download-baseline
  run: |
    cp baseline/app-release.apk app/tinker/old-apk/old.apk
    cp baseline/mapping.txt    app/tinker/old-apk/mapping.txt
    cp baseline/R.txt          app/tinker/old-apk/R.txt
    gradlew tinkerPatchRelease -PtinkerEnabled=true -PTINKER_ID=$BASELINE_TAG
  artifacts:
    - app/build/outputs/apk/tinkerPatch/release/patch_signed_7zip.apk
```

---

## 7. 不能用补丁修复的场景

把这些情况列入清单,出问题直接走发版,不要硬上补丁:

| 场景 | 原因 |
|---|---|
| Application、ContentProvider 等启动期类 | 在补丁加载之前已被系统类加载器载入 |
| 增减四大组件 | AndroidManifest 改动需要重新安装 |
| 修改 `versionCode` / `versionName` 触发的逻辑 | 补丁不能改 manifest 版本号 |
| 修改 `compileSdk` / `targetSdk` 行为 | 补丁不重打 manifest |
| 修复需要新增 native so | Tinker 支持 so 增量,但 minSdk 较老的设备需要谨慎测试 |
| 涉及加密资源 / 动态权限 manifest 声明 | 同上 |

---

## 8. 常见报错速查

| 报错 | 原因 | 处理 |
|---|---|---|
| `tinker id is not equal` | 补丁与基线 `TINKER_ID` 不一致 | 确认 CLI 传的 `-PTINKER_ID` 与基线一致 |
| `unable to find ... in old apk` | mapping/old.apk 与基线不同步 | 重新归档基线三件套 |
| `patch is already installed` | 同一补丁重复 apply | 先 `cleanPatch` 再 apply,或直接重启验证 |
| `Hilt component already created` | Tinker 安装时机晚于 Hilt | 确认 `attachBaseContext` 内 `applicationLike.onBaseContextAttached(base)` 在 Hilt 任何注入前执行 |
| AGP 8 + tinker-patch-gradle-plugin 报变体 / transform API 不存在 | 官方插件未适配 AGP 8 | 临时降 AGP 到 7.4.x 单独打补丁;或换社区 fork;或等官方更新 |

---

## 9. 安全与签名

- 补丁包必须使用与基线**同一份签名**,否则被 Tinker 拒绝
- 签名信息建议放 `local.properties`(已 gitignore)或 CI 密钥管理,不要进 Git
- 线上下发务必走 HTTPS + 文件 MD5 校验,避免补丁被中间人替换

---

## 10. 关联文件

| 路径 | 作用 |
|---|---|
| `app/build.gradle.kts` | `tinkerEnabled` 开关 + `tinkerPatch` 配置块 |
| `app/proguard-rules.pro` | Tinker 类保留规则 |
| `app/src/main/java/com/mian/accountrecord/AccountRecordApp.kt` | 安装入口 |
| `app/src/main/java/com/mian/accountrecord/tinker/SampleApplicationLike.kt` | Tinker 生命周期 |
| `app/src/main/java/com/mian/accountrecord/tinker/TinkerManager.kt` | 对外 API |
| `app/src/main/AndroidManifest.xml` | 注册 `DefaultTinkerResultService` |
| `gradle/libs.versions.toml` | Tinker / multidex 版本 |
| `gradle.properties` | `tinkerEnabled` 默认值 |
