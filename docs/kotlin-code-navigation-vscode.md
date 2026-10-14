# VSCode / Cursor 里 Kotlin Multiplatform 代码跳转说明

> 结论先说：**当前（2026-09）VSCode 系编辑器没有任何一个 Kotlin 语言服务器能对这个仓库做出可靠的语义跳转**
> —— 这是上游能力缺口，不是配置问题。但"读 Jetpack Compose 与安卓 framework 源码"不需要语言服务器，
> 所以已经落地两件事：**① 把项目真实版本的依赖源码 + framework 源码接成可浏览工作区（第 3 节）**、
> **② 把误报红关掉（第 4 节）**，另外把"以后语义跳转怎么开"写成清单（第 6 节）。
>
> 要用起来：项目根目录跑 `.vscode/link-kotlin-sources.sh`，然后打开
> `.vscode/destiny-sources.code-workspace`。

## 1. 你的环境实际情况

先纠正一个前提：你机器上**没有装真正的 VS Code**。

| | 实际情况 |
|---|---|
| `/usr/local/bin/code` | 符号链接 → `/Applications/Cursor.app/.../bin/code`，即 **Cursor 3.21.18** |
| 已装 Kotlin 相关 | `fwcd.kotlin@0.2.36` |
| 已装 Java 相关 | `redhat.java@1.56.0`、`vscjava.vscode-gradle@3.18.0`、java-pack 全家桶 |
| 真正的 VS Code | 未安装（`~/.vscode/extensions` 是历史残留，里面是 `mathiasfrohlich.kotlin`） |
| 其它 VSCode 系 | Trae CN、Qoder CN（`~/.trae/extensions` 里有 Java 全家桶 + Dart/Flutter） |

## 2. 为什么会爆红、为什么跳不动

这个仓库是**纯 KMP + Compose Multiplatform**：Kotlin `2.4.10`、Gradle `9.1.0`、AGP `9.0.1`、
Compose Multiplatform `1.12.0`、KSP `2.3.11`、Room 3.0.1，target 覆盖
`iosArm64 / iosSimulatorArm64 / jvm / wasmJs / android`（`shared/build.gradle.kts`）。

三个层面的原因叠在一起：

### 2.1 fwcd.kotlin 明确不支持非 JVM target

`fwcd.kotlin` 背后的 `kotlin-language-server` **上游最后一次发布是 1.3.13（2025-01）**，
`vscode-kotlin` 仓库最后提交 2024-12。它自己 issue 列表里这些还开着：

- `#11` Support non-JVM backends (Kotlin/Native, Kotlin/JS, Kotlin/Wasm)
- `#376` [Multiplatform + Neovim + VSCode] Performance, unresolved references, errors
- `#447` Problems when using for a compose multiplatform project

它拿不到 KMP 的 source set 模型，于是 `commonMain` 里引到的 `iosMain`/`wasmJs` 声明、
跨 target 的 `expect/actual` 全部解析不到 → 满屏 `Unresolved reference`，`go to definition` 自然也没有候选。

### 2.2 官方 JetBrains Kotlin LSP 还没做 KMP 导入

官方实现是 [`Kotlin/kotlin-lsp`](https://github.com/Kotlin/kotlin-lsp)（VSCode 扩展
`JetBrains.kotlin-server`，最新 `v263.4702.0` / 2026-09-13，Alpha）。README 原文：

> Build system support for JVM projects: Gradle, Maven, experimental Android Gradle Plugin support
> **Support for Kotlin Multiplatform (KMP) projects is coming in the future releases.**

跟踪中的 issue 是 [`#3` Support for Kotlin Multiplatform](https://github.com/Kotlin/kotlin-lsp/issues/3)（仍 open，
内部工单 `LSP-910`）。Kotlin 组织成员在 issue 里确认过："only JVM-only Kotlin Gradle projects are supported
out-of-the-box"。所以它现在导入这个工程同样不会完整。

### 2.3 生成代码本来就不可解析

`shared/src/commonMain/.../BookStore.kt:3` 有：

```kotlin
import destiny.shared.generated.resources.Res
```

`Res` 是 Compose Multiplatform resources 插件在构建时生成的访问器类；同类还有 KSP/Room 的实现类。
**任何**不做完整 Gradle 导入的语言服务器都看不到它们，除非先跑过生成任务、且它认识 generated source root。

### 2.4 附带噪音：redhat.java

`redhat.java` + `vscode-gradle` 会去尝试导入这个 Gradle 工程。AGP 9 + Gradle 9 + KMP 这套它处理不了，
除了报额外的问题还会白占几 GB 内存（本仓库基本没有 Java 源码，导入它零收益）。

## 3. 已落地方案 A：把依赖与框架源码变成可浏览的工作区根

语义跳转指望不上，但**读源码不需要语言服务器，只需要文件在磁盘上**。本机其实早就有全部原料，
只是没人把它们接起来：

- Gradle 缓存里散落着 394 个 `-sources.jar`，但**版本是错的**（别的工程留下的 1.10.0，本工程实际用 1.12.0）
- Android SDK 的 framework 源码（含 compileSdk 36）已装在 `$SDK/sources/android-36`

所以做了 `.vscode/link-kotlin-sources.sh`：**先问 Gradle 要本项目真实解析到的坐标**，再按坐标取源码。

```bash
# 在项目根目录
.vscode/link-kotlin-sources.sh              # 完整流程（会跑一次 Gradle 导坐标）
.vscode/link-kotlin-sources.sh --skip-gradle  # 复用已有 coords.txt，几秒完成
.vscode/link-kotlin-sources.sh --force        # 重新解压
```

它做四件事：

1. `--init-script` 调 `dumpDependencyCoords`，把 `:androidApp` + `:shared` 解析到的坐标写进
   `~/.cache/destiny-sources/coords.txt`（**不修改任何构建文件**）
2. 按组白名单收敛到 300 个（compose / androidx / skiko / kotlinx / ktor / coil / kotlin-stdlib），
   Gradle 缓存优先、缺失的按标准 Maven 布局从 Google Maven 与 Maven Central 并行拉 `-sources.jar`
3. 解压成 `~/.cache/destiny-sources/libs/<group>__<artifact>__<version>/`，并清理版本升级后残留的旧目录
4. 生成 `.vscode/destiny-sources.code-workspace`（多根工作区：项目 + 依赖源码 + framework 源码，
   源码根设为只读、不参与文件监听）

**打开 `.vscode/destiny-sources.code-workspace` 即可**，实测结果：

| 项 | 值 |
|---|---|
| 依赖源码包 | 297 / 300（缺的 3 个是 2 个 BOM + 1 个平台打包件，无代码） |
| 版本一致性 | `androidx.compose.{ui,runtime,foundation}:1.12.0`、`material3:1.5.0-alpha22`，与 `debugRuntimeClasspath` 完全一致 |
| 可读文件数 | 13302 个 `.kt` / `.java`，约 167 MB |
| 已验证可搜到 | `AndroidComposeView.android.kt`、`class Recomposer`、`class CompositionImpl`、`interface Modifier`、`MaterialTheme.kt`、`android/app/Activity.java` |
| 重跑耗时 | 6.6 s（幂等，零重复解压、零重复下载） |

用起来就是：`Cmd+P` 输 `AndroidComposeView` / `Activity.java` 直接开；
`Cmd+Shift+F` 搜 `class Recomposer` 看 Compose 怎么驱动重组；framework 根里搜 `Looper` 看安卓框架本身。
换 Compose 版本后重跑一次脚本即可（旧版本目录会被自动清掉）。

## 4. 已落地方案 B：把误报红关掉

`.vscode/settings.json`（`.vscode/` 已在 `.gitignore`，纯本地，不污染仓库）：

- `kotlin.diagnostics.enabled: false` —— fwcd 的 unresolved reference 全是误报，关诊断，**保留补全和 hover**。
- `java.import.gradle.enabled: false` + `java.import.exclusions` —— 不让 JDT 去啃这个 KMP Gradle 工程。
- `search.exclude` / `files.watcherExclude` —— 排除 `build/`、`.gradle/`、`.kotlin/`、`kotlin-js-store/`，
  让全局搜索和 `Cmd+P` 变快。

生效：`Cmd+Shift+P` → `Developer: Reload Window`。
万一以后语义解析能用了，把 `kotlin.diagnostics.enabled` 改回 `true` 就能看到真问题（一行开关，可逆）。

## 5. 现在各种跳转能做到什么程度

| 需求 | 能不能 | 怎么做 |
|---|---|---|
| 读 Compose / androidx / framework 源码 | ✅ | 打开 `destiny-sources.code-workspace`，`Cmd+P` + `Cmd+Shift+F`（方案 A） |
| 项目内按文件名跳 | ✅ | `Cmd+P`，支持 `BookStore.kt:120` 落到行 |
| 单文件内符号列表 | ✅ 应当可用 | `Cmd+Shift+O`（只需单文件解析，不吃项目模型；不放心就打开任一 `.kt` 试一下） |
| 项目内跨文件 `Cmd+B` | ❌ | 语言服务器没有 KMP 项目模型 |
| find usages / 重命名 | ❌ | 同上 → 用 Android Studio / IntelliJ |
| 自己代码里找引用 | ⚠️ 兜底 | `Cmd+Shift+F` 搜符号名，配合方案 A 也能搜进依赖源码 |

要真正的语义跳转（跨模块、find usages、重构），**目前仍然只能用 Android Studio / IntelliJ**：
只有它的插件完整实现了 KMP + Compose + KSP 项目模型。建议分工是
**AS/IntelliJ 负责导航与重构，Cursor/Qoder 负责编辑 + AI 辅助 + 读依赖源码**。

> 曾经考虑过 ctags 兜底：系统自带 `/usr/bin/ctags` 是 Xcode 的 BSD ctags，不支持 Kotlin；
> 且 universal-ctags 对 Kotlin 是正则式实现，对 `expect/actual` 与生成代码同样无解。
> 方案 A 直接给真源码，比 tags 更准，所以不必再走那条路。

## 6. 等 KMP 支持落地时，怎么开（照做即可）

盯 [`Kotlin/kotlin-lsp` issue #3](https://github.com/Kotlin/kotlin-lsp/issues/3)，
README 的 Supported Features 一行从 "coming in the future releases" 改掉就是信号。届时：

1. **装扩展**（Marketplace ID 已换过，注意别装错）：
   - 新：`JetBrains.kotlin-server`
   - 旧：`jetbrains.kotlin` —— 官方明确说**两个同时存在会导致新扩展无法激活**，
     安装时会弹窗提示卸载旧的，接受并重载窗口。
   - Cursor 里：`code --install-extension JetBrains.kotlin-server`
2. **先把第 4 节的关诊断设置去掉**：删掉 `kotlin.diagnostics.enabled: false`，否则它报的问题你看不到。
3. **避免冲突**：卸载 `fwcd.kotlin`，两个 Kotlin 语言服务器同时在会抢 `definition` provider，
   表现为"随机跳不动"。`redhat.java` 如果不做 Java 调试也可以一并卸掉。
4. **准备 JDK**：kotlin-lsp 基于 IntelliJ 平台，是重进程；`kotlin.java.home` 指一个 JDK 17+
   （你当前 `java` 是 Zulu 23，可用，但 `JAVA_HOME` 是空的，最好显式配）。
5. **先让生成代码就位**，再开 IDE，否则 `Res` / Room 仍然红：

   ```bash
   ./gradlew :shared:compileKotlinJvm   # 生成 Compose resources 访问器与 KSP 输出
   ```

   （或直接把日常使用的 target 都编译一遍；产物在 `shared/build/generated/` 下。）
6. **验证是否真的通了**，按这个顺序测：
   - `commonMain` 里某个 `expect fun` → `Cmd+B` 能否跳到 `iosMain`/`jvmMain` 的 `actual`
   - `BookStore.kt:3` 的 `Res` import 是否还红
   - `settings.gradle.kts` 里 `include(":liuyao-core")` 等**跨模块**符号能否跳（这是 issue `#68`
     "LSP does not recognize subprojects" 的靶子）
   - 三项全过才算可用；只要 `Res` 或跨模块还红，就说明 KMP/子模块导入仍不完整，回到第 5 节的表格。

## 7. 一句话总结

- **爆红**：已治（第 4 节，`kotlin.diagnostics.enabled: false` + 不让 JDT 导入 Gradle）。
- **读 Compose / androidx / 安卓 framework 源码**：已通（第 3 节，版本和项目完全一致）。
- **自己代码里的语义跳转（`Cmd+B`、find usages、重构）**：VSCode 系目前治不了，
  责任在 fwcd 不维护 + 官方 kotlin-lsp 的 KMP 导入未实现，不是你的配置问题 ——
  这块请走 Android Studio/IntelliJ，等 issue #3 关闭后按第 6 节切换。
