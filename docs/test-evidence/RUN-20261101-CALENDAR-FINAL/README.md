# RUN-20261101-CALENDAR-FINAL：0.3.5 日历演示版

任务发起用户日期：2026-11-01；文档更新：2026-11-02（America/Los_Angeles）。本轮于 2026-11-02 07:18:31 UTC 开始，目标截止为 08:18:31 UTC。**本轮已完成：冻结源码完整构建成功，224 次测试执行全部通过；荣耀 X50 同签名升级、日历计划分项及最终清理通过。07:59:33.565965 UTC 的 18 项审计全真，手机可断开，距任务开始约 41 分钟。本轮新分支远程 CI 尚无结果；最终文档提交／推送时间按后续真实记录。**

## 冻结范围与产物标识

| 项目 | 当前记录 |
| --- | --- |
| 分支与基础 | `codex/demo-final-20261101`，基础 `5df4320`，产品源码冻结为 `f024ecea4b287c7a8768c2dc62af9be9228c9ae6`，已推送 `origin/codex/demo-final-20261101`；不等同于已通过新分支远程 CI |
| 目标包 | 0.3.5／versionCode 8、Room schema 4，依赖不变；aapt 核对包名 `cn.edu.hust.ddlsaver`、版本 0.3.5／8 |
| 最终 APK | 15,551,000 字节；SHA-256 `e689b896156514a9da7b8be57c67b3399e51e244328782f4f73ca2595fde2cd4`；本机产物为 `.local-tools/calendar-final-20261101/final/app-debug.apk` |
| APK 签名 | 校验成功；证书 SHA-256 `978833bcaf0fc001248f5d8501bfa1f9d1273922474ed601f2c60a4548a652b9`，与设备旧包基线一致；实际安装包版本／哈希／证书均独立核对匹配 |
| 日历来源 | PR #11 月历日期格数量；PR #12 月／周／日切换、周期导航和筛选 |
| 其他基础 | PR #13 仅含重复任务周期计算基础，未提供用户可用的重复任务创建／管理功能；本包不纳入 PR #14 数据迁移 |
| 新发现问题与修复 | 周视图折叠“日历”面板时，周范围因 `calendarOpen` 变化退回锚点单日；现已从 `inWeekView` 条件移除该依赖，新增 48 行页面回归，定向先红后绿 |
| 设备原包 | 0.3.4／7，SHA-256 `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`；目标包需同签名覆盖 |

上一批 [0.3.4 记录](../RUN-20261101-DEVICE-034/README.md)已独立完成收尾；其 218 次本机／CI #46 执行与设备结果不直接当作本批新源码的验证。演示与代码阅读入口为[0.3.5 交付页](../../demo-final-0.3.5.md)。

## 构建前修复与定向复测

以下时间均为 2026-11-02 UTC，按实际阶段分别记录，不累计为最终完整测试次数。

| 阶段 | 实际结果 |
| --- | --- |
| 07:20:30 修复前全量启动 | 随后确定周折叠缺陷；主线程于 07:22:14 决定停止本次精确构建进程树，保留 `pre-fix-interrupted` 记录，不把部分构建或测试报告登记为通过 |
| 07:24:28 新页面用例验证旧行为 | 1 次执行、1 次失败：周内两条事项在折叠面板后退为锚点单日的一条，确认测试能捕获缺陷 |
| 07:26:03 修改生产条件后复测 | 同一用例 1 次执行、0 失败；仍保持周／单日选择含义和存储事项数 |
| 07:27:11.975300—07:30:00.541607 最终完整构建 | `f024ece` 构建成功，用时 168.562 秒；29 份 XML、185 个去除 SDK 后缀后的独立方法、224 次实际执行，失败／错误／跳过均为 0；Lint 为 0 错误、16 警告 |

新增用例为 [SaverAppFlowTest](../../../app/src/test/java/cn/edu/hust/ddlsaver/ui/SaverAppFlowTest.kt) 的 `collapsingWeekCalendarPreservesWholeWeekUntilADayIsSelected`。本轮产品改动仅为版本号、`inWeekView` 判断和该页面回归；不改 schema 或依赖。定向通过不代替整包回归与手机操作。

最终本机执行了 `:app:cleanTestDebugUnitTest :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`，其中 cleanTest 与测试任务均实际执行，产品源文件哈希在构建前后全部相同。结果来自本批[完整构建汇总](build/final/summary.json)与构建产物，不复用旧报告。Lint 的 16 项警告为 ExifInterface 1、GradleDependency 10、ObsoleteSdkInt 1、StaticFieldLeak 1、UseKtx 3。该阶段在 Windows／JDK 17／Robolectric 上执行，不含手机操作；本机通过与远程 CI、实机验收分别登记。

## 升级前新鲜基线

本轮设备执行者于 07:21:13 UTC 报告：1 台授权荣耀设备，已解锁、Awake、USB 充电、deviceidle ACTIVE；实际版本和旧包哈希匹配。基线获取未启动或强停产品。

原业务为 3 条事项、4 份附件；schema 4。提醒账本为 33 行、14 条 SENT、2 条 active、0 条 cancelPending；其中包含上一批保留的 QA 提醒历史，不能沿用更早的 12 条 SENT 假设。本批另建私有 salt 与全部原 14 条 SENT 的精确 key 集，用于最终比对；原始数据库、私有索引、设备序列号与原事项内容不归档。

基线 `before-upgrade` 原文件的 scope 模板误写为 after upgrade；保留原文件并另作[基线注释](device/upgrade/baseline-annotation.json)。实际采集为 07:21 的旧包 versionCode 7，按时间和包身份属于安装前基线，不修改原始字段掩盖差异。该阶段安全原始记录已归档，见下方证据入口。

## 同签名覆盖与正常首启

07:32:17 UTC 已独立核对手机实际安装为 0.3.5／8，APK 哈希及证书与本批目标一致。覆盖安装仅执行一次，进程退出码为 0；辅助程序严格要求 `output == 'Success'` 的断言失败，但未保留精确 stdout，因此不能确定输出前缀或原文。实际包身份与安装后数据检查独立确认升级成功，未因断言再次安装。

安装后与 07:32:32 UTC 正常首启后的 7 组比较均通过：原六张业务表、4 份附件字节、设置、全部 33 条提醒、原 14 条 SENT 及控制状态保持。正常打开后 stopped／notLaunched 均为 false，下一实际精确 Alarm 为北京时间 2026-11-03 08:00。此结果只覆盖升级及普通启动，不代表重启后无需打开 App 的自动恢复通过。

## 实际样本与验收结果

新包安装及身份核对后，已于 07:37:17 UTC 完成 5 条 `QA-CAL-1101` 前缀事项的实际界面创建：A／B 为设备当前北京时间今日晚间，C 为明日晚间，D 为当年 12 月 31 日，E 为次年 1 月 1 日；均为未来截止、无速通时长。不改系统时钟、不改原事项，不新建测试分类。下表为实际操作结果；5 条 QA 已于 07:59:09 清理。

| 检查 | 核对范围 | 实际结果 |
| --- | --- | --- |
| 同签名升级 | 核对新 APK 与实际安装包版本／哈希／签名，原业务和附件保持 | 通过；安装及正常首启比较全真，辅助程序输出断言差异另行保留 |
| 月格数量 | 今日两条、明日一条；选中今日后其他日期数量仍保留 | 通过；11 月 2 日 2／3 日 1，选中今日列表为 2，月格仍为 2／1 |
| 月→周→日 | 本周包含 A／B／C；点某天进入对应日列表 | 通过；本周 3→日今日 2→周 3，周内点明日为 C 共 1，今天回今日 2 |
| 周折叠／展开 | 面板收起和直接重开均保持整周范围 | 通过；周 3→折叠 3→独立直接重开 3，QA 查询保持 |
| 跨年导航 | 12 月月格、跨年周和向前跨年的日导航 | 通过；12 月 31 日月格 1／日 D 1，2026-12-28—2027-01-03 周 D／E 共 2，日 D 1→E 1→1 月 2 日空 0；未核对 1 月月份网格或反向跨年导航 |
| 空周期／今天 | 空周期显示空态；“今天”返回当前日／周 | 通过；下一周 0→今天回本周 3，跨年日视图“今天”回 11 月 2 日列表摘要 2；不据此认定全局今日 DDL 统计已复验 |
| 关键词 | QA 关键词细化、恢复 | 通过；今日列表前缀 2→A 1→前缀 2 |
| 完成状态 | 完成 B 后待办日期格数量下降，完成列表出现 B | 通过；待办月格今日 1／明日 1，已完成月格今日 1、选日 B 1；清除日期再加 QA 查询后的待办摘要为 4 |
| 详情往返 | 完成 B 后从详情返回，当前查询与周期保持 | 通过；返回今日列表 A 1，日期和 QA 查询保持 |
| 来源空交集 | 未指定来源的 QA 筛选原已有来源后为零，全部来源恢复；不修改分类或归档其名称／ID | 通过；待办摘要 4→原已有来源 0→全部来源 4；只证明空交集，非正向来源／多标签组合 |
| 最终清理 | 仅删除本批精确 QA；原六表、4 附件字节、设置及原 14 条 SENT 保持；计划停用且无待取消，提醒账本历史保留，正常首页可用 | 通过；5／5 条 QA 已删除，18 项最终审计全真，原 33 条提醒全行保持；可断开手机 |

图片、角色、通知自动恢复及完整拒绝路径不因日历分项通过而重新登记全例通过。尤其 OBS-02 的真实重启自动恢复仍开放；演示前从正常入口打开应用检查提醒。分类正向交集、更多跨期边界及未实际操作的条件按本轮真实范围记录。

月／周／日阶段于 07:43:20 UTC 汇总；07:46:27 的独立重开阶段确认重新展开后立即仍为 3 条。重开辅助程序曾因滚动方向定位退出一次，有界退出和修正锚点后的单次操作保留在[工具观察](device/week-reopen/reexpanded-week-tool-observation.json)，不记为产品失败。跨年阶段于 07:50:09 完成，系统时钟未变。4 张手机截图已由主线程独立审阅，只含本轮 QA。此阶段只证明实际列表／日期格行为，不扩展为全局今日 DDL、逾期分组、归档或正向分类交集通过。

07:54:00 UTC 的完成与详情阶段确认 B 通过真实界面完成；B 只有 1 条 DEADLINE 计划，`active=0`、`cancelPending=0`，历史 `state=PENDING` 仍保留，因此结论为“计划停用、无待取消”，不写成没有 PENDING 账本。清除日期并恢复 QA 查询后的 4 是全部待办摘要，不声称四条事项同时出现在屏幕上。07:55:46 的来源阶段仅选择原有唯一来源并恢复全部来源，实际为 4→0→4；未编辑分类，未导出其名称／ID，未截分类面板。完成阶段另有 2 张已审阅安全 QA 截图，本批共 6 张。

## 最终清理与数据审计

07:59:09 UTC 已通过实际界面删除本轮 5／5 条 QA。07:59:33.565965 UTC 完成的[终稿审计](device/cleanup/final-data-and-state-audit.json)18 项均为 true，`allRequiredChecksPassed` 和 `readyToDisconnect` 均为 true。原六张业务表精确保持为 3 事项、1 来源、1 标签、1 关联、1 字段、4 附件，四附件实际字节及设置不变；全部原 33 条提醒逐行 saltedDigest 相同，原 14 条 SENT 的精确集合保持。

新增 5 条计划均 inactive、cancelPending=0，QA SENT=0；QA 事项与通知均为 0，账本键和历史状态保留，最终总账本为原 33＋新增 5＝38 行，原 controlDigest 一致。11 项系统设置、通知策略、日志属性不变，实际 0.3.5／8、APK 哈希及证书匹配。手机回到正常首页，QA 查询清空，stopped／notLaunched=false 且进程存在；下一原精确 Alarm 为北京时间 2026-11-03 08:00、window=0，与控制状态一致。临时 UI／数据库文件已清理，未强停产品、删除提醒账本或重复确认删除。

最终审计的 runtimeIntent 沿用了“不要恢复测试前 stopped=true”的静态模板文字；本批 07:21 只读实测实际为 stopped=false／notLaunched=false，最终也为 false／false。保留原审计并另作[模板注释](device/cleanup/final-audit-template-annotation.json)，不能据此误记本批起点为强停。该文字问题与前述基线 scope 误标、安装 stdout 断言及滚动定位观察分别记录，不影响实际状态比对结论。

## 已归档证据入口

全批已归档 165 份安全原始证据及 16 份哈希清单，覆盖构建、升级、全部日历分项与最终清理审计；已逐份核对字节数及 SHA-256 均匹配。6 张手机截图均已由主线程独立审阅，只含安全 QA 内容。

| 阶段 | 精简入口 |
| --- | --- |
| 完整本机构建 | [汇总](build/final/summary.json)、[产物与源码核对](build/final/verification.json)、[10 份记录清单](build/final/files-hashes.json)、[29 份 JUnit XML 清单](build/final/test-results/files-hashes.json) |
| 修复过程 | [中断记录](build/interrupted/interruption.json)、[旧条件失败汇总](build/red/summary.json)、[修复后通过汇总](build/green/summary.json)；各阶段目录及 red／green 的 test-results 均保留独立 files-hashes.json |
| 安装与首启 | [13 份安全记录清单](device/upgrade/files-hashes.json)、[安装比对](device/upgrade/install-comparison.json)、[首启比对](device/upgrade/launch-comparison.json)、[安装输出断言注释](device/upgrade/install-output-parser-observation.json) |
| 样本与月格 | [5 条样本创建结果](device/samples/calendar-samples-created.json)、[选择单日后的月格](device/month/month-counts-after-single-day-selection.json)；各目录保留独立哈希清单 |
| 周／日与独立重开 | [月／周／日汇总](device/week-day/calendar-month-week-result.json)、[直接重开结果](device/week-reopen/calendar-reexpanded-week-result.json) |
| 跨年 | [跨年导航汇总](device/cross-year/calendar-cross-year-result.json)、[跨年周截图](device/cross-year/03-cross-year-week.png)、[元旦日视图截图](device/cross-year/04-january-first-day.png) |
| 完成与详情返回 | [完成／筛选汇总](device/completion/calendar-completion-filter-result.json)、[B 实际记录与计划](device/completion/calendar-b-completed-qa.json)、[待办月格](device/completion/05-todo-counts-after-completion.png)、[已完成单日](device/completion/06-completed-day-one.png) |
| 来源空交集 | [来源空交集汇总](device/source-filter/calendar-source-empty-intersection-result.json)，仅 4→0→4，无正向来源／多标签或分类编辑覆盖 |
| 最终清理 | [5 条 QA 清理结果](device/cleanup/final-cleanup-result.json)、[18 项最终审计](device/cleanup/final-data-and-state-audit.json)、[提醒账本比对](device/cleanup/final-reminder-history-audit.json)、[30 份安全记录清单](device/cleanup/files-hashes.json) |

原开发机的同字节交付副本为 `.local-tools/demo-delivery-0.3.5/DDL-Saver-0.3.5.apk`。[仓库中的演示安装包](../../../releases/android/DDL-Saver-0.3.5.apk)与本页正式包的 SHA-256 一致，交付说明见[交付页](../../demo-final-0.3.5.md)。

## 结果追加规则

本机红／绿回归、完整构建、APK 核对、用户安装协助、实际手机步骤与失败、最终清理分别记录。只有实际执行结果才改变上表状态。截图只保留本轮 QA 或安全设置页面；含其他应用通知或原事项内容的图排除归档。保留失败与调整记录，不以延长超时、删除账本或提前打开 App 覆盖原观察。

本轮最终交付页绑定准确源码和 APK 哈希；冻结分支之外的新提交另行验证，不混入这次演示版结论。日历分项和清理完成不关闭 OBS-01／OBS-02，也不代替完整课程验收与正式演练。
