# 岳羽泉｜reminders 复核笔记（2026-11-01）

- 成员：岳羽泉
- 时区：Asia/Shanghai（UTC+08:00）
- 分支：`feature/yueyuquan-reminders`（本文件仅本人新增，不改公共任务板或其他成员文件）
- 对照：`docs/design.md` 第 7 节（D-06）；源码 `app/.../reminders/`；既有自动测试与测试报告

## 1. 代码与设计映射

| 设计要点 | 主要实现 | 说明 |
| --- | --- | --- |
| 三类计划：每日／危险／临近 | `ReminderModels.ReminderRules.eventPlans`、`dailyPlan` | 危险依赖速通时间；临近提前量与普通紧急阈值独立 |
| 发送键去重 `(eventId, deadlineAt, kind)` | `ReminderPlanEntity.key`，如 `DANGER:id:deadline` | 每日键为 `DAILY:yyyy-MM-dd` |
| 同刻合并展示、分原因记账 | `notificationTag` 含同刻 kind 签名；`ReminderEngine.reconcile` 按 `eventId:deadlineAt:triggerAt` 分组投递 | 同刻一条通知，两个 SENT 键 |
| 业务保存与计划同事务 | `ReminderPlanStore.rebuild`（调用方持有 Room 事务） | 保存成功 ≠ 系统 Alarm 必成功 |
| 单一最早 Alarm + 校对恢复 | `ReminderEngine.reconcile`、`ReminderControlEntity`、`ReminderRuntime` | 回调只触发校对，不以 Intent 快照为准 |
| 权限／精确能力分开 | `AndroidReminderPlatform.capabilities`、设置页入口 | 无通知能力时保留计划、暂停唤醒 |
| 启动／广播恢复 | `ReminderRestoreReceiver`、`ReminderAlarmReceiver` | 强停后需用户再开应用 |

## 2. 已由自动测试覆盖的行为（本机复核）

对照 `ReminderRulesTest`、`ReminderEngineTest`、`AndroidReminderPlatformTest`：

1. 同刻危险+临近：共用 tag、两个 key；不同时刻分别提示且不重复发送。
2. 无速通／关闭开关：不生成对应计划；完成／归档事项不生成截止前提醒。
3. 每日汇总按北京时间下一候选时刻，不因新安装追赶补发。
4. 仓库提交时写入计划；计划写入失败可回滚事项。
5. 改期／删除后旧计划取消路径与发送账本状态有引擎级用例。
6. 无通知权限时不安排系统唤醒；能力变化后可强制重排。

## 3. 边界与后续问题清单（不预填通过）

| 编号 | 问题 | 依据 | 建议后续动作 |
| --- | --- | --- | --- |
| B-01 | 锁屏、Doze、省电下是否准时送达未充分实测 | 测试报告：完整 TC-19—TC-28 仍部分执行 | 设备批次补测并记计划／显示时刻 |
| B-02 | 设备重启后 Alarm 重建仅有设计与恢复广播，缺独立重启证据 | design 7.3；报告未覆盖重启 | 重启后打开／不打开应用各测一轮 |
| B-03 | 强停后重开：已有“已 SENT 不重发”观察，不等于完整强停恢复验收 | RUN-20261028-001 | 扩到未到期计划重排与权限仍在场景 |
| B-04 | 通知权限完整撤销／拒绝／重授组合未测 | 测试报告 | 与设置页文案一致性一并检查 |
| B-05 | DB 提交与系统通知非同一事务，CLAIMED 未确定窗口可能重复提示 | design 7.2；引擎注释 | 保持现状说明；答辩时勿承诺恰好一次 |
| B-06 | 汇总通知点击落点（今日列表／清筛选）仍待核对 | RUN-20261028-001 | UI 路由复核（可与支盟渊联调） |
| B-07 | `urgentMinutes` 不参与临近提醒计算 | `ReminderRulesTest.separateReasonTimes...` | 符合设计；答辩时需能说清与临近提前量区别 |

## 4. 本轮功能验证

### 4.1 环境尝试（Gradle／Robolectric）

- 目标命令：`.\gradlew.bat testDebugUnitTest --tests "cn.edu.hust.ddlsaver.reminders.*"`
- JDK：本机 `JAVA_HOME` 原指向 JDK 25 且路径异常；改用 IntelliJ JBR 21 后 Gradle 可启动。
- 结果：**未能完成本机 Gradle 测试**。原因：未安装 Android SDK，缺少 `local.properties`／`ANDROID_HOME`（`SDK location not found`）。
- 仓库既有 reminders 自动测试规模：`ReminderRulesTest` 6、`ReminderEngineTest` 16、`AndroidReminderPlatformTest` 3（共 25 个 `@Test`）。历史 CI／RUN-005 曾全量通过，但不能代替本机本次复跑。

### 4.2 可执行回退验证（纯规则脚本）

在无 SDK 条件下，对 `ReminderRules` 与 `ReminderRulesTest` 对齐的纯逻辑做可运行核对：

- 脚本：`test-tools/yueyuquan_reminder_rules_check.py`（本人新增）
- 命令：`python test-tools/yueyuquan_reminder_rules_check.py`
- 结果（2026-11-01 本机）：`ALL_CHECKS_PASSED count=4`
  - 同刻危险+临近：共用 tag、双 key
  - 不同时刻：分 tag
  - 无速通／关开关／完成／归档：不生成对应计划
  - 每日汇总下一北京时间候选，不追赶补发
- 边界：不覆盖 `ReminderEngine` 事务、平台 Alarm、权限与真机通知。

### 4.3 结论

设计第 7 节与 `reminders/` 映射已核对；规则层关键去重／合并行为与测试意图一致。设备类缺口 B-01—B-04、B-06 仍开放。安装 Android SDK 后应补跑完整 `reminders.*` 单元测试，并在 CI 或真机上留证。

## 5. 与后续 to-do 的衔接

1. 计划／去重／同刻合并：在既有用例上针对 B-05／改期边界做复核或补测（下一 to-do）。
2. 旧提醒失效：围绕完成／归档／删除／改期补强。
3. 权限文案：对照 B-04。
4. 实机：优先 B-01—B-03、B-06。
