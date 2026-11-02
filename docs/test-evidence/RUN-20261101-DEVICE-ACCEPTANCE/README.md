# RUN-20261101-DEVICE-ACCEPTANCE

**本批操作及收尾已完成，状态为 completed_with_open_findings；不是全部验收通过。** OBS-01 保持开放；OBS-02 重启自动恢复子项未通过，后续优先调查。 用户在 America/Los_Angeles 的 2026-11-01 连接手机并授权实机验收，随后确认安装。设备业务时间已为北京时间 2026-11-02；下列原始观察保留 UTC 时间，批次名称沿用用户日期，不改动手机时钟。

设备为荣耀 X50／HONOR ALI-AN00，Android 15／API 35、MagicOS 9.0.0，构建 `ALI-AN00 9.0.0.230(CNC00E206R8P4)`。被测软件仍为 0.3.3／versionCode 6、Room schema 4，包含远程 `ac1d978` 后尚未提交的本机增量。7 份源码哈希和 APK 标识见 [summary.json](summary.json)，此前本机 194 次执行及首次失败证据仍属于[独立本机批次](../RUN-20261101-RECOVERY-DIAGNOSTICS/README.md)，不与本次手机操作累加。

## 已完成的安装与首启分项

| 分项 | 实际证据与边界 |
| --- | --- |
| 同签名覆盖 | 02:21:25 UTC，`adb install -r` 成功；实际安装 APK SHA-256 为 `6f30078bf0dfa95731afe9fae0c08f06dd19d048eb815fd2236022c90e19e65b`，证书 SHA-256 为 `978833bcaf0fc001248f5d8501bfa1f9d1273922474ed601f2c60a4548a652b9`，与本机原安装包相同；未卸载产品或清库 |
| 启动前数据 | [安装对比](install-comparison.json)确认六张业务表全部行、4 份附件实际字节、设置、提醒全表／SENT 子集及控制记录保持；原数据为 3 事项、1 来源、1 标签、1 关联、1 自定义字段、4 附件 |
| 首次启动 | 正常启动并进入[设置页面](01-upgraded-settings.png)；[首启对比](launch-comparison.json)确认原业务行、附件与设置保持。提醒计划由 18→19 行、SENT 由 8→9，本日 DAILY 由未发送状态变为 SENT；不能将首启后的提醒账本写成全行保持 |

旧安装包 SHA-256 为 `7b8f8d6872fcc1526dd5e64e9ac47a8f989be71cd4cac67230fa989409e3ecc8`，与新包版本号相同而内容不同，必须使用实际哈希识别。本轮安装前通知已授予、精确提醒 app-op 为 allow、提醒通道 importance 3；USB 充电、普通省电关闭、idle 为 ACTIVE，原设置见[设备基线](device-before.json)及[业务基线](before-upgrade.json)。

## 每日汇总的执行限制

首启脚本为取得稳定数据库快照执行了强停。强停可能清除活跃通知，而该动作前没有保留通知观察；因此之后“没有活跃通知”不能倒推出此前未产生通知。生产逻辑也会将无今日有效事项的空汇总标为 SENT，账本 SENT 本身不等于用户已看到通知。

创建任何本轮样本前，页面显示[今日待办为 0](original-today-count.json)，与空汇总处理逻辑一致；这仍不是首启通知状态的直接观察。

本日 DAILY 每天只处理一次。**真实每日汇总冷启动点击尚未完成**，不删除发送账本、不改系统时钟来制造第二条，也不以 `am start` 路由调用替代真实通知点击。后续结果与未测边界继续按实际记录补充。

## 单项通知的已观察事实

通过真实页面创建 `QA-ACCEPT-1101-LOCK`，截止为北京时间 12:31、速通 120 分钟，危险提醒计划为 10:31。实际数据库计划与输入相符，见[计划](qa-lock-plan.json)和[创建后 QA 快照](lock-created.json)。创建脚本曾因页面日期不补零而触发格式断言，随后核对持久日期一致，没有重复创建样本；这不是生产日期保存缺陷。

[定时观察](lock-timed-observations.json)中的系统通知创建时间为北京时间 10:31:00.267，比计划晚 267 ms；首次保留通知观察为 02:31:04.300249 UTC。系统创建时刻、主机观察时刻和用户实际阅读时刻不是同一口径。最后一条相邻设备状态显示 Keyguard 可见、屏幕关闭、Power `Dozing`，但 deviceidle 仍为 `ACTIVE` 且 USB 充电；前面的观察还存在 `Awake` 段。因此这里只记录通知出现及当次状态，不声明连续锁屏全程或 Doze 通过，也未据此验证声振。

随后使用后台 `am kill`，确认进程已消失而同一通知仍保留，见[冷点击前置](lock-before-cold-click.json)。用户报告点击通知并进入对应事项后，执行器独立核对产品前台、样本标题、12:31 截止与 2 小时速通，并由主线程审阅[实际详情截图](03-lock-cold-notification-detail.png)，见[点击结果](lock-cold-click-result.json)。本样本的**真实单项通知冷启动路由子项通过**，没有用构造 Intent 代替点击；不等同于每日汇总路由或所有冷启动条件通过。

之后经页面标记该样本完成，[QA 快照](lock-after-complete.json)确认两条计划 inactive 且 `cancelPending=0`。通知已在此前点击时自动取消，不能将此后的通知消失归因于完成操作；这一步只支持已观察的计划失效。

## 通知总开关恢复

使用独立 `QA-ACCEPT-1101-STATE` 未来样本，经真实系统页面关闭应用通知后返回产品：[关闭时证据](05-notification-off.json)显示 `POST_NOTIFICATIONS` 未授予、应用 importance 为 NONE，而提醒通道仍为 3；两条业务计划保持 active／PENDING，控制记录触发时刻为空，系统中本 App 提醒 Alarm 为空。设置页面显示不可通知及“计划保留、暂停唤醒”的[对应文案](05-notification-off.png)。

重新开启并返回产品，[恢复证据](06-notification-restored.json)显示授权恢复、importance DEFAULT，业务计划未变，11:33 的精确系统 Alarm（window 0）重建，[页面](06-notification-restored.png)恢复“已允许”。该总开关关闭／恢复子项通过；不扩展为所有拒绝路径或不返回 App 时的行为。通道单独关闭的结果见下一节，精确访问另行验证，不用这一步替代。

## 通道开关、首次恢复差异及后续核对

单独关闭“DDL 提醒”通道后，[07 记录](07-channel-off.json)显示应用通知权限仍授予、应用 importance DEFAULT、通道 importance 0；业务计划保留，系统提醒 Alarm 为空。重新开启后，[08 原结果](08-channel-restored.json)显示产品恢复可通知并重建 11:33 Alarm，但通道 importance 为 2，未恢复基线 3，恢复断言未通过。

首次重新开启时只支持应用业务恢复，未通过基线 importance 断言，该中间结果继续保留。进入／加载荣耀自有通道页面期间，后续采样的 importance 已为 3；确切触发点未隔离，不能把恢复归因于其后静默 false→true→false 序列，因为序列开始前已是 3。该序列前后完整通道行哈希相同、静默最终为 false，见[08b 核对](08b-channel-oem-restore.json)。

再次返回产品，[08c 结果](08c-channel-baseline-restored.json)确认原记录的权限、importance 3 和 11:33 精确 Alarm 恢复，QA 计划未变；通道开关的业务响应及已记录的重要级别恢复子项通过。这里没有修改生产代码，也不将 OEM 中间状态归为产品缺陷。完整通道行哈希仅比较 08b 序列前后；最初基线未保存完整声音／振动属性，不能声称本轮前后所有通道字段完全一致。

## 精确访问与强停后重开

在真实系统页面撤销精确提醒访问并返回 App，[09 记录](09-exact-access-off.json)显示通知仍允许、精确 app-op 为 deny，页面改为[“近似提醒，可能延迟”](09-exact-access-off.png)；同一 QA 业务计划保持，系统 Alarm 使用近似窗口 `+31m29s945ms`。恢复权限后，[10 记录](10-exact-access-restored.json)及[页面](10-exact-access-restored.png)回到精确模式，窗口为 0。权限与调度模式切换子项通过；该近似窗口不是已测到的送达延迟，本次没有等待近似 Alarm 实际送达。

随后强停产品，再主动打开。[强停记录](state-force-stop-result.json)和[重开核对](11-state-reopened.json)确认 QA 计划保持、同一 11:33 精确 Alarm 重建。该结果支持未来计划在重开后恢复调度，不承诺强停期间发送，也不替代设备重启或厂商长期后台限制。

## 图片阶段前置与执行器修正

本轮已实际安装独立[剪贴板夹具](clipboard-fixture-install.json)，包名 `cn.edu.hust.ddlsaver.qa.clipboard`，本机开始时未安装。选定 APK SHA-256 为 `fed09852fed3b1b114baffbeb0166aa122fe9849ada71c5c87daa11bdaa822c2`，与预审的合成 Provider 构建一致；夹具不等于真实相册普遍兼容；本轮安装已在图片步骤结束后卸载，见下文。

首次从夹具返回时，执行器使用裸 `am start` 新建了另一 MainActivity 并出现新首页。一次真实 Back 找回原编辑器，原 QA 标题／描述保留，未保存重复事项、当时未完成导入；[QA 快照](image-draft-navigation-check.json)保留该阶段的持久状态。助手已改为 Back 返回原任务。这是执行器导航修正，不登记为产品丢草稿；图片导入结果继续按随后实际证据填写。

## 两次独立图片粘贴

通过夹具分别复制 blue 和 orange，在原编辑器两次主动点击“粘贴图片”，草稿附件数量 0→1→2，见[两图草稿](12-image-two-incremental-draft.png)。保存后的[QA 数据与文件核对](image-two-saved.json)确认两张私有副本的字节数、SHA-256 与对应合成源相同，位置为 0／1，原合成描述保持；本样本的连续增量粘贴与保存子项通过。

源文件名带历史 `QA1027` 前缀，是同一组可复用合成资源，不是本轮日期或私人相册来源。旧 URI 的强停复现和后续失败恢复另行记录，不根据本次成功预填其根因。

## OBS-01 本轮复现与诊断限制

保存两图后强停产品并正常重开，复用此前成功粘贴留下的 orange ClipData，没有重新复制。再次粘贴失败并显示[实际通用错误](13-old-uri-paste-result.png)：“图片导入失败，请确认图片可读取且本机存储空间充足。”[本次记录](image-old-uri-probe.json)与[失败后 QA 快照](image-after-old-uri-probe.json)确认原两附件及已保存描述保持，新输入的合成草稿描述仍在，私有文件 6→6，没有新增或删除文件。

旧 URI 失败现象在本轮复现。该首次采集时实际包为 debuggable，但指定 `DDLSaver.Attachment` 结果为 0 行；这一轮不能填写失败 stage／category，也不能仅据该轮宣称诊断已得到设备验证，后续单 tag 条件下的结果见下节。OBS-01 的根因仍未定位；若另作 shell 日志探针，它只检查采集条件，不算 App 失败诊断证据。首轮重新复制后恢复并保存 3 张，原图保持；损坏图片被拒绝且草稿保持 3 张，但指定 tag 同样零行，见[首轮重试保存](image-retry-saved.json)和[受控失败](image-controlled-broken-probe.json)。其间前台离开产品，输入保护暂停了操作；恢复后继续保存，没有将这次执行中断记为产品故障。

## 日志阈值核对、实际 App 诊断与恢复

[只读采集检查](attachment-log-pipeline-probe.json)发现 `persist.log.tag=S`，本 tag 与非持久全局 tag 值为空；同 tag 的 shell 探针退出 0，但过滤结果也为 0 行。该探针不是 App 导入事件，先前零行证据继续保留。

随后只临时设置非持久的 `log.tag.DDLSaver.Attachment=W`，没有改变全局持久静默。在这个明确条件下，先确认 fresh orange 可以导入，再取消该临时草稿、强停并重开产品，复用未重新复制的同一 orange ClipData。实际 App [失败记录](image-old-uri-diagnostic.json)为 `stage=open_source`、`scheme=content`、`item=1`、`count=1`、`category=security_denied`、`rollbackFailures=0`；已保存 3 张，失败后草稿仍 3 张，私有文件 7→7 且集合不变。

再次 fresh copy 后恢复到 4 张；合成 broken 图片被拒绝，App [受控失败诊断](image-broken-diagnostic.json)为 `decode_image／validation／rollbackFailures=0`，草稿仍 4 张、私有文件保持。最终[保存快照](image-final-saved.json)确认 4 张实际字节数与 SHA-256 均匹配合成源，原 3 张保持，合成草稿描述成功保存。[执行结果](image-diagnostic-retry-result.json)与前面的 shell 探针分别归档，新增诊断已在本设备的单 tag W 条件下观察到真实 App 事件。

[finally 恢复记录](image-tag-override.json)确认 tag 恢复原空值，全局 `persist.log.tag` 仍为 S。当前证据将此次旧 URI 失败缩小到**打开来源时权限被拒绝**；最终授权失效机制、其他来源及其他系统行为仍未确定，不称为 Android 系统缺陷，**OBS-01 保持开放**。下次采集须先检查实际阈值，不能由“零行”直接判定没有失败或 App 诊断失效。

## 移除图片来源后保持

03:19:37 UTC [卸载记录](clipboard-fixture-removal.json)确认只移除本轮新增夹具，工具包已不存在，事先清空合成剪贴板，没有卸载产品。正常重开产品后，[之前](image-before-fixture-removal.json)／[之后](image-after-fixture-removal.json)的整条 IMAGE 数据（含 4 张附件元数据、实际字节数及 SHA-256、合成描述）完全相同；蓝／橙两类私有图均实际打开预览，见[结果](image-final-result.json)。预览截图已经审阅，但包含非 QA 系统覆盖，原图不归档。随后经页面完成 IMAGE，[计划核对](image-completed.json)确认相关计划 inactive、cancelPending 0。

该子项支持本次合成来源卸载后私有副本仍可用，不扩展为所有第三方来源兼容。图片夹具已清理不等于整个批次的 QA、原业务与系统状态收尾已完成。

## 受控 idle 的实际送达

页面创建 `QA-ACCEPT-1101-IDLE`，截止北京时间 13:32、速通 120 分钟，危险计划为 11:32，见[输入计划](qa-idle-plan.json)及[系统精确 Alarm 基线](idle-before.json)。本次使用系统测试命令强制 deep idle，并模拟拔除供电；物理 USB 连接仍在。这是受控 idle，不是自然进入 Doze 或极限省电。

[实际结果](idle-result.json)保留 33 次观察：deviceidle `mState=IDLE`、`mForceIdle=true` 均持续，但中间有 `Awake`／屏幕开启段，所以不声明连续熄屏或持续凭据锁屏。系统通知创建于北京时间 11:32:00.171，相对计划 +171 ms；首次保留通知观察为 03:32:06.288298 UTC，相邻状态为 Power Dozing、Keyguard 可见、屏幕关闭、强制 IDLE、模拟未充电。该受控条件下真实提醒送达子项通过，不把系统创建时刻当作用户阅读时刻。

finally 已恢复真实 USB 供电、正常电池更新及非强制状态；最初恢复采样存在系统状态过渡，后续[03:32:21 核对](idle-restoration-settled.json)明确 `ACTIVE`／`mCharging=true`／`mForceIdle=false`、`updatesStopped=false`。实际电量随时间变化，没有强制还原百分比。用户[确认只解锁](idle-user-unlock-confirmation.json)，未点击通知或打开产品；[普通入口启动前](idle-notification-before-normal-launch.json)及[详情完成前](idle-notification-before-complete.json)均仍有同一通知。随后通过页面标记完成，[结果](idle-completion-result.json)确认通知 1→0，相关计划 inactive、cancelPending 0。这个 IDLE 样本支持真实已发通知随完成撤回，与 LOCK 点击时自动取消的边界分别记录。

## OBS-02：真实重启后未自动恢复，主动打开后恢复

独立 `QA-ACCEPT-1101-BOOT` 截止北京时间 13:44、速通 120 分钟，危险计划为 11:44。重启前已退出受控 idle，供电正常、通知允许、精确访问允许、通道 importance 3，系统已有 11:44 精确 Alarm，见[计划](qa-boot-plan.json)及[重启前状态](boot-before-reboot.json)。执行真实重启，没有合成 BOOT 广播。

首轮 240 秒内未观察到 ADB 重新连接，[超时原记录](boot-reconnect-timeout.json)保留；这段连接缺口不等于手机启动失败或产品故障。用户[确认仅首解锁](boot-user-unlock-confirmation.json)，未开 App／点通知后，03:40:45 UTC 恢复观察，新 boot ID 哈希不同、uptime 262 秒，确认真实重启；App 非前台，11:44 的业务计划仍 active／PENDING，但系统 Alarm 为空，见[重连](boot-after-reconnect.json)与[解锁后](boot-after-unlock.json)。

[未主动打开窗口](boot-result.json)含 27 次观察，03:40:55.679—03:45:37.480 UTC，跨过预定 11:44 北京时间：产品一直不在前台，每次系统 Alarm 及本样本通知均为空。[首次普通打开前](boot-before-first-explicit-launch.json)再核对通知权限 true、精确 app-op allow、通道 3，包 `stopped=false`／`notLaunched=false`，进程不存在，危险计划仍 PENDING。**本设备／配置下，该窗口的重启自动恢复子项未通过，登记 OBS-02 并保持开放。**

随后首次普通 launcher 打开，原危险计划变为 SENT，实际通知创建于北京时间 11:46:12.629，下一条 12:44 的 DEADLINE 精确 Alarm（window 0）重建，见[对照结果](boot-explicit-launch-comparison.json)。主动打开后补发／恢复成功，不抵销前面的自动恢复结果，也不代表重启前后所有发送条件通过。

下一重点是定位重启恢复链路：区分系统广播是否交付、接收器与恢复任务是否执行、系统调度及厂商设置影响；当前证据尚未隔离原因，不能直接归因 OEM 限制或产品代码。此前本机广播集成通过不替代这次设备结果。窗口前的 ADB 缺口、后续未继续等待的时段均不推定行为。

## 最终清理、原数据与状态复核

仅通过确认页面删除本轮 5 个 QA 事项，未删除提醒历史，见[清理结果](final-cleanup-result.json)。强停取最终快照之前已独立观察[产品活跃通知为 0](final-notifications-before-force-stop.json)，不把强停清通知误当作业务撤回。

[最终停机核对](final-data-and-state-audit.json)的 **16 项检查全部通过**：原六张业务表全部行的带盐摘要、原 4 份附件实际字节／引用、原业务设置与升级前完全一致；本轮 QA 为 0，仅原 4 个私有附件文件保留。原有 8 条 SENT 行摘要保持；本日正常新增 1 条 DAILY SENT、本轮 3 条 QA SENT 及 9 条 inactive QA 计划历史合法保留，无待取消或提醒错误。因此不声称整个提醒表与基线物理相同。通知权限、精确访问、通道已记录 importance、网络、字体／屏幕与省电等记录值恢复；完整通道声振基线的限制仍保留。

[初次收尾检查](final-data-and-state-audit-initial.json)中，唯一未满足项是日志属性：真实重启后 `persist.log.tag` 被观察为 M，而本轮初始及图片诊断结束时为 S，原因未确定。经授权只为恢复基线进行一次 M→S 写入并复读成功，见[独立恢复记录](global-log-baseline-restoration.json)。这与图片诊断期间只临时设置单 tag W 分开记录；不能宣称整个批次全局属性从未变化。初次不通过结果保留，最终所有日志属性回到记录原值。

03:51:58.290032 UTC 完成最后普通启动：手机回到正常首页、QA 搜索清空，临时 UI XML 和本机临时数据库副本目录已移除；本轮新增工具已卸载。正常提醒调度为北京时间 2026-11-03 08:00、exact/window 0，见[正常启动 QA 核对](final-normal-launch-qa.json)与最终 audit，`readyToDisconnect=true`。该清理通过不关闭 OBS-01／OBS-02，也不替代剩余业务和课程整体验收。

## 剩余范围与归档

连续锁屏、自然 Doze／省电与真实每日汇总点击仍待补测；本轮收尾已完成；重启分项已执行但自动恢复未通过，OBS-02 待调查；受控 idle 已实际送达并恢复系统测试状态；图片重试与受控失败已有证据，OBS-01 的最终授权失效机制仍未确定。执行步骤见[验收安排](../../demo-and-acceptance.md)。OBS-01 不因诊断代码已安装自动关闭，最终课程验收也未完成。

本目录初始收录 8 份已检查的安全 JSON 与 1 张经主线程审阅的设置截图，原始与归档 SHA-256 见 [initial-files-hashes.json](initial-files-hashes.json)。业务数据仅保存计数和本批带盐摘要，盐保留在忽略目录，不包含业务明文、原始数据库、设备序列号、完整通知 dump 或未审阅截图。后续仅加入实际生成且复核过的本批证据。

后续提醒过程的安全 JSON 与原始／归档哈希见 [reminder-files-hashes.json](reminder-files-hashes.json)。活跃进程 QA 快照仅用于本轮计划观察，不代替停机边界上的原数据保留核验。

能力与权限过程文件及哈希见 [capability-files-hashes.json](capability-files-hashes.json)。产品页面截图只证明其当前显示，与系统状态／真实计划／Alarm 元数据配合判读。

图片阶段安全文件及哈希见 [image-files-hashes.json](image-files-hashes.json)，当前安装状态以本轮实际记录为准。

受控 idle 原始安全元数据及哈希见 [idle-files-hashes.json](idle-files-hashes.json)。

真实重启与主动打开对照的安全元数据及哈希见 [boot-files-hashes.json](boot-files-hashes.json)。

最终清理、初次检查差异及恢复后的安全元数据哈希见 [cleanup-files-hashes.json](cleanup-files-hashes.json)。
