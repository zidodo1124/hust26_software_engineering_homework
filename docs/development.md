# 开发、构建与首轮安装

本文对应仓库根目录的 Android 工程。功能范围及未完成项见[实现计划](implementation-plan.md)和[项目状态](../STATUS.md)；分工见[小组任务板](../team/README.md)。

## 1. 准备环境

| 工具 | 本工程版本或要求 |
| --- | --- |
| JDK | 17；不要使用电脑原有 Java 8 编译本工程 |
| Gradle | 8.11.1，由仓库 Wrapper 下载并校验，不必另装系统 Gradle |
| Android Gradle Plugin | 8.9.2 |
| Android SDK | Platform 35、Build Tools 35.0.0、Platform Tools |
| 编译／目标 API | 35／35 |
| 最低 API | 26，即 Android 8.0；这不是已验证全部 Android 8 设备的承诺 |

上述组合参考 [AGP 8.9 官方兼容说明](https://developer.android.com/build/releases/agp-8-9-0-release-notes)。语言与库的版本见[技术清单](technology-stack.md)，实际构建配置以根目录和 `app/` 的 Gradle 文件为准。

可用 Android Studio 打开仓库根目录，通过 SDK Manager 安装所需组件，并将 Gradle JDK 设为 JDK 17。也可安装官方 [Android 命令行工具](https://developer.android.com/studio#command-tools)和 [Temurin JDK](https://adoptium.net/temurin/releases/?version=17)后使用命令行。

本次开发机的工具安装在被忽略的 `.local-tools/`，没有修改系统 Java。该目录不进入 Git，队友克隆后需要准备自己的环境。`local.properties` 只保存本机 SDK 路径，不提交。

如果使用系统已安装的工具，在 PowerShell 设置当前会话环境；以下路径应改成自己的实际位置：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ANDROID_HOME = 'C:\path\to\Android\Sdk'
```

也可以在仓库根目录的本机 `local.properties` 写入 `sdk.dir=C:/path/to/Android/Sdk`。首次构建需要网络下载依赖；应用的本机核心功能不依赖联网。

## 2. 构建与检查

首轮源码 [1655a70](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/1655a70) 已在本机完成下述三项检查并生成 Debug APK。30 项本地自动测试全部通过；Lint 为 0 错误、10 条依赖版本更新提示，编译另有 `LocalClipboardManager` 弃用提示。同源码在线 CI 也已通过，详见下方下载入口；手机安装仍待验证。

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

辅助脚本优先采用本项目 `.local-tools/jdk-*` 下的 JDK；其他电脑使用当前 `JAVA_HOME`。它只影响本次进程环境，不修改系统环境变量。

准备好 JDK 和 SDK 后，也可直接运行 Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

macOS／Linux 使用 `./gradlew` 并配置各自的 JDK 17 与 SDK 路径。本轮在 Windows 本机和 GitHub Actions 的 Ubuntu 24.04 环境完成检查，macOS 尚未验证。

| 产物 | 相对仓库的位置 |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| JVM／Robolectric 测试报告 | `app/build/reports/tests/testDebugUnitTest/index.html` |
| JUnit XML | `app/build/test-results/testDebugUnitTest/` |
| Android Lint 报告 | `app/build/reports/lint-results-debug.html` |
| Room schema | `app/schemas/`；结构变更需随代码提交并检查迁移 |

`build/` 产物和工具缓存不提交 Git；与验证结论相关的精简证据保存到 `docs/test-evidence/`，实际结果见[测试记录](test-records.md)。

### GitHub 自动构建与下载

[Android 工作流](../.github/workflows/android.yml)在推送 `main` 或提交 PR 时运行同样的构建、自动测试与 Lint。三项检查成功后上传 `ddl-saver-debug-提交号` 安装包；测试和 Lint 报告另存为构建产物，保留 14 天。

首轮源码 `1655a70` 的[工作流运行](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40490279525)已成功，JUnit 报告核对为 30 项测试通过。可下载 [Debug APK](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40490279525/artifacts/12728437151)及[测试／Lint 报告](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40490279525/artifacts/12728172188)，需要登录有仓库访问权限的 GitHub 账号。CI 与本机是分别构建的产物，本地记录中的 APK 哈希不代替 CI 产物标识。

队友可在仓库的 [Actions](https://github.com/zidodo1124/hust26_software_engineering_homework/actions) 中选择对应提交的 Android 运行，在 Artifacts 下载安装包或报告。只有实际工作流结果显示成功时，才能说该次线上检查通过；工作流文件存在本身不代表已经运行。

工作流使用 [checkout](https://github.com/actions/checkout)、[setup-java](https://github.com/actions/setup-java)、[setup-android](https://github.com/android-actions/setup-android) 和 [upload-artifact](https://github.com/actions/upload-artifact)，具体版本固定在 YAML 中。权限仅为读取仓库内容，不发布 Release。

本地 Robolectric 测试需要下载对应 Android 运行库。工程通过[官方支持的系统属性](https://robolectric.org/configuring/#system-properties)显式使用 Maven Central 的 `repo.maven.apache.org` 地址；首次下载失败属于测试环境阻断，不能填写为功能通过。实际下载错误及重试结果在执行记录中保留。

## 3. 安装与最小检查流程

Debug APK 用于本组开发和课程演示准备。可将实际构建产物传入手机安装，或在手机开启 USB 调试并确认授权后执行：

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

运行前确认 `adb devices` 中是自己的目标手机。当前用户选择暂不连接荣耀 X50，本轮不预先登记安装或启动通过。

建议首次实机检查：创建一条事项并填写速通时间；重开后查看详情；修改截止时间；依次完成、恢复、归档和恢复；取消一次删除，再确认删除另一条测试事项。检查今日／危险数量和卡片状态是否随操作更新，并按测试模板记录版本、设备及实际结果。

## 4. 代码入口

| 位置 | 职责 |
| --- | --- |
| `MainActivity.kt` | 应用启动、Compose 内容和 ViewModel 创建 |
| `ui/` | 首页、表单、详情、设置、页面状态及错误反馈 |
| `domain/` | 事项和设置模型、输入约束、北京时间规则、仓库接口 |
| `data/` | Room 实体、DAO、数据库、事务与仓库实现 |
| `app/src/test/` | 规则、存储与本地界面测试；不能替代荣耀 X50 验证 |

完整包路径为 `app/src/main/java/cn/edu/hust/ddlsaver/`。后续平台通知和图片能力按设计增量接入，不把 Android 系统调用放入纯时间规则中。
