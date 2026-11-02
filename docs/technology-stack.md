# HUSTer's DDL Saver 技术清单

- 小组：花园鳗。
- 更新日期：2026-11-02。
- 依据：用户允许根据项目需要选择语言和框架，并要求集中说明实际采用的技术。
- 当前本地：`codex/local-sync-20261102`／`d602835`，0.3.6／9、schema 5，已纳入 PR #14；197 方法／236 次执行通过、Lint 0／16，依赖未变。见[本机证据](test-evidence/RUN-20261102-LOCAL-SYNC/README.md)和[源码导航](source-navigation.md)。
- 手机版本：冻结演示分支 `codex/demo-final-20261101`，产品源码 `f024ece`，0.3.5／versionCode 8、Room schema v4；依赖不变。完整本机 185 方法／224 次执行通过、Lint 0／16；荣耀 X50 安装与首启数据保持，月格、周／日与跨年等分项通过，完成状态、详情返回与来源空交集通过，07:59:33 UTC 最终 18 项审计全部通过，5 条 QA 已清理，原数据／附件／设置及全部原 33 条提醒保持。准确范围见[本轮证据](test-evidence/RUN-20261101-CALENDAR-FINAL/README.md)。主线后续迁移不纳入本包，OBS-01／OBS-02 仍开放。

本文用于回答“项目使用什么技术、各自负责什么、为什么选择、实际使用哪个版本”。后续新增、替换或移除依赖时同步更新，具体模块和接口见[设计文档](design.md)，安装与日常操作见[用户使用手册](user-guide.md)。

## 1. 选择依据

第一版面向 Android，采用离线单机模式，需要访问系统通知、定时任务、图片选择和本地存储。因此采用 Android 原生方案，直接使用平台能力。小组熟悉 Python、C++、Java，用户未限定实现语言；本轮选择 Kotlin 配合 Compose 编写应用，不将成员熟悉的语言全部列为项目依赖。

Kotlin 支持与 Java 互操作，Compose 提供基于 Kotlin 的原生界面开发方式。本轮延续既有方案，不新增第三方依赖或升级依赖版本；日历和分类页面使用已有 Compose 组件，图片使用 Android 平台接口。构建与自动检查在开发机进行，荣耀 X50 的实机结果按独立 RUN 批次记录。[Kotlin 官方说明](https://kotlinlang.org/docs/android-overview.html)、[Compose 官方介绍](https://developer.android.com/compose)。

## 2. 应用与测试技术

| 类别 | 本轮选择 | 在项目中的用途 | 当前采用状态 | 版本记录 |
| --- | --- | --- | --- | --- |
| 实现语言 | Kotlin | 业务规则、界面状态和数据访问 | 已集成 | Kotlin 与 Compose 编译插件均为 2.1.20 |
| 界面 | Jetpack Compose、Material 3 | 首页卡片、表单、详情、分类管理、月／周／日日历、组合筛选、批量选择、设置和角色气泡 | 沿用现有组件绘制气泡并适配窄屏／大字号；0.3.2 已有目标机操作与 150% 字号分项，0.3.3 修复版也已复验气泡及字号布局 | Compose BOM 2025.04.01；库版本由 BOM 管理 |
| Activity 与界面状态 | Activity Compose、AndroidX ViewModel / Lifecycle | Activity 入口、页面状态、生命周期感知订阅及操作协调 | 已集成 | Activity 1.10.1；Lifecycle 2.9.0 |
| 异步与数据订阅 | Kotlin Coroutines / Flow | 数据库观察、后台数据操作和页面更新 | 已集成 | 1.10.2 |
| 结构化存储 | Room / SQLite | 保存事项、分类关联、字段、附件元数据、设置、提醒与重复系列／周期；业务变化与计划重建同事务 | schema 5 共 11 表，`MIGRATION_4_5` 增加系列和周期表，保留原迁移链 | Room 2.7.1；当前本地 schema 5，手机冻结包为 4 |
| 代码生成 | KSP | 生成 Room 数据访问实现并导出 schema | 已配置 | 2.1.20-1.0.32 |
| 文件与图片导入 | Activity Result、Photo Picker、ClipboardManager、ContentResolver | 相册多选、主动粘贴可读图片 URI，流式保存私有副本 | 已集成 `PickMultipleVisualMedia` / `ImageOnly`；兼容回退由 Activity 契约提供 | 使用既有 Activity 1.10.1 及 Android 平台 API；无新增依赖 |
| 图片校验与展示 | BitmapFactory、android.media.ExifInterface、Matrix | 图片边界与真实解码校验、采样缩略图／预览、EXIF 方向处理 | 已集成 `platform/AttachmentStore.kt`；文件操作在 IO 调度器执行并由互斥锁协调 | Android 平台 API；未引入 Coil、Glide 或 AndroidX ExifInterface |
| 提醒调度 | AlarmManager | 安排截止相关定时任务，处理取消和重建 | 第三轮统一最早到期 Alarm 已加入，具体检查见 RUN-005 | Android 平台 API；精确访问独立检查，无能力时明确近似模式 |
| 通知呈现 | Android 通知接口 | 每日汇总和单项提醒，以及通知点击 | 第三轮使用平台 NotificationManager／NotificationChannel，稳定 tag 与点击路由已加入 | Android 平台 API；Android 13+ POST_NOTIFICATIONS，未新增兼容库 |
| 角色反馈 | 本地 WebP、BitmapFactory、Bitmap 与纯 Kotlin 台词规则 | 经典立绘／Q 版选择、36 条本地预设台词、状态优先级与成功完成短暂反馈；台词以气泡呈现 | 图集在 IO 线程解码，提取帧并对固定排除区置透明，随后回收临时图；保持画布及其他像素，源文件、许可与署名随包保留 | 无新增第三方运行依赖，素材见[角色记录](character-assets.md) |
| 业务规则测试 | JUnit、Coroutines Test | 时间边界、状态规则、组合查询、字段与附件元数据校验 | 当前结果见[测试报告](test-report.md)，不同方法与配置执行分别统计 | JUnit 4.13.2；Coroutines Test 1.10.2 |
| 本地 Android 行为测试 | Robolectric、AndroidX Test Core、Room Testing | 在开发机 JVM 中检查 Room 事务、真实 v1／v2 文件迁移、附件平台行为及重开持久化 | 第三轮本机提醒、迁移及资源检查通过，不能替代实机验证 | Robolectric 4.14.1；Test Core 1.6.1；Room Testing 2.7.1 |
| 本地界面流程测试 | Compose UI Test JUnit4、测试宿主 Manifest | 在 Robolectric 中操作界面，检查草稿、筛选、批量、重建及气泡长文本／字号布局 | 当前结果见[测试报告](test-report.md)，不同方法与配置执行分别统计；渲染图不作为实机截图 | 由 Compose BOM 2025.04.01 管理 |
| 实机与设备测试 | ADB、UIAutomator 界面采集、Python 操作助手与截图审阅 | 在荣耀 X50 顺序操作真实页面并记录结果 | 第二轮功能、v0.3.0 通知和 v0.3.2 形象／字号分项已有记录；0.3.3 已完成覆盖、修复表情／字号复验和 QA 收尾，未测条件继续保留 | Android 15／API 35、MagicOS 9.0.0；Platform Tools 37.0.1 |

版本依据为根目录 [build.gradle.kts](../build.gradle.kts)、[app/build.gradle.kts](../app/build.gradle.kts)和 [Gradle Wrapper 配置](../gradle/wrapper/gradle-wrapper.properties)。以上“已集成”表示依赖或代码已加入工程，不单独代表构建、测试或实机检查通过。文本复制和图片 URI 粘贴已接入；能否从某个外部应用复制图片，取决于其剪贴板内容及访问授权，普通图片网址不直接导入。

Room 是 SQLite 的访问封装，适合本项目的本地结构化数据；数据及附件生命周期的具体安排属于项目设计，不由库自动解决。[Room 官方文档](https://developer.android.com/training/data-storage/room)。

本轮通过 `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)` 执行显式升级，保留历史 schema 文件并测试旧库数据，不启用破坏性重建。[Room 迁移说明](https://developer.android.com/training/data-storage/room/migrating-db-versions)。图片选择只访问用户选定的内容，选择器不可用时由 Activity 契约回退；剪贴板中的图片引用通过 ContentResolver 读取。[Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)、[复制粘贴](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)。

解码前读取图片尺寸，再用采样控制预览内存；当前为 320 像素缩略目标及 1600 像素预览目标。[Android 大图加载说明](https://developer.android.com/topic/performance/graphics/load-bitmap)。20 MiB／100 MP 单图保护、24 小时孤儿副本保留期是本项目工程参数，不是新增第三方依赖、平台统一限制或用户已确认的业务上限，详见[图片设计](design.md#6-d-05-图片导入与附件生命周期)。

本轮保留平台 `android.media.ExifInterface`；Lint 提出迁移至 AndroidX ExifInterface 的维护建议，后续若采纳应登记新依赖并重新验证。第二轮已验证合成横／竖图和 EXIF6；不据此宣称兼容所有格式、全部方向或所有来源。

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

## 4. 验收设备与运行条件

| 项目 | 已知信息或记录要求 |
| --- | --- |
| 演示设备 | 荣耀 X50，用户已确认 |
| 实际系统 | 已从设备读取 HONOR ALI-AN00、Android 15／API 35、MagicOS 9.0.0；构建为 `ALI-AN00 9.0.0.230(CNC00E206R8P4)` |
| 权限与电量设置 | 每个通知测试批次记录通知权限、定时访问条件及电量优化设置 |
| 系统时间 | 记录设备时区；应用业务日期统一按北京时间解释 |
| SDK 范围 | `minSdk = 26`、`targetSdk = 35`、`compileSdk = 35`；声明范围不代表所有设备已测 |
| 应用版本 | 冻结演示版 `versionName = 0.3.5`、`versionCode = 8`；包名 `cn.edu.hust.ddlsaver`；schema v4；历史设备结果按各批实际版本记录 |
| 构建证据 | 保存实际依赖版本、构建命令、代码提交号和 APK 标识，关联[测试记录](test-records.md) |

## 5. 实机测试辅助工具

以下工具只服务于测试，不是发布 App 的运行依赖；本轮产品 Kotlin、Compose、Room 及其他依赖版本保持不变。

| 工具 | 实际用途与版本 | 位置与边界 |
| --- | --- | --- |
| 原生 Java 测试夹具 | 独立 Activity 与只读 ContentProvider，将固定合成图片 URI 放入系统剪贴板 | [clipboard-fixture](../test-tools/clipboard-fixture/README.md)，独立包名；不参与产品 Gradle 构建 |
| JDK 与 Android SDK 构建工具 | JDK 17、Platform 35、Build Tools 35.0.0；使用 javac、d8、aapt2、zipalign、apksigner 构建并核验夹具 | [build_fixture.py](../test-tools/clipboard-fixture/build_fixture.py)；生成 APK 与密钥留在本机忽略目录 |
| Python | 本机 3.12.4；顺序执行设备测试、读取 UTF-8 记录、整理样本与产物标识 | 操作助手留在本机忽略目录，实际证据按 RUN 导出 |
| Pillow | 本机 12.1.1；生成纯合成横图、竖图及 EXIF 方向样本 | [generate_fixtures.py](../test-tools/clipboard-fixture/generate_fixtures.py)；正常夹具构建直接核验现有资源，无需重新生成 |

夹具源码及合成资源已提交 `db28277`，资源换行保护在 `8ea56b5` 单独提交。它们只证明工具准备可复核；真实粘贴能力仍须以设备上实际跨应用操作判定，不能把夹具结果推广到所有第三方相册。

## 6. 更新约定

1. 集成某项技术时，将“未集成”改为实际状态，填写版本及配置文件位置。
2. 引入新的第三方库时，补充用途、官方来源和许可说明；不把仅阅读过的资料列为已使用依赖。
3. 替换方案时，记录原因和受影响的设计、测试用例，保留相应 Git 修改记录。
4. 依赖已集成、构建成功和实机测试通过分别记录；一种状态不能代替另一种。

当前未采用云服务、账户系统、OCR 或生成式对话服务。具体功能范围以[需求分析](requirements.md)为准，开发进展见[项目状态](project/STATUS.md)。
