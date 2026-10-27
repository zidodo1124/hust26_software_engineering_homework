# HUSTer's DDL Saver 技术清单

- 小组：花园鳗。
- 更新日期：2026-10-27。
- 依据：用户允许根据项目需要选择语言和框架，并要求集中说明实际采用的技术。
- 当前状态：已选定本轮设计方案，尚未建立 Android 工程；下表中的应用技术均未完成集成或实机验证。

本文用于回答“项目使用什么技术、各自负责什么、为什么选择、实际使用哪个版本”。后续新增、替换或移除依赖时同步更新，具体模块和接口见[设计文档](design.md)。

## 1. 选择依据

第一版面向 Android，采用离线单机模式，需要访问系统通知、定时任务、图片选择和本地存储。因此采用 Android 原生方案，直接使用平台能力。小组熟悉 Python、C++、Java，用户未限定实现语言；本轮选择 Kotlin 配合 Compose 编写应用，不将成员熟悉的语言全部列为项目依赖。

Kotlin 支持与 Java 互操作，Compose 提供基于 Kotlin 的原生界面开发方式。选择这一组合是本项目的设计决定，后续仍需通过工程构建和荣耀 X50 实机验证。[Kotlin 官方说明](https://kotlinlang.org/docs/android-overview.html)、[Compose 官方介绍](https://developer.android.com/compose)。

## 2. 应用与测试技术

| 类别 | 本轮选择 | 在项目中的用途 | 当前采用状态 | 版本记录 |
| --- | --- | --- | --- | --- |
| 实现语言 | Kotlin | 业务规则、界面状态、数据访问及 Android 系统交互 | 设计选定，未集成 | 建立工程时记录编译器及插件版本 |
| 界面 | Jetpack Compose | 首页卡片、表单、详情、日历列表、设置及角色展示 | 设计选定，未集成 | 记录实际 Compose 依赖或 BOM 版本 |
| 界面状态 | AndroidX ViewModel | 管理页面状态与用户操作，避免将持久数据仅保存在界面组件中 | 设计选定，未集成 | 记录实际 Lifecycle 依赖版本 |
| 结构化存储 | Room / SQLite | 保存事项、标签、自定义文本字段、设置及提醒记录 | 设计选定，未集成 | 记录 Room 版本及数据库结构版本 |
| 文件与图片导入 | Android 文件接口、Photo Picker、剪贴板内容接口 | 从相册或可读取的图片内容导入附件，保存应用私有副本 | 设计选定，未集成 | 平台能力记录对应 API；支持库版本以工程为准 |
| 提醒调度 | AlarmManager | 安排与截止时间相关的系统定时任务，处理取消和重建 | 设计选定，未集成 | Android 平台 API；实际运行条件随设备记录 |
| 通知呈现 | Android 通知接口 | 发布每日汇总和单项提醒，处理通知点击跳转 | 设计选定，未集成 | Android 平台 API；兼容库如被引入则另记版本 |
| 业务规则测试 | JVM 单元测试工具，优先采用 JUnit | 验证危险期、时间边界、统计和状态转换 | 测试方案选定，未集成 | 实施时记录实际测试依赖版本 |
| 设备与界面测试 | Android 仪器化测试、Compose UI 测试及人工实机检查 | 验证页面操作、权限、附件导入和通知运行行为 | 测试方案选定，未集成 | 实施时记录运行器及测试库版本 |

Room 是 SQLite 的访问封装，适合本项目的本地结构化数据；数据及附件生命周期的具体安排属于项目设计，不由库自动解决。[Room 官方文档](https://developer.android.com/training/data-storage/room)。

定时任务与通知展示是两个环节，相关权限也需分别检查。实际是否按预期提醒，应在指定设备和权限条件下验证，不能仅凭选用了某个 API 就作保证。[AlarmManager 文档](https://developer.android.com/develop/background-work/services/alarms)、[通知权限说明](https://developer.android.com/develop/ui/compose/notifications/notification-permission)。

本地测试与设备测试按运行位置区分，单元测试与集成测试按检查范围区分；测试工具在工程中集成后才能产生实际执行结果。[Android 测试基础](https://developer.android.com/training/testing/fundamentals)。

## 3. 开发与协作工具

| 工具 | 用途 | 当前状态 |
| --- | --- | --- |
| Android Studio | Android 工程开发、设备连接与调试 | 计划使用，环境尚未验证 |
| Gradle Wrapper / Android Gradle Plugin / JDK / Android SDK | 编译、依赖管理、打包和设备测试 | 尚未建立工程，版本需一起核对并记录 |
| Git / GitHub | 版本管理、审核修订及团队提交记录 | 已用于本项目文档管理 |
| Markdown / Mermaid | 文档编写及架构、数据关系和流程图表达 | 已用于本轮文档；图示不是可运行软件 |

构建工具将源码与资源打包为可安装或分发的产物。工程建立后，应以实际构建配置和 Wrapper 文件为版本依据，并记录一次可重复的构建过程。[Android 构建文档](https://developer.android.com/build)。

## 4. 验收设备与待登记环境

| 项目 | 已知信息或记录要求 |
| --- | --- |
| 演示设备 | 荣耀 X50，用户已确认 |
| 实际系统 | MagicOS、Android 版本及 API 级别待从设备读取，不用机型的出厂配置代替 |
| 权限与电量设置 | 每个通知测试批次记录通知权限、定时访问条件及电量优化设置 |
| 系统时间 | 记录设备时区；应用业务日期统一按北京时间解释 |
| SDK 范围 | `minSdk`、`targetSdk`、`compileSdk` 在工程创建与兼容性验证时记录 |
| 构建证据 | 保存实际依赖版本、构建命令、代码提交号和 APK 标识，关联[测试记录](test-records.md) |

## 5. 更新约定

1. 集成某项技术时，将“未集成”改为实际状态，填写版本及配置文件位置。
2. 引入新的第三方库时，补充用途、官方来源和许可说明；不把仅阅读过的资料列为已使用依赖。
3. 替换方案时，记录原因和受影响的设计、测试用例，保留相应 Git 修改记录。
4. 依赖已集成、构建成功和实机测试通过分别记录；一种状态不能代替另一种。

当前未采用云服务、账户系统、OCR 或生成式对话服务。具体功能范围以[需求分析](requirements.md)为准，开发进展见[项目状态](../STATUS.md)。
