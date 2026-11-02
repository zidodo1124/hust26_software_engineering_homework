# 本地同步 0.3.6 验证记录

本记录对应合入 PR #14 后的本机代码：**0.3.6／versionCode 9、Room schema 5**。本机完整构建、测试和 Lint 已通过；此次没有安装或操作手机，手机已有 0.3.5 的实机结果不扩展为 0.3.6 已验收。

## 时间、代码与构建命令

- 执行窗口：2026-11-02 **09:54:45—10:00:08 UTC**，对应 **01:54:45—02:00:08 America/Los_Angeles（PST，UTC−08:00）**。
- 当时构建 HEAD 为 `4dd432086a522183c0a60e77ff8d2184d876dcb8`，工作区另有尚未提交的两行版本修改：0.3.5／8 → 0.3.6／9。不能将 HEAD `4dd4320` 单独称为本次被测产品。
- 测试后同一产品内容已提交为 **`d602835920080bf4b7f3bb2fd509fc454f884391`**。归档时该提交仅在本地，未伪造远程提交链接。
- [产品输入核对](source-inputs.json)记录 95 个输入文件的 SHA-256：构建前、构建后、归档时工作区全部一致，并确认当前输入与上述产品提交一致。

在项目根目录执行的命令：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/gradle.ps1 --no-daemon --console=plain --stacktrace --continue --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

脚本采用本地 JDK 17 和 Android SDK。Gradle 返回 0，日志显示 `BUILD SUCCESSFUL in 5m 21s`；55 项任务中 26 项执行、29 项为最新状态，单元测试任务实际执行，并非复用旧测试报告。

## 本机结果

| 项目 | 实际结果 |
| --- | --- |
| JUnit XML | 29 份，见下方逐项入口 |
| 测试方法／执行次数 | 197 个不同方法，236 次实际执行 |
| 失败／错误／跳过 | 0／0／0 |
| Lint | 0 错误、16 警告 |
| 原日历修复 | `HomeScreen` 周范围不依赖面板展开状态；原 48 行折叠回归保留并实际执行通过 |
| 验证检查 | [verification.json](verification.json) 13 项全部通过，包含新报告时间、源文件稳定、APK 版本、schema 和回归结果 |
| APK | `cn.edu.hust.ddlsaver`，0.3.6／9，minSdk 26、targetSdk 35，15,184,547 字节 |
| APK SHA-256 | `84cb2026fa087c4623f4fa948c2e1b2cd3006574e6aae424ff887e4eb791702c` |

测试计数来自 XML 中的真实 `testcase`；197 是去除 SDK 参数后按类名及方法名去重的数量，236 才是执行次数。图片损坏输入产生的解码提示保留在日志，最终测试失败数仍为 0。

## 远程 CI 与本地验证分开记录

[远程快照](remote-snapshot.json)核对于 2026-11-02 09:51:34 UTC：PR #14 已合并，main 为 `eaa9a590a6baae46f9808eff911463d947c43434`，当时无开放 PR。

| 远程运行 | 对应源码 | 快照结论 |
| --- | --- | --- |
| [CI #67](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41416993975) | PR 分支 `8760bca29cc1e1f60448c08a21684140553d2e4b` | success |
| [CI #68](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41418746542) | main `eaa9a590a6baae46f9808eff911463d947c43434` | success |

上述 CI 成功不等于本地产品提交已在远程运行。本目录的 236 次测试、16 条 Lint 警告属于本机合并结果，不能套用为 CI #67／#68 的测试计数。远程快照只保留有关 PR、main 与两次 CI 的公开状态字段，不含凭据或请求头。

## 归档入口与完整性

- [结果摘要](summary.json)、[13 项验证](verification.json)、[构建日志](gradle.log)。
- [Lint XML](lint-results-debug.xml)、[APK 元数据](apk-badging.txt)、[源码输入哈希](source-inputs.json)。
- [归档 payload 哈希清单](files-hashes.json)：覆盖 36 个 payload 文件；`README.md` 与清单自身不纳入哈希，避免自引用。
- 29 份 JUnit XML 保持原字节。构建日志与 Lint XML 仅将本机绝对工作区前缀替换为 `WORKSPACE`，并统一为 LF；其他错误、警告、行号与计数不变。原始日志留在被 Git 忽略的本机证据目录。
- 不归档 APK、HTML、手机数据库、设备标识或个人事项。源码输入列表只含仓库产品代码、资源和构建配置。

### 原始 JUnit XML

- [TEST-cn.edu.hust.ddlsaver.data.CharacterStylePersistenceTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.data.CharacterStylePersistenceTest.xml)
- [TEST-cn.edu.hust.ddlsaver.data.RoomEventRepositoryTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.data.RoomEventRepositoryTest.xml)
- [TEST-cn.edu.hust.ddlsaver.data.RoundTwoRepositoryTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.data.RoundTwoRepositoryTest.xml)
- [TEST-cn.edu.hust.ddlsaver.data.SaverDatabaseMigrationTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.data.SaverDatabaseMigrationTest.xml)
- [TEST-cn.edu.hust.ddlsaver.domain.CharacterFeedbackTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.domain.CharacterFeedbackTest.xml)
- [TEST-cn.edu.hust.ddlsaver.domain.EventQueryTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.domain.EventQueryTest.xml)
- [TEST-cn.edu.hust.ddlsaver.domain.EventValidationTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.domain.EventValidationTest.xml)
- [TEST-cn.edu.hust.ddlsaver.domain.ExtendedEventValidationTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.domain.ExtendedEventValidationTest.xml)
- [TEST-cn.edu.hust.ddlsaver.domain.RecurrenceRulesTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.domain.RecurrenceRulesTest.xml)
- [TEST-cn.edu.hust.ddlsaver.domain.UrgencyRulesTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.domain.UrgencyRulesTest.xml)
- [TEST-cn.edu.hust.ddlsaver.platform.AttachmentImportDiagnosticsTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.platform.AttachmentImportDiagnosticsTest.xml)
- [TEST-cn.edu.hust.ddlsaver.platform.AttachmentStoreTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.platform.AttachmentStoreTest.xml)
- [TEST-cn.edu.hust.ddlsaver.reminders.AndroidReminderPlatformTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.reminders.AndroidReminderPlatformTest.xml)
- [TEST-cn.edu.hust.ddlsaver.reminders.ReminderEngineTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.reminders.ReminderEngineTest.xml)
- [TEST-cn.edu.hust.ddlsaver.reminders.ReminderRecoveryDiagnosticsTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.reminders.ReminderRecoveryDiagnosticsTest.xml)
- [TEST-cn.edu.hust.ddlsaver.reminders.ReminderRestoreIntegrationTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.reminders.ReminderRestoreIntegrationTest.xml)
- [TEST-cn.edu.hust.ddlsaver.reminders.ReminderRulesTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.reminders.ReminderRulesTest.xml)
- [TEST-cn.edu.hust.ddlsaver.reminders.ReminderStatusCopyTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.reminders.ReminderStatusCopyTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.AttachmentClipboardFlowTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.AttachmentClipboardFlowTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.CalendarGridTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.CalendarGridTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.CharacterArtworkTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.CharacterArtworkTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.CharacterAssetsTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.CharacterAssetsTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.CharacterBubbleLayoutTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.CharacterBubbleLayoutTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.ContentClipboardFlowTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.ContentClipboardFlowTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.OceanThemeContrastTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.OceanThemeContrastTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.OceanUiRenderTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.OceanUiRenderTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.ReminderColdLaunchTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.ReminderColdLaunchTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.SaverAppFlowTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.SaverAppFlowTest.xml)
- [TEST-cn.edu.hust.ddlsaver.ui.SaverViewModelFeedbackTest.xml](test-results/TEST-cn.edu.hust.ddlsaver.ui.SaverViewModelFeedbackTest.xml)
