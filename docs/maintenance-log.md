# 维护与修复日志

更新日期：2026-11-02（America/Los_Angeles）。本页按实际处理顺序记录，机器执行保留 UTC；未记录的用户消息时分不补造。当前演示产品冻结为 **0.3.5／versionCode 8、Room schema 4**（`codex/demo-final-20261101`／`f024ece`）；日历导航等分项通过，完成状态、详情返回与来源空交集通过，07:59:33 UTC 最终 18 项审计全部通过，5 条 QA 已清理，原数据／附件／设置及全部原 33 条提醒保持，旧批次按原版本保留。

## 2026-11-01：分工、PR 与演示稳定性

| 处理 | 实际结果与证据 |
| --- | --- |
| 成员与责任 | 当前成员为支盟渊、付泽文、岳羽泉。支盟渊接手页面、图片、角色的后续实现、复核、兼容验证与演示；旧成员及历史成果按原来源保留。见[用户记录](../interaction-records/支盟渊/2026-11-01.md)和[任务板](../team/README.md)。 |
| PR 审阅合并 | 付泽文 [PR #10](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/10) 在 head `d4e09428b4deb87110067ff356c78a3cb98f4eac` 获 [APPROVE](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/10#pullrequestreview-5732043244)，实际 merge `c1e94f3d80fcf4aa6d9983c4e3be5e7b4e0a1d48`。当时 7 项规划扩展仍是提案；合并不代表功能已实现或规则已全部确认。 |
| 保存前轮增量 | `778429e0e0b8311c491a3167400e95f4b085f08c` 保存此前 7 份源码／测试，包括图片导入诊断、剪贴板流程和提醒恢复补测。合并后本机基线 167 方法／194 次执行全部通过。 |
| 图片权限反馈 | [bdcd76f](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/bdcd76f916aae546bd146ce1fb1a14859bcd5eb4) 仅对来源阶段的 SecurityException 提示重新复制图片或从相册重选；内部存储错误保留原提示。草稿保护、批次回滚和原附件保持已有回归；OBS-01 授权失效机制未由此解决。 |
| 提醒恢复诊断 | 同一提交加入仅调试包启用的私有恢复记录，区分健康错误、通知阻塞、超时、取消和异常，并保留 BOOT 独立记录。静态审阅发现同步写盘可能拖延 Receiver 的 P2，随后改成独立单写入器，最多 6 个待写槽加 1 个在途记录；复审通过。中间阶段可合并、进程退出可丢未写记录，恢复业务和广播收尾不等待磁盘。 |
| 用户可见说明 | 设置页说明重启或强停后打开应用检查提醒；版本升为 0.3.4／7。未改变数据库 schema、提醒策略、系统权限或依赖版本。新增诊断不等于 OBS-02 自动恢复已修复。 |
| 历史 CI 失败 | [CI #45](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41387758880) 为 169 次执行／1 失败，最后切回经典形象保存后等待首页超时。根因未定；增强选中／按钮断言与安全状态诊断，未延长等待或删除测试。 |
| 定向失败与修订 | 首轮 108 次执行有 2 次失败，来自两种 API 下取消异常对象的 assertSame 身份前提。改为验证取消传播、分类和 finish 一次，生产取消语义保持。原报告及源码快照保留。 |
| 最终本机回归 | 2026-11-02 04:35:10.510483—04:38:44.548167 UTC：完整构建成功，28 XML／179 方法／218 次执行，失败／错误／跳过各 0，Lint 0／16。新增 1 条 UseKtx、原 1 条 InlinedApi 消失；不能称全部警告均为旧项。 |
| 推送与远程回归 | `b95b6c6e699d0c0537ff15bc4ec00c50507ad13d` 汇总文档和证据并与前两次代码提交推送。[CI #46](test-evidence/RUN-20261101-DEMO-STABILITY/ci46/README.md) 在 04:43:35—04:48:42 UTC 成功：28 XML／179 方法／218 次执行全通过，Lint 0／17，比本机多 1 条 OldTargetApi。两轮执行分开统计。 |

本机 APK 为 15,119,011 字节，SHA-256 `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`；证书与原设备包一致。详见[交付摘要](test-evidence/RUN-20261101-DEMO-STABILITY/delivery.json)、[完整修复证据](test-evidence/RUN-20261101-DEMO-STABILITY/README.md)、[测试报告](test-report.md#18-pr-合并基线与-034-演示稳定性2026-11-01)及[执行记录](test-records.md#21-run-20261101-demo-stability合并基线与演示稳定性)。这些软件工作按 AI 辅助执行记录，不补记为其他成员个人开发成果。

## 2026-11-01：转入新版手机复验

用户先选择“先交付本机验证版，手机稍后再测”，随后明确要求把上述工作整理为日志，并告知手机已连接、开始手机测试。新批次 [RUN-20261101-DEVICE-034](test-evidence/RUN-20261101-DEVICE-034/README.md) 已在 2026-11-02 04:56:41—04:58:33 UTC 完成只读核对和新鲜基线：1 台授权荣耀 ALI-AN00／Android 15／API 35，已解锁，原包 0.3.3／6、签名匹配，原业务 3 条事项／4 附件。通知允许、精确访问 allow、通道 3，活跃产品通知 0；28 条提醒中 12 条 SENT、2 条 active、0 条 cancelPending，本日 DAILY 已 SENT。检查前产品已 stopped／无进程／无 Alarm，原因未定，未归因本轮操作。安装、数据保持、图片提示、提醒恢复和收尾结果继续按实际追加，该准备阶段尚未登记升级成功。

| 未解决项 | 当前状态与后续检查 |
| --- | --- |
| OBS-01 | 来源权限拒绝提示已改善；授权失效机制仍未知。复验失败提示、原草稿／附件保持和 fresh copy 恢复。 |
| OBS-02 | 0.3.3 的真实重启自动恢复子项未通过；0.3.4 在该准备阶段尚待独立手机观察，后续结果见下文。分别核对未主动打开窗口与主动打开后的结果，不把记录 completed 当作通知送达。 |
| CI #45 页面超时 | 随后本机及 CI #46 流程通过，原失败根因仍未确定；诊断保留供复现。 |
| 完整验收 | 新版本设备覆盖、完整 DA／TC、持续锁屏／自然 Doze、真实每日汇总点击及正式演练按实际步骤推进。 |

### 同签名升级与正常首启分项

上述准备态已随 `f19e3fd` 提交并推送。用户确认系统安装后，2026-11-02 04:59:22.860660 UTC 的覆盖安装返回 Success，实际版本 0.3.4／7、APK 哈希和签名均匹配。安装后未强停或主动打开时，六张业务表、4 附件、设置、全部提醒行、原 12 条 SENT 和控制记录均保持；05:00:12.290088 UTC 正常启动后的独立比对仍全部保持。

普通打开后 stopped／notLaunched 均 false，实际下一精确 Alarm 恢复为北京时间 11 月 3 日 08:00，设置页新增提醒说明完整可见。安装后和前台恢复后均无 receiver 诊断文件；普通前台恢复本来不写此记录，不能借缺失记录判断 PACKAGE_REPLACED 是否交付。[新批次记录](test-evidence/RUN-20261101-DEVICE-034/README.md)已追加本分项，当时图片测试正在进行，重启自动恢复及整批收尾尚未判定。

### 图片新提示重试与回滚实测

`QA-034-1101-IMAGE` 两次独立粘贴并保存 2 张图片后，复用强停前旧 orange URI 的实际粘贴失败，完整来源权限提示可见；原 2 张附件、已存描述、未保存的新描述和草稿保持，私有文件 6→6。此轮未修改日志属性，诊断日志为空，仍保留实际界面失败证据。

fresh copy 恢复到 3 张；坏图被独立拒绝，草稿／描述保持且无残留副本，保存前数据库仍为原 2 张。05:06:41.856606 UTC 的[最终结果](test-evidence/RUN-20261101-DEVICE-034/image-v034-result.json)确认保存后及重进编辑均为 3 张及新描述，原 2 张附件行和字节保持。仅将 **0.3.4 本样本提示、重试与回滚子项**记为通过；OBS-01 授权失效机制未关闭。10 份安全 JSON 和 4 张已审阅截图已按[哈希清单](test-evidence/RUN-20261101-DEVICE-034/images-files-hashes.json)原样归档；本轮空诊断日志与旧批次日志分开。当时继续进行夹具卸载后的独立副本及后续角色／提醒步骤。

### 远程文档提交核对

2026-11-02 05:11:49 UTC 只读核对：远程 main 仍为准备状态文档提交 `f19e3fd`，开放 PR 为 0；该提交的 [CI #47／41391476843](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41391476843) API 状态为 success。此处只核对运行状态，未下载或解析报告，不新增测试次数统计；源码完整远程测试与 Lint 证据仍引用 [CI #46](test-evidence/RUN-20261101-DEMO-STABILITY/ci46/README.md)。

### 来源卸载与角色保存分项

2026-11-02 05:08:11 UTC 本轮新增合成夹具卸载成功并确认不存在，合成剪贴板清空；05:11:45 UTC 的[普通重开结果](test-evidence/RUN-20261101-DEVICE-034/image-final-result.json)确认 3 私有副本行／字节／描述保持、三预览可见。一次 am kill 后 6 次观察仍有进程，未重试或强停替代，冷启动与排除缓存条件未建立；不登记为产品缺陷。

05:13:39 UTC 的[角色往返](test-evidence/RUN-20261101-DEVICE-034/character-roundtrip-result.json)完成经典→Q 版保存回首页并重入、再回经典保存回首页并普通重开保持；预览未提前落库，其余设置、六表、附件与提醒摘要不变。CI #45 超时本次未复现，原根因仍未知。17 份安全文件已按[清单](test-evidence/RUN-20261101-DEVICE-034/source-role-files-hashes.json)原样归档；蓝图两次截图因含非测试内容均排除，不再补拍，三预览实际观察和私有字节比对保留。该阶段后的正常通知、真实重启与收尾结果按后续时序追加。

### 普通后台提醒与完成后撤销

2026-11-02 05:19:00 UTC（手机北京时间 13:19:00）的 NORMAL 危险期计划与真实精确 Alarm 匹配，系统通知创建相对计划 +191 ms，13:19:12 首次轮询观察到。18 个样本均为亮屏、USB 充电、App 后台及 deviceidle ACTIVE，不覆盖锁屏或自然 Doze。[Alarm journal](test-evidence/RUN-20261101-DEVICE-034/normal-after-notification-journal-1.json)首次读取即 finished／completed、通知与精确权限可用、无健康错误，时间和当前 boot 的校验通过；05:03:28.731 UTC 的旧 BOOT 项另行保留，不当作新重启结论。

普通入口打开后，标记完成前同一通知仍在；05:19:45.903714 UTC 的[结果](test-evidence/RUN-20261101-DEVICE-034/normal-completion-result.json)确认标记完成后同一通知撤销，NORMAL 的两条计划 inactive、cancelPending=0，未点击通知。IMAGE 的未来计划当时仍 active，不把结果字段 allQaPlansInactive 扩大为全批 QA 均停用。16 JSON 与 2 张主线程已审阅截图按[清单](test-evidence/RUN-20261101-DEVICE-034/normal-files-hashes.json)归档；循环中间快照的 observed=false 与最终 observed=true 的差异及相同 samples 已在[批次说明](test-evidence/RUN-20261101-DEVICE-034/README.md#普通后台通知诊断与完成撤销)解释，原字节保留。该 NORMAL 阶段结束时，真实重启与本批收尾尚待实际结果。

主线程原样保留[截图排除记录](test-evidence/RUN-20261101-DEVICE-034/screenshot-review-exclusions.json)及[哈希清单](test-evidence/RUN-20261101-DEVICE-034/review-files-hashes.json)，两张含非测试通知的蓝图截图未归档，未转录通知内容。

### 恢复执行时的远程新增核对

2026-11-02 06:50:50 UTC 只读核对发现远程 main 已推进至 [646eaa3](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/646eaa3)：付泽文 [PR #11](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/11) 的 head 为 `fe058da`，已合入既有月历的每日事项数量显示，并在既有 SaverAppFlowTest 增加 3 条断言。数量按列表、关键词、来源／标签、危险期筛选计算，不按选中单日缩减；这不是重新实现整个月历，也不是 PX-01／PA-01 整体验收。[PR #12](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/12)（head `10913c6`）的周／日周期界面及筛选仍开放、未合入，PX-03—PX-07 没有因此实现。贡献归于原 PR 作者，不计为本批 AI 新开发。

本批手机仍测试 `bdcd76f` 的 0.3.4／7，APK SHA-256 `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`；远程增量版本号也为 0.3.4／7，应以源码提交和 APK 哈希区分。本批本机与 CI #46 的 218 次执行，以及本批手机结果，均不覆盖上述远程新增代码。 本轮先保留手机实际被测产物，工作区只取得远程引用，没有为这次远程核对安装新包；后续文档同步合并须保留 PR #11 的使用手册增量。

[CI #49／41400509769](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41400509769) 对 `646eaa3` 的运行、Build and check、APK 与报告上传步骤已由 API 核对 success（job `125131111697`）。随后读取 artifacts 列表发生 WinError 10060 连接超时，尚未取得或解析该轮原始报告，故不登记新测试次数或 Lint 数；网络取证超时不是远程测试失败，也未为此重跑测试。PR #12 的 CI #50 最新状态另待核对。

### 继续执行、真实重启与前台恢复对照

执行子代理因额度限制中断后，用户要求继续未完成任务。主线程只读确认现场后接管手机操作；未补造中断期间步骤或用户消息时分。06:46:43 UTC 实际 reboot，boot ID 变化且 uptime 从 11418.7 降至 57.03 秒；用户确认仅解锁，未开 App 或点通知。06:48 时段首解锁观察无产品进程／Alarm，stopped=false、notLaunched=false，见[开始阶段清单](test-evidence/RUN-20261101-DEVICE-034/boot-start-files-hashes.json)。

自动窗口至 06:55:43 UTC，41 样本全部非前台、解锁亮屏、USB 充电、ACTIVE，最后设备时间为北京时间 14:54 计划 +99 秒，均无 Alarm／BOOT QA 通知，BOOT 危险期仍 PENDING。诊断保留旧 BOOT 05:03:28.731 与旧 Alarm 05:19，elapsed 不属于新 boot；没有新记录不能独立证明广播未交付。**本样本自动恢复仍未通过，OBS-02 根因开放。**

窗口结束后才普通打开，06:55:56 UTC 对照确认补发：系统创建 `1793602550060`，计划 `1793602440000`，晚 110.060 秒；BOOT danger SENT，下一实际精确 Alarm 为北京时间 15:01 的 IMAGE。诊断仍旧；普通打开恢复成功不关闭自动恢复失败。重启后 `persist.log.tag` 由 S 变 M，测试脚本未设置该变化；完整观察后 06:56:05 UTC 仅恢复原 S，其余日志属性空值保持，不归因于 App。结束阶段 11 份安全 JSON 已按[清单](test-evidence/RUN-20261101-DEVICE-034/boot-end-files-hashes.json)归档；原 QA 事项行保持，旧 BOOT／Alarm 全阶段相同。最终清理和原数据复核随后另记。

### 本批清理与最终数据审计

06:58:11 UTC 通过真实 UI 删除本批 BOOT／IMAGE／NORMAL 三个已记录 QA，剩余 QA 0、相关通知撤销；五条 QA 计划 inactive、cancelPending=0，账本及两条 QA SENT 保留；总账本33行／14 SENT分别为原28+QA5、原12+QA2，不写成全账本不增。原 12 条 SENT 按基线精确 key 集核对完整行未变。

06:58:57.861995 UTC 的[最终审计](test-evidence/RUN-20261101-DEVICE-034/final-data-and-state-audit.json)17 项全部通过：原六表（3 事项／4 附件等）、四份附件实际字节、设置和原 SENT 保持；11 项记录的系统设置、权限／通道、精确访问、字体显示、网络、电源／idle 与日志属性一致，夹具已移除，临时 UI XML 与私有数据库目录清理完成。未 force-stop 收尾、未改钟或删账本；手机以空查询正常主页停留，stopped=false／notLaunched=false、进程存在，下一实际精确 Alarm 为北京时间 11 月 3 日 08:00，readyToDisconnect=true。初始 stopped=true 有意不恢复为强停。收尾23份安全JSON按[清单](test-evidence/RUN-20261101-DEVICE-034/cleanup-files-hashes.json)原样归档。**本批设备分项及收尾完成，自动重启恢复失败仍保留，整体验收未完成。**

### 远程随后合入周／日界面

06:58:10 UTC 的新快照确认队友已合入 [PR #12](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/12)，远程 main 为 `99490e692b67f3d4f46548aa8e97a0a56d864221`，开放 PR 为 0。该 PR 原 head `10913c6` 的 [CI #50／41402242211](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41402242211) 为 success；merge 后 [CI #51／41403137121](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41403137121) 当时运行中。前文 06:50 的 PR #12 待审是历史快照，不再表示当前状态；此次由队友合入，不记为本线程执行审阅或 merge。本批手机仍为 `bdcd76f`／APK `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`，新月历／周日代码未安装，本批本机与手机结论不覆盖它。

## 2026-11-01：冻结 0.3.5 与日历实机验证

用户要求在 1 小时内交付最终演示版并开始准备讲解。本轮 2026-11-02 07:18:31 UTC 开始，截止 08:18:31 UTC；冻结 `codex/demo-final-20261101` 的产品源码 `f024ece`，版本 0.3.5／8、schema 4。纳入月格数量、月／周／日增量；重复周期计算基础保留，但没有可用的重复任务界面，后续 PR #14 迁移不纳入。

发现周视图折叠会把整周结果缩为单日，移除周期判断对面板展开状态的依赖，并增加页面回归；旧行定向 1／1 失败、修复后 1／0 通过。修复前中断的全量记录保留；随后完整构建 29 XML／185 方法／224 次执行通过，Lint 0／16，源码字节前后一致。目标 APK 已同签名覆盖手机，07:32:17 的安装身份核对和 07:32:32 的正常首启数据比对通过，原 33 条提醒及 14 条 SENT 保持；辅助程序的 stdout 严格断言异常与基线 scope 模板误标均保留，不猜测缺失原输出或重写原文件。

五样本的月格、周／日、折叠重开、空周期、跨年和关键词分项已通过，完成状态、详情返回和来源空交集也已通过；07:59:33 UTC 最终 18 项审计全部通过，5 条 QA 已清理，原数据／附件／设置及全部原 33 条提醒保持，远程冻结分支已推送但尚无新 CI 结果。准确 SHA、时间、后续实测及安全归档入口见[本轮记录](test-evidence/RUN-20261101-CALENDAR-FINAL/README.md)。准备演示及代码阅读见[交付页](demo-final-0.3.5.md)，OBS-01／OBS-02 未因此关闭。

07:59:09 UTC 已通过真实界面删除本轮 5 条 QA，07:59:33.565965 最终 18 项审计通过：原六表、4 附件字节、设置、原 33 条提醒逐行摘要与原 14 条 SENT 精确集合保持；新增 5 条计划停用且无待取消、QA SENT 为 0，提醒历史保留。11 项系统设置、通知策略与日志属性保持；手机返回正常空查询首页，实际包身份及下一北京时间 11 月 3 日 08:00 精确 Alarm 核对完成，可断开。未强停或删账本收尾，临时 UI／数据库文件已清理。任务开始至手机审计完成约 41 分钟；本轮完成不关闭 OBS-01／OBS-02，也不代替完整课程验收和正式演练。

本轮原始[最终审计](test-evidence/RUN-20261101-CALENDAR-FINAL/device/cleanup/final-data-and-state-audit.json)中的 runtimeIntent 沿用旧模板“pre-test stopped=true”；[独立注释](test-evidence/RUN-20261101-CALENDAR-FINAL/device/cleanup/final-audit-template-annotation.json)说明本批实际基线和最终均 stopped=false／notLaunched=false，原文件未改。最终提醒账本 38 行＝原 33＋新增 5，原控制摘要一致。本批安全证据 165 份、哈希清单 16 份均已归档核对，远程 CI 结果仍独立登记。

## 2026-11-02：PR #14 本地同步与文档集中整理

核对时 PR #14 已合并到远程 `eaa9a59`，无开放 PR，PR CI #67 与主线 CI #68 均成功。先以 `3e8eb3c` 保存既有五分钟材料，再在新分支 `codex/local-sync-20261102` 合并主线（`4dd4320`）；原周历折叠修复及回归保留。以 `d602835` 区分新本地 0.3.6／9、schema 5 与手机旧演示包，不覆盖安装手机。

本轮完整构建、197 方法／236 次执行全通过，Lint 0 错误／16 警告；95 个产品输入构建前后相同，13 项核验通过，见[本机证据](test-evidence/RUN-20261102-LOCAL-SYNC/README.md)。审阅覆盖重复系列事务、迁移、跳过记录、后续范围及历史保留，未发现阻断同步的高置信缺陷；底层模板异常的旧文案、长期后台和新功能设备条件留作后续。

根目录四份 Markdown 移入 `docs/project/`，原想法和课程要求正文保留，修复相对链接；[文档总入口](README.md)与[当前源码导航](source-navigation.md)区分新本地代码和手机 0.3.5。新导航给出主要功能、实际文件／函数／行号及三条阅读路线；五分钟演示仍用原手机版本。完整操作与范围见[审阅记录 R-016](review-log.md#r-016pr-14-已合并核查本地同步与文档归档)。
