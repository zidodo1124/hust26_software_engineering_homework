# 开发、构建与安装升级

本文当前对应 `0.3.1`、`versionCode = 4`、schema v4，包名 `cn.edu.hust.ddlsaver`；源码 [1a44059](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/1a44059abc8bef12e8fdb648ad9c4353eba375d3)。127 个不同测试方法／145 次执行全部通过，失败、错误、跳过均为 0；Debug APK 构建成功，Lint 0 错误／16 条维护警告，详见 [RUN-20261028-002](test-evidence/RUN-20261028-002/README.md)。第三轮 v0.3.0 的设备结果保留为独立历史批次。功能及未完成项见[实现计划](implementation-plan.md)和[项目状态](../STATUS.md)；分工见[小组任务板](../team/README.md)。

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

每轮实现后执行构建、自动测试和 Lint。第三轮补充提醒、真实 v2 迁移、平台桥接、通知路由／重建和角色检查，并回归既有数据、图片及界面，实际通过数、失败修复、维护提示、被测提交和 APK 标识统一记入[测试报告](test-report.md)与[测试记录](test-records.md)。本文给出复现入口，具体结果以对应提交的报告为准。

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

辅助脚本优先采用本项目 `.local-tools/jdk-*` 下的 JDK；其他电脑使用当前 `JAVA_HOME`。它只影响本次进程环境，不修改系统环境变量。

准备好 JDK 和 SDK 后，也可直接运行 Wrapper：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

macOS／Linux 使用 `./gradlew` 并配置各自的 JDK 17 与 SDK 路径。开发机为 Windows，GitHub Actions 配置为 Ubuntu 24.04；本轮实际运行状态见测试报告，macOS 尚未验证。

| 产物 | 相对仓库的位置 |
| --- | --- |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| JVM／Robolectric 测试报告 | `app/build/reports/tests/testDebugUnitTest/index.html` |
| JUnit XML | `app/build/test-results/testDebugUnitTest/` |
| Android Lint 报告 | `app/build/reports/lint-results-debug.html` |
| Room schema | `app/schemas/cn.edu.hust.ddlsaver.data.SaverDatabase/`；保留 `1.json`、`2.json`，第三轮编译导出 `3.json`，结构变更随代码提交并检查迁移 |

`build/` 产物和工具缓存不提交 Git；与验证结论相关的精简证据保存到 `docs/test-evidence/`，实际结果见[测试记录](test-records.md)。

### GitHub 自动构建与下载

第三轮源码 [857ca5f](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/857ca5faf29af4c2b861bf4529634ec66e3e13bd) 的 [CI 40530551146](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40530551146) 已核对成功：实际 132 次执行通过，CI Lint 0 错误／17 提示，比本机 16 多一条 OldTargetApi；详见 [CI 证据](test-evidence/RUN-20261027-005/ci-verification.json)。本机构建与 CI 分别打包，哈希和签名须各自核对，不能用一个包的标识代替另一个。线上 [Debug 产物](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40530551146/artifacts/12746207092)和[报告产物](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40530551146/artifacts/12745413579)可供有仓库权限者下载。

2026-10-28 荣耀 X50 已完成 v0.2.0→v0.3.0 同签名覆盖升级。该设备后续升级继续沿用已核验的本机签名环境，本次使用包为 `.local-tools/artifacts/ddl-saver-0.3.0-857ca5f-debug.apk`，不入 Git；其哈希、证书及版本在 [RUN-005](test-evidence/RUN-20261027-005/README.md) 登记。不要为安装不同签名的 CI 包卸载产品或清空用户数据。

**第二轮历史产物：** [对应源码的 GitHub Actions](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40500794045)已通过，下载报告核对为 87 次执行全部通过。CI Lint 为 0 错误、12 条警告，比本机多一条 `OldTargetApi` 提示，分别保留两边实际结果。[第二轮 Debug APK](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40500794045/artifacts/12733885228)及[测试／Lint 报告](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40500794045/artifacts/12733560898)可供有仓库访问权限的成员下载。

[Android 工作流](../.github/workflows/android.yml)在推送 `main` 或提交 PR 时运行同样的构建、自动测试与 Lint。三项检查成功后上传 `ddl-saver-debug-提交号` 安装包；测试和 Lint 报告另存为构建产物，保留 14 天。

首轮历史结果可查看源码 `1655a70` 的[工作流运行](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40490279525)。第二轮应选择对应提交的运行及产物，不把首轮成功记录当作本轮验证。下载通常需要登录有仓库访问权限的 GitHub 账号，产物超过保留期后可能不可用。CI 与本机是分别构建的产物，本地记录中的 APK 哈希不代替 CI 产物标识。

队友可在仓库的 [Actions](https://github.com/zidodo1124/hust26_software_engineering_homework/actions) 中选择对应提交的 Android 运行，在 Artifacts 下载安装包或报告。只有实际工作流结果显示成功时，才能说该次线上检查通过；工作流文件存在本身不代表已经运行。

工作流使用 [checkout](https://github.com/actions/checkout)、[setup-java](https://github.com/actions/setup-java)、[setup-android](https://github.com/android-actions/setup-android) 和 [upload-artifact](https://github.com/actions/upload-artifact)，具体版本固定在 YAML 中。权限仅为读取仓库内容，不发布 Release。

本地 Robolectric 测试需要下载对应 Android 运行库。工程通过[官方支持的系统属性](https://robolectric.org/configuring/#system-properties)显式使用 Maven Central 的 `repo.maven.apache.org` 地址；首次下载失败属于测试环境阻断，不能填写为功能通过。实际下载错误及重试结果在执行记录中保留。

### 第二轮测试入口

| 测试入口 | 主要检查内容 |
| --- | --- |
| `domain/EventQueryTest.kt`、`ExtendedEventValidationTest.kt` | AND 组合筛选、多个标签全部匹配、北京时间跨日、字段独立输入、附件 ID／路径校验 |
| `data/RoundTwoRepositoryTest.kt` | v2 重开、分类改名与停用、字段与附件归属、批量选择及回滚、过期策略、设置原子保存、旧修订冲突 |
| `data/SaverDatabaseMigrationTest.kt` | 读取历史 `1.json` 创建真实旧数据库，升级验证旧数据和设置，再保存 v2 关联并重开 |
| `platform/AttachmentStoreTest.kt` | 私有副本、格式与大小校验、导入失败清理、安全路径、预览与孤儿文件回收 |
| `ui/CalendarGridTest.kt`、`SaverAppFlowTest.kt` | 月份日期布局及本地界面流程；实际覆盖和结果见测试报告 |

排查迁移或图片问题时，可单独执行相关测试；正式记录仍应关联实际执行范围：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 :app:testDebugUnitTest --tests 'cn.edu.hust.ddlsaver.data.SaverDatabaseMigrationTest'
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 :app:testDebugUnitTest --tests 'cn.edu.hust.ddlsaver.platform.AttachmentStoreTest'
```

迁移测试直接使用仓库的历史 schema，需从仓库根目录执行。不要为使测试通过而修改 `1.json`、清空旧库或启用破坏性重建。Room 升级和迁移验证的官方依据见 [Room 迁移说明](https://developer.android.com/training/data-storage/room/migrating-db-versions)。

### 第三轮测试入口

以下代码入口用于复现检查，是否通过仍以 RUN-005 的实际 XML 和日志为准。

| 测试入口 | 本轮重点 |
| --- | --- |
| [ReminderRulesTest](../app/src/test/java/cn/edu/hust/ddlsaver/reminders/ReminderRulesTest.kt) | 北京每日时刻、危险／临近触发点、开关和汇总范围 |
| [ReminderEngineTest](../app/src/test/java/cn/edu/hust/ddlsaver/reminders/ReminderEngineTest.kt) | 同事务、重复与同刻、旧回调、状态失效、平台失败、取消快照和未确定提交窗口 |
| [SaverDatabaseMigrationTest](../app/src/test/java/cn/edu/hust/ddlsaver/data/SaverDatabaseMigrationTest.kt) | 真实 v1／v2 schema 升级，旧数据和新设置默认值 |
| [AndroidReminderPlatformTest](../app/src/test/java/cn/edu/hust/ddlsaver/reminders/AndroidReminderPlatformTest.kt) | 在 SDK 28／35 调用生产适配器，检查框架服务中的通知、PendingIntent、重排取消和降级 |
| [ReminderColdLaunchTest](../app/src/test/java/cn/edu/hust/ddlsaver/ui/ReminderColdLaunchTest.kt)、[SaverViewModelFeedbackTest](../app/src/test/java/cn/edu/hust/ddlsaver/ui/SaverViewModelFeedbackTest.kt) | 唯一 Activity 冷启动重建及受控时钟下的真实反馈生命周期 |
| [CharacterFeedbackTest](../app/src/test/java/cn/edu/hust/ddlsaver/domain/CharacterFeedbackTest.kt) | 有效事项优先级、边界、明确成功完成反馈和稳定台词 |
| [CharacterAssetsTest](../app/src/test/java/cn/edu/hust/ddlsaver/ui/CharacterAssetsTest.kt) | 固定文件哈希和 Android 平台离线解码 |
| [SaverAppFlowTest](../app/src/test/java/cn/edu/hust/ddlsaver/ui/SaverAppFlowTest.kt) | 设置校验、通知热路由、草稿暂缓、失效目标及真实 Activity 重建；冷启动和精确反馈另有独立类 |

## 3. 安装、升级与实机检查

Debug APK 用于本组开发和课程演示准备。可将实际构建产物传入手机安装，或在手机开启 USB 调试并确认授权后执行：

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

运行前确认 `adb devices` 中是自己的目标手机。2026-10-27 已在荣耀 X50（HONOR ALI-AN00，Android 15／API 35、MagicOS 9.0.0）完成 USB 调试连接、v0.2.0 首次安装和启动，前台为 `MainActivity`，空首页可见。设备此前未安装本应用，因此本次不是覆盖升级测试。实际系统构建、安装输出、原始截图及范围见[安装启动记录](test-records.md#10-run-20261027-003荣耀-x50-安装与首次启动)；[后续业务实测](test-records.md#11-run-20261027-004第二轮功能实机验证)已完成事项、分类、字段、日历、进程重开和部分批量子场景；相册保存／重开／移除、跨应用两图粘贴、错误提示和取消草稿也已有证据，确认批删、卸载测试来源／删除公共样本后重开读取，以及测试项归档也已完成。下方清单未覆盖的步骤及其他图片来源仍应继续记录。

本次 Windows 连接曾受通用 USB 驱动绑定影响，经官方配套驱动处理后 ADB 才列出设备。驱动来源、管理员权限、安装脚本非零退出与独立复查分别保留在[连接排查记录](test-records.md#9-实机连接准备未开始业务测试)，不能仅凭驱动脚本的退出码判断最终连接状态。驱动安装曾返回重启提示，本次未重启系统，后续重启后的连接情况尚未验证。

### 从 0.1.0 升级时保留数据

第二轮通过 `MIGRATION_1_2` 升级原 `ddl-saver.db`，保留事项、完成／归档状态、修订号及普通紧急阈值，新增过期策略默认为 `KEEP`。验证升级时先在首轮版本创建已知测试事项和设置，再用 `adb install -r` 覆盖安装第二轮；第一次打开后核对原数据，再新增分类、字段和图片并重开检查。

覆盖安装要求包名及签名一致，安装版本也须符合系统升级规则。本地 Debug 密钥与另一位队友或 CI 的 Debug 密钥可能不同；同为 Debug APK 并不保证能覆盖安装。遇到签名不一致时，应使用与原安装包相同的签名环境重新构建或准备独立测试设备，不把卸载当成保留数据的升级办法。卸载应用或清除应用数据会移除本机数据库及应用私有图片副本，无法据此验证迁移。

### 第二轮操作检查

1. 创建来源与多个标签，在事项中选择；改名后检查旧事项显示，停用后检查新建不可选、旧关联仍保留；重新启用后再选择。
2. 给两个事项分别添加“地点／网址”等字段，编辑其中一条后检查另一条未变化。组合关键词、来源、多个标签和日历日期，核对条件按交集筛选；检查北京时间跨日、闰年与月份切换。
3. 仅选择部分事项批量完成、归档和删除，核对未选事项不变；取消删除确认应保留数据。一键清理过期是归档操作，不是永久删除；自动归档只处理未完成的过期事项。
4. 创建事项并从相册选择多张图片，查看缩略图与单图预览，保存后重开；再取消一次带新增图片的编辑，检查原附件仍在。移除附件后保存，或删除测试事项，确认相册原图未受影响。
5. 在支持复制图片的外部应用中复制图片，再主动点击“粘贴图片”；普通图片网址或纯文本应提示无法直接导入。系统选择器、剪贴板授权与不同来源应用需要在荣耀 X50 实测。[Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)、[复制粘贴](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)。
6. 检查损坏／不可读图片、超限图片及重建页面后的草稿；错误不应清空已填文本。当前工程保护为单张 20 MiB、100 MP，缩略／预览采样目标为 320／1600 像素；这些值和 24 小时孤儿清理等待期需结合实机与审核调整，未作为用户已确认的需求上限。

以上六步保留第二轮检查入口；第三轮提醒和角色代码已加入，当前后台提醒、角色渲染是否通过仍须以本轮实际结果为准。每次实机检查均按测试模板登记应用版本、设备、实际步骤、结果和必要截图。

自动归档启用后，恢复一条已过期且未完成的事项，下一次分钟或前台检查仍会按同一规则将其归档；如果需要继续保留在待办列表，应先把过期策略改为“保留”。

### 图片剪贴板测试工具

为复现跨应用 `content://` 图片粘贴，仓库提供[独立测试夹具](../test-tools/clipboard-fixture/README.md)。它使用单独包名，只提供合成图片和明确的复制／清空按钮，不属于发布 App，也不参与产品 Gradle 构建。构建命令、资源哈希、安装边界和仅卸载夹具包的清理方式均见该目录；实机结果应注明实际来源应用和 APK 标识，不能推广为所有第三方相册均兼容。

### 第三轮提醒与角色使用步骤

第三轮 v0.3.0／3、schema 3 已通过本机检查，安装新版本前以 [RUN-005](test-evidence/RUN-20261027-005/README.md) 的实际 APK 标识为准。2026-10-27 按用户选择先交付，该批未安装新包；2026-10-28 已在 [RUN-20261028-001](test-evidence/RUN-20261028-001/README.md) 完成 v0.2.0 同签名覆盖安装并核对 schema 3 和原数据，未卸载或清空产品。

1. 打开“设置”，分别检查每日汇总、危险期、临近截止三个开关；默认都开启，汇总按北京时间 08:00、临近提前 60 分钟，普通橙色阈值仍独立。汇总输入为 HH:mm，提前量须为正分钟数。
2. 点击“设置系统通知权限”，按实际手机提示允许或进入系统设置；若权限请求不再弹框，可使用“打开系统通知设置”直接进入本包设置页。通知关闭和提醒通道关闭都会影响显示。应用不在启动时自动弹出所有授权。
3. 查看“调度方式”。Android 12 及以上可通过“允许精确提醒”进入专门访问设置；未允许时显示“近似提醒，可能延迟”，该权限不能代替通知权限。
4. 返回应用或点击“重新检查并安排提醒”，核对状态和待安排／重试数量。事项和设置保存成功但提醒失败时分别提示；不要反复新建同一事项来重试安排。
5. 当前 v0.3.0 首页展示原鲸鱼娘立绘与本地台词；成功完成后短暂显示快乐反馈。点击“素材与署名”查看非官方关系、创作链、许可及来源。用户后续要求更可爱的 Q 版并命名“蓝色大肥鱼”，[候选方案](character-q-candidates.md)尚未替换进当前包。

2026-10-28 实际设备批次为 [RUN-20261028-001](test-evidence/RUN-20261028-001/README.md)，取代准备阶段未执行的 RUN-006 预留名称。已记录两类权限、精确模式、前后台四次通知的计划与系统创建时间，单项真实点击已进入对应详情；汇总具体落点、锁屏、省电及重启等仍待核对。Doze 有调度频率限制，精确访问不代表任何系统条件下都准点。强制停止期间不保证送达；重新主动打开后恢复校对。每个通知点击都重新查数据，失效目标回到列表并说明，不能进入其他事项。运行条件依据 [AlarmManager](https://developer.android.com/develop/background-work/services/alarms)、[通知权限](https://developer.android.com/develop/ui/compose/notifications/notification-permission)及 [Android 15 强停状态说明](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state)。

本批于 2026-10-28 08:15:29 UTC 完成收尾：本轮 QA 事项已清零，原事项及四张旧测试附件核对保留，首页恢复正常，业务设置恢复有效默认值。初始 settings 无持久化行，收尾通过正常界面保存默认行，不是还原为空行。系统通知及精确提醒权限保持支盟渊已授权状态；未改设备时钟、网络或省电设置。完整设备条件和证据以本批记录为准。

## 4. 代码入口

| 位置 | 职责 |
| --- | --- |
| `MainActivity.kt` | 启动、Compose 和 ViewModel，消费新通知 Intent、保存待处理路由与恢复页面 |
| `ui/` | 首页、日历、分类、字段表单、附件交互、批量管理、页面状态及错误反馈 |
| `domain/` | 事项和设置模型、输入约束、北京时间规则、组合查询、仓库接口 |
| `data/` | Room 实体、DAO、显式迁移、事务与仓库实现 |
| `platform/AttachmentStore.kt` | 图片副本导入、校验、采样预览及受管文件清理 |
| `reminders/` | 业务计划兼发送账本、统一最早 Alarm、平台通知、Receiver 与应用级恢复 |
| `ui/ReminderSettingsPanel.kt`、`CharacterPanel.kt` | 三类开关与权限入口、四表情和署名 |
| `app/src/test/` | 规则、迁移、存储、图片及本地界面测试；不能替代荣耀 X50 验证 |

完整包路径为 `app/src/main/java/cn/edu/hust/ddlsaver/`。图片与第三轮通知平台能力已按设计分开封装，不把 Android 系统调用放入纯时间规则中。图片采样采用平台解码选项，依据见 [Android 大图加载说明](https://developer.android.com/topic/performance/graphics/load-bitmap)。


### 从 0.3.0 升级到 0.3.1

使用同签名 Debug APK 执行覆盖安装，Room 自动运行 `MIGRATION_3_4`，新增形象默认经典立绘，旧数据及提醒账本保留。升级后进入设置，选择“Q 版形象”并保存；再次进入可切回经典立绘。两套素材均随包离线提供，无需额外下载。开发机已验证旧库迁移及设置保持，本批尚未执行荣耀 X50 升级。
