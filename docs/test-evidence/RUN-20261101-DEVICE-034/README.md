# RUN-20261101-DEVICE-034：0.3.4 手机复验

用户日期：2026-11-01（America/Los_Angeles），原始观察时间使用 UTC。**状态：同签名覆盖、正常首启、本样本图片提示／重试、来源卸载后普通重开、角色保存、普通后台通知与完成撤销分项通过；真实重启自动恢复本样本未通过、普通打开恢复成功；本批清理与 17 项审计完成，完整验收尚未完成。** 用户先选择本机交付，随后确认手机已连接并要求开始测试；检测到 1 台授权荣耀 ALI-AN00（Android 15／API 35），只读核对时已解锁。

目标包：0.3.4／versionCode 7、Room schema 4，产品源码 `bdcd76f`；本机 APK SHA-256 `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`，签名与前批设备包一致。本机与 [CI #46](../RUN-20261101-DEMO-STABILITY/ci46/README.md) 均为 218 次执行通过，不能替代本批手机结果。

本批执行期间，远程先合入 [PR #11](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/11) 月历日期格数量，06:58:10 UTC 又确认队友已合入 [PR #12](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/12) 周／日界面，main 为 [99490e6](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/99490e692b67f3d4f46548aa8e97a0a56d864221)，开放 PR 为 0。手机始终使用上列 `bdcd76f` 产物；两者版本号同为 0.3.4／7，本页结果不覆盖远程日历增量。

## 升级前只读核对

2026-11-02 04:56:41—04:58:33 UTC 已取得本批[只读状态](read-only-preflight.json)和[新鲜数据基线](before-upgrade.json)，期间没有主动启动或强停产品；[基线结果](baseline-result.json)与[原始包状态](read-only-package-state.json)分开保留。升级阶段 18 份安全证据已按[归档哈希](upgrade-files-hashes.json)核对原字节一致。

| 项目 | 观察结果 |
| --- | --- |
| 已安装包 | 0.3.3／6，15,490,535 字节，SHA-256 `6f30078bf0dfa95731afe9fae0c08f06dd19d048eb815fd2236022c90e19e65b`；证书与目标 0.3.4 包一致 |
| 原业务 | 3 条事项、1 来源、1 标签、1 关联、1 自定义字段、4 附件；本批及前批 QA 前缀事项均为 0 |
| 提醒账本 | 28 行、12 条 SENT、2 条 active、0 条 cancelPending；北京时间 11 月 2 日 DAILY 已 SENT，不清账本或改时钟制造第二次 |
| 权限与条件 | 通知允许、精确访问 allow、通道 importance 3；活跃产品通知 0；USB 充电、deviceidle ACTIVE、未强制 idle |
| 产品运行状态 | 测试操作前已为 `stopped=true`、无进程、无系统提醒 Alarm；原因尚未确定，不能归因本轮操作或据此判定新版本失败 |

## 同签名覆盖与正常首启

用户实际确认系统安装提示后，2026-11-02 04:59:22.860660 UTC 的 [`adb install -r`](install-result.json) 返回 `Success`。[实际安装包核对](device-after-install.json)为 **0.3.4／7**，APK SHA-256 与上述目标包一致，证书匹配。

| 分项 | 实际结果 |
| --- | --- |
| 安装后、主动打开前 | 未在安装后强停产品；六张业务表全部行、4 份附件字节、设置、所有提醒行、原 12 条 SENT 及控制记录均与新鲜基线一致，见[快照](after-install-before-launch.json)与[逐项比对](install-comparison.json) |
| 正常首启 | 05:00:12.290088 UTC 的[核对](first-normal-launch-result.json)中上述各项仍[全部保持](launch-comparison.json)，包状态为 stopped=false／notLaunched=false，产品进程存在；没有强停后再取首启快照 |
| 提醒安排 | 普通打开后已重建实际精确 Alarm，北京时间 11 月 3 日 08:00、window 0；这与本批初始无 Alarm 的状态分开记录，不代替重启自动恢复 |
| 页面说明 | 设置页新提示完整可见，[实际截图](01-v034-reminder-settings.png)已审阅，只有设置内容，无私有事项 |
| 恢复记录 | [安装后](upgrade-before-explicit-launch-journal.json)及[普通打开后](after-first-normal-launch-journal.json)均未见诊断文件；普通前台恢复本来不写 receiver 记录，此处无记录合理。安装后未见记录也不能反推 PACKAGE_REPLACED 广播没有收到 |

## 连续图片、旧 URI 提示与失败恢复

本轮安装独立合成剪贴板夹具，安装前不存在，见[安装记录](clipboard-fixture-install.json)。对 `QA-034-1101-IMAGE` 通过[两次独立复制／粘贴](12-image-two-incremental-draft.png)保存 2 张图片后，强停并正常重开，再复用未重新复制的旧 orange URI。05:04:39.508544 UTC 的[实际观察](image-old-uri-probe.json)为粘贴失败，[完整新提示](13-old-uri-paste-result.png)可见：`图片导入失败：来源访问权限不可用。请重新复制图片，或使用“从相册选择”重新添加。`

失败时原已保存 2 张附件及数据库描述保持，未保存的新描述和 2 张草稿图片均保留；私有文件 6→6，无新增或删除。此轮诊断日志为空，未修改日志属性，不能用日志为空否认实际失败。

重新复制同一 orange 图片后，草稿 2→3，恰好新增 1 张。另粘贴坏图出现独立的[图片无效／损坏提示](14-broken-image-draft-retained.png)，仍保留 3 张草稿和新描述，没有残留新增副本；旧 URI 失败、fresh copy 重试和坏图拒绝后的数据库 QA 整行均与[原 2 张保存态](image-two-saved.json)一致，分别见[旧 URI 后](image-after-old-uri-probe.json)、[fresh copy 后](image-after-fresh-retry-before-save.json)、[坏图后](image-after-broken-before-save.json)。完成重试后才保存，05:06:41.856606 UTC 的[结果](image-v034-result.json)确认最终 3 张与新描述，[最终详情](15-image-final-saved-detail.png)及重进编辑核对一致；原 2 张附件行和字节未变，第 3 张为新 orange 副本，描述和修订号按本次保存更新。

本段 10 份安全 JSON 与 4 张已审阅截图已独立复核并原样归档，[哈希清单](images-files-hashes.json)核对字节一致。本结论仅关闭 **0.3.4 本样本的新提示、重试与回滚实机子项**；OBS-01 的授权失效机制、其他图片来源兼容仍开放。本轮 `diagnostics=[]`，没有将旧批次的 open_source 日志套入本轮。该阶段结束时夹具仍安装，卸载后私有副本独立性及后续角色／提醒检查尚未登记通过。

## 来源卸载后的副本与角色保存

05:08:11.957278 UTC 的[卸载记录](clipboard-fixture-removal.json)确认仅移除本轮新增的合成剪贴板夹具，复查不再安装，合成剪贴板已清空。随后从普通入口重开，[05:11:45 结果](image-final-result.json)与[卸载前](image-before-fixture-removal.json)／[卸载后](image-after-fixture-removal.json)比对确认 3 份私有图片行、实际字节及描述保持，三张预览均可见。橙图与第三图的[截图](17-private-orange-after-fixture-removal.png)、[截图](17b-private-third-after-fixture-removal.png)已审阅归档。

尝试一次 am kill 后，初始 6 次观察进程仍在，见[条件观察](image-cold-start-attempt.json)；没有重试或强停替代，故本段只证明来源移除后的普通重开，未建立冷启动或排除预览缓存。该条件未达成不登记为产品缺陷。蓝图的两次截图均因含非测试内容[排除归档](screenshot-review-exclusions.json)，不再补拍；三预览结论来自实际观察，归档预览截图仅为第 2／3 张，私有副本字节另有比对。

05:13:39.006554 UTC 的[角色往返结果](character-roundtrip-result.json)：经典→Q 版预览未提前持久化，点击保存后回到首页，重入设置确认 Q 版；再选经典保存返回首页，普通重开仍为经典。[预览](18-character-alternate-preview.png)、[Q 版重入](19-character-alternate-reopened.png)、[经典重入](20-character-original-reopened.png)已审阅。其余设置字段、六张业务表、附件字节与提醒摘要保持，字体和旋转未改。历史 CI #45 的超时本次未复现，原根因仍未知。

本段 12 份安全 JSON 与 5 张已审阅截图按[哈希清单](source-role-files-hashes.json)原样归档，结论限上述实际样本及步骤。

## 普通后台通知、诊断与完成撤销

`QA-034-1101-NORMAL` 的[业务计划](qa-normal-plan.json)与[系统精确 Alarm](normal-before-background.json)均为北京时间 2026 年 11 月 2 日 13:19:00，window 0；建立后保留 183 秒，再回到桌面。[18 次观察](normal-timed-observations.json)均为 App 非前台、亮屏、USB 充电、deviceidle ACTIVE，未强制 idle、未点击通知，不覆盖持续锁屏或自然 Doze。

[最终通知结果](normal-notification-result.json)中的系统创建时间为计划 +191 ms，首次轮询在手机时间 13:19:12 观察到同一 QA tag。[Alarm 诊断](normal-after-notification-journal-1.json)首次读取即为 finished／completed，can_post 与 can_schedule_exact 均 true、has_error=false；[时间与 boot 校验](normal-journal-positive-control.json)确认本次 Alarm 的 received_at、elapsed 与当前 boot 相符。这是实际通知和诊断写入的正向对照，不把 completed 单独当作送达证明。[之前已有的 BOOT 项](normal-before-alarm-journal.json)的 received_at 为 05:03:28.731 UTC，属于同一旧 boot；该项不能证明后续真实重启恢复。

从普通入口打开 App 后、标记完成之前，[同一通知仍在](normal-notification-before-complete.json)；05:19:45.903714 UTC 的[完成结果](normal-completion-result.json)确认 UI 标记完成后同一通知消失，NORMAL 的 DANGER／DEADLINE 两条计划均 inactive、cancelPending=0，见[事项与计划](normal-completed-qa.json)及[通知状态](normal-notification-after-complete.json)。没有通过点击通知触发自动取消。结果字段 allQaPlansInactive 仅指 NORMAL，IMAGE 当时仍有未来的 active 计划，不能泛指本批全部 QA。

`normal-timed-observations.json` 保留循环中先保存、后更新标志的原始中间快照：顶层 observed=false，但最后一个 sample 已含目标通知；其 18 个 samples 与最终 observed=true 的 `normal-notification-result.json` 完全一致。判读依据最终结果和实际 sample，原文件未改写，此差异不登记为产品失败。本段 16 份安全 JSON、[新建详情](02-normal-detail.png)及[完成详情](21-normal-completed-detail.png)已按[归档清单](normal-files-hashes.json)核对原字节一致。

## 真实重启自动窗口与普通打开对照

前一阶段执行子代理因额度限制中断后，用户要求继续未完成任务；主线程先只读确认现场，再接管后续实际操作，没有补写中断期间的测试结果或用户消息时分。[新建计划](qa-boot-plan.json)为 `QA-034-1101-BOOT`，危险期触发设为北京时间 14:54:00。重启前确认实际精确 Alarm、权限与通道，stopped=false／notLaunched=false，并保留[旧诊断基线](boot-before-reboot-journal.json)。

06:46:43 UTC 的[真实重启请求](boot-reboot-request.json)后，[重连结果](boot-reconnect-result.json)确认 boot ID 已变化、uptime 从 11418.7 降至 57.03 秒。用户[确认仅解锁](boot-user-unlock-confirmation.json)，没有打开 App 或点击测试通知。06:48 时段的[首解锁观察](boot-after-first-unlock.json)为产品非前台、无产品进程、无 Alarm，包标志仍 stopped=false／notLaunched=false。

[自动观察](boot-result.json)持续至 06:55:43 UTC；41 个样本全部为产品非前台、解锁亮屏、USB 充电和 ACTIVE，无实际 Alarm 或 BOOT QA 通知，BOOT 危险期仍 PENDING。最后设备时间为计划 +99 秒，完整覆盖 +90 秒窗口。诊断仅保留旧 BOOT（05:03:28.731 UTC）与旧 Alarm（05:19）记录，其 received_at 在重启前、elapsed 不属于新 boot，不能当成本次成功。**0.3.4 本样本的重启自动恢复子项仍未通过，OBS-02 继续开放。** 未出现新诊断记录本身不能证明广播未交付，根因尚未隔离。

仅在自动窗口结束后才从普通 launcher 打开，06:55:56 UTC 的[普通打开对照](boot-explicit-launch-comparison.json)确认补发：通知创建时间为 `1793602550060`，相对计划 `1793602440000` 晚 110.060 秒；BOOT 危险期转为 SENT，下一实际精确 Alarm 恢复至北京时间 15:01（本批 IMAGE）。[诊断](boot-after-explicit-launch-journal.json)仍为旧记录，原 QA 事项行保持。此处证明普通打开后的恢复，不抵销前述自动窗口失败。

重启后观察到全局 `persist.log.tag` 从原值 S 变为 M，测试脚本没有设置该变化；其余 3 个记录的日志属性仍为空。完整自动窗口和普通打开对照后，主线程于 06:56:05 UTC [仅将此属性恢复为基线 S](post-boot-log-restoration.json)，其他值保持。不将系统重启前后属性差异归因于 App。开始阶段 17 份安全 JSON 已按[清单](boot-start-files-hashes.json)原样归档；结束窗口、普通打开及属性恢复的 11 份安全 JSON 已独立复核并按[结束阶段清单](boot-end-files-hashes.json)原样归档；旧 BOOT／Alarm 记录在所有阶段保持原样，该 BOOT 阶段结束后进入下述清理和原数据复核。

## 清理、原数据与设备最终状态

06:58:11 UTC 的[清理结果](final-cleanup-result.json)确认仅通过真实 UI 删除本批已记录的 BOOT／IMAGE／NORMAL 三个 QA，剩余 QA 为 0，相关通知已撤销。[历史审计](final-reminder-history-audit.json)确认五条 QA 提醒计划全部 inactive、cancelPending=0，提醒账本和其中两条 QA SENT 历史保留。[最终快照](final-after-cleanup.json)的总账本为 33 行／14 SENT，即原 28 行加 QA 5 行、原 12 SENT 加 QA 2 SENT，并非全账本数量不变。QA 删除后的空 plans 仅是按现存 QA 过滤的结果，账本保留结论来自历史审计。按升级前私有索引的精确 key 集核对，原 12 条 SENT 完整行保持，不删除发送账本或按日期排除原记录。

06:58:57.861995 UTC 的[最终审计](final-data-and-state-audit.json)17 项全部通过：原六张业务表全行一致（3 事项、1 来源、1 标签、1 关联、1 自定义字段、4 附件），四份原附件实际字节、全部业务设置及原 SENT 保持；记录的 11 项系统设置、通知权限／通道重要级别、精确访问、字体／显示、网络、电源／idle 与日志属性均与基线一致。合成夹具已卸载，临时 UI XML 和私有数据库临时目录无残留；实际安装包仍为上列 0.3.4／7，哈希及签名一致。

收尾没有 force-stop。最终普通首页已清除测试搜索，stopped=false、notLaunched=false、产品进程存在；实际下一精确 Alarm 为北京时间 11 月 3 日 08:00、window 0，与控制记录一致。初始 stopped=true 没有通过再次强停来“恢复”，有意保留应用正常可用。readyToDisconnect=true，可断开手机；未改时钟、未清空产品数据。完整 DA／TC、持续锁屏／自然 Doze、真实每日汇总点击及正式演练不由这 17 项收尾检查替代。

本段 23 份安全 JSON 已独立审阅并按[收尾清单](cleanup-files-hashes.json)原样归档；全批共 8 个阶段清单、119 份安全原文件，原始／归档 SHA-256 与字节数一致。

## 后续步骤

本批已收尾；继续调查 OBS-01／OBS-02、补齐完整场景，并单独核对远程新日历增量的构建与设备表现。

OBS-01、OBS-02 和历史 CI #45 未定根因继续保留。实际步骤、失败、用户协助及收尾证据由本批执行结果补充；[维护日志](../../maintenance-log.md)保留前序处理，[既有 0.3.3 设备结果](../RUN-20261101-DEVICE-ACCEPTANCE/README.md)独立保存。
