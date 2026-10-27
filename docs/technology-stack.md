# HUSTer's DDL Saver 技术清单

- 小组：花园鳗。
- 更新日期：2026-10-27。
- 依据：用户允许根据项目需要选择语言和框架，并要求集中说明实际采用的技术。
- 当前版本：第二轮 `0.2.0`，`versionCode = 2`，Room schema v2。来源标签、自定义字段、日历、批量管理、过期整理及图片附件已接入；本轮构建、测试及 CI 结论见[测试报告](test-report.md)。用户暂不连接实机。

本文用于回答“项目使用什么技术、各自负责什么、为什么选择、实际使用哪个版本”。后续新增、替换或移除依赖时同步更新，具体模块和接口见[设计文档](design.md)。

## 1. 选择依据

第一版面向 Android，采用离线单机模式，需要访问系统通知、定时任务、图片选择和本地存储。因此采用 Android 原生方案，直接使用平台能力。小组熟悉 Python、C++、Java，用户未限定实现语言；本轮选择 Kotlin 配合 Compose 编写应用，不将成员熟悉的语言全部列为项目依赖。

Kotlin 支持与 Java 互操作，Compose 提供基于 Kotlin 的原生界面开发方式。本轮延续既有方案，不新增第三方依赖或升级依赖版本；日历和分类页面使用已有 Compose 组件，图片使用 Android 平台接口。构建与自动检查在开发机进行，荣耀 X50 的实机结果后续单独记录。[Kotlin 官方说明](https://kotlinlang.org/docs/android-overview.html)、[Compose 官方介绍](https://developer.android.com/compose)。

## 2. 应用与测试技术

| 类别 | 本轮选择 | 在项目中的用途 | 当前采用状态 | 版本记录 |
| --- | --- | --- | --- | --- |
| 实现语言 | Kotlin | 业务规则、界面状态和数据访问 | 已集成 | Kotlin 与 Compose 编译插件均为 2.1.20 |
| 界面 | Jetpack Compose、Material 3 | 首页卡片、表单、详情、分类管理、月份日历、组合筛选、批量选择和设置 | 已集成第二轮页面；角色后续接入 | Compose BOM 2025.04.01；库版本由 BOM 管理 |
| Activity 与界面状态 | Activity Compose、AndroidX ViewModel / Lifecycle | Activity 入口、页面状态、生命周期感知订阅及操作协调 | 已集成 | Activity 1.10.1；Lifecycle 2.9.0 |
| 异步与数据订阅 | Kotlin Coroutines / Flow | 数据库观察、后台数据操作和页面更新 | 已集成 | 1.10.2 |
| 结构化存储 | Room / SQLite | 保存事项、来源／标签、关联表、自定义字段、附件元数据及两项设置；批量操作与关联读取使用事务 | 已集成 schema v2 和显式 `MIGRATION_1_2`；提醒表后续扩展 | Room 2.7.1；schema 版本 2 |
| 代码生成 | KSP | 生成 Room 数据访问实现并导出 schema | 已配置 | 2.1.20-1.0.32 |
| 文件与图片导入 | Activity Result、Photo Picker、ClipboardManager、ContentResolver | 相册多选、主动粘贴可读图片 URI，流式保存私有副本 | 已集成 `PickMultipleVisualMedia` / `ImageOnly`；兼容回退由 Activity 契约提供 | 使用既有 Activity 1.10.1 及 Android 平台 API；无新增依赖 |
| 图片校验与展示 | BitmapFactory、android.media.ExifInterface、Matrix | 图片边界与真实解码校验、采样缩略图／预览、EXIF 方向处理 | 已集成 `platform/AttachmentStore.kt`；文件操作在 IO 调度器执行并由互斥锁协调 | Android 平台 API；未引入 Coil、Glide 或 AndroidX ExifInterface |
| 提醒调度 | AlarmManager | 安排截止相关定时任务，处理取消和重建 | 后续计划，尚未集成 | Android 平台 API；运行条件随设备记录 |
| 通知呈现 | Android 通知接口 | 每日汇总和单项提醒，以及通知点击 | 后续计划，尚未集成 | 具体兼容库及权限在实现时登记 |
| 业务规则测试 | JUnit、Coroutines Test | 时间边界、状态规则、组合查询、字段与附件元数据校验 | 已补充第二轮测试；执行结果见测试报告 | JUnit 4.13.2；Coroutines Test 1.10.2 |
| 本地 Android 行为测试 | Robolectric、AndroidX Test Core、Room Testing | 在开发机 JVM 中检查 Room 事务、真实 v1 文件迁移、附件平台行为及重开持久化 | 已补充第二轮测试；不能替代实机验证 | Robolectric 4.14.1；Test Core 1.6.1；Room Testing 2.7.1 |
| 本地界面流程测试 | Compose UI Test JUnit4、测试宿主 Manifest | 在 Robolectric 中操作基础与第二轮界面，检查草稿、筛选、批量及重建流程 | 已补充第二轮测试；执行结果见测试报告 | 由 Compose BOM 2025.04.01 管理 |
| 实机与设备测试 | Android 设备测试、人工检查 | 后续验证荣耀 X50 安装、交互、权限、附件和通知表现 | 本轮暂不连接实机，尚未验证 | 实际系统与所用测试工具后续记录 |

版本依据为根目录 [build.gradle.kts](../build.gradle.kts)、[app/build.gradle.kts](../app/build.gradle.kts)和 [Gradle Wrapper 配置](../gradle/wrapper/gradle-wrapper.properties)。以上“已集成”表示依赖或代码已加入工程，不单独代表构建、测试或实机检查通过。文本复制和图片 URI 粘贴已接入；能否从某个外部应用复制图片，取决于其剪贴板内容及访问授权，普通图片网址不直接导入。

Room 是 SQLite 的访问封装，适合本项目的本地结构化数据；数据及附件生命周期的具体安排属于项目设计，不由库自动解决。[Room 官方文档](https://developer.android.com/training/data-storage/room)。

本轮通过 `addMigrations(MIGRATION_1_2)` 执行显式升级，保留历史 schema 文件并测试旧库数据，不启用破坏性重建。[Room 迁移说明](https://developer.android.com/training/data-storage/room/migrating-db-versions)。图片选择只访问用户选定的内容，选择器不可用时由 Activity 契约回退；剪贴板中的图片引用通过 ContentResolver 读取。[Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)、[复制粘贴](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)。

解码前读取图片尺寸，再用采样控制预览内存；当前为 320 像素缩略目标及 1600 像素预览目标。[Android 大图加载说明](https://developer.android.com/topic/performance/graphics/load-bitmap)。20 MiB／100 MP 单图保护、24 小时孤儿副本保留期是本项目工程参数，不是新增第三方依赖、平台统一限制或用户已确认的业务上限，详见[图片设计](design.md#6-d-05-图片导入与附件生命周期)。

本轮保留平台 `android.media.ExifInterface`；Lint 提出迁移至 AndroidX ExifInterface 的维护建议，后续若采纳应登记新依赖并重新验证。当前不宣称兼容全部图片格式，各来源照片的旋转、镜像与实际显示效果仍需在荣耀 X50 检查。

定时任务与通知展示是两个环节，相关权限也需分别检查。实际是否按预期提醒，应在指定设备和权限条件下验证，不能仅凭选用了某个 API 就作保证。[AlarmManager 文档](https://developer.android.com/develop/background-work/services/alarms)、[通知权限说明](https://developer.android.com/develop/ui/compose/notifications/notification-permission)。

本地测试与设备测试按运行位置区分，单元测试与集成测试按检查范围区分。Robolectric 用于开发机上的 Android 行为检查；即使 Compose 本地流程通过，也不直接证明荣耀 X50 的安装、渲染或系统通知符合预期。[Android 测试基础](https://developer.android.com/training/testing/fundamentals)。

## 3. 开发与协作工具

| 工具 | 用途 | 当前状态 |
| --- | --- | --- |
| Android Studio | 可用于打开工程、编辑、设备连接和调试 | 可选工具；本轮实际使用命令行，未据此宣称 IDE 验证通过 |
| Gradle Wrapper / Android Gradle Plugin | 编译、依赖管理和打包 | Gradle 8.11.1；AGP 8.9.2；Wrapper 配置包含分发包 SHA-256 |
| JDK | Gradle 与 Kotlin/JVM 编译运行环境 | 本机使用 Eclipse Temurin 17.0.20.1+1；工程要求 JDK 17 |
| Android SDK | 编译平台及构建工具 | Platform 35、Build Tools 35.0.0；本机工具位于不提交的 `.local-tools/` |
| Git / GitHub | 版本管理、审核修订及团队提交记录 | 已用于代码与文档；各轮被测提交和产物标识见测试记录 |
| GitHub Actions | 在推送 `main` 或提交 PR 时构建、运行自动测试和 Lint，上传产物 | 工作流已配置；本轮运行结论以对应提交的实际 Actions 结果及测试报告为准 |
| Markdown / Mermaid | 文档编写及架构、数据关系和流程图表达 | 已用于本轮文档；图示不是可运行软件 |

构建工具将源码与资源打包为可安装或分发的产物。可重复的命令、环境准备与报告位置见[开发说明](development.md)。本轮构建和检查的实际结果以[执行记录](test-records.md)为准。[Android 构建文档](https://developer.android.com/build)。

本轮仍运行 `assembleDebug`、`testDebugUnitTest` 和 `lintDebug`，结果和维护提示在测试报告中统一记录。后续依赖升级应重新验证，不为消除版本提示直接更换尚未检查的依赖。

## 4. 验收设备与待登记环境

| 项目 | 已知信息或记录要求 |
| --- | --- |
| 演示设备 | 荣耀 X50，用户已确认 |
| 实际系统 | 用户本轮暂不连接实机；MagicOS、Android 版本及 API 级别后续从设备读取，不用出厂配置代替 |
| 权限与电量设置 | 每个通知测试批次记录通知权限、定时访问条件及电量优化设置 |
| 系统时间 | 记录设备时区；应用业务日期统一按北京时间解释 |
| SDK 范围 | `minSdk = 26`、`targetSdk = 35`、`compileSdk = 35`；声明范围不代表所有设备已测 |
| 应用版本 | 第二轮 `versionName = 0.2.0`、`versionCode = 2`；包名 `cn.edu.hust.ddlsaver`；schema v2 |
| 构建证据 | 保存实际依赖版本、构建命令、代码提交号和 APK 标识，关联[测试记录](test-records.md) |

## 5. 更新约定

1. 集成某项技术时，将“未集成”改为实际状态，填写版本及配置文件位置。
2. 引入新的第三方库时，补充用途、官方来源和许可说明；不把仅阅读过的资料列为已使用依赖。
3. 替换方案时，记录原因和受影响的设计、测试用例，保留相应 Git 修改记录。
4. 依赖已集成、构建成功和实机测试通过分别记录；一种状态不能代替另一种。

当前未采用云服务、账户系统、OCR 或生成式对话服务。具体功能范围以[需求分析](requirements.md)为准，开发进展见[项目状态](../STATUS.md)。
