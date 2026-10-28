# RUN-20261028-001：第三轮荣耀 X50 实测

2026-10-28 开始。支盟渊表示手机 USB 已连接，要求接续第三轮设备验证。AI 助手执行测试及证据核对，必要的系统授权与人工操作另行记录实际参与者。

**本批已于 08:15:29 UTC（America/Los_Angeles 01:15:29）完成测试与收尾。** 同签名升级、三类提醒的指定样本、旧计划控制、真实单项点击及角色画面取得设备证据。本轮未确认新的产品缺陷，产品源码与 APK 未修改；未覆盖条件继续保留，不判定完整课程验收通过。详细执行见[测试记录第 13 节](../../test-records.md#13-run-20261028-001第三轮荣耀-x50-设备验证)。

本批次接替此前准备阶段预留、但未执行的 `RUN-20261027-006`。开发机结果仍见 [RUN-20261027-005](../RUN-20261027-005/README.md)，第二轮手机结果仍对应 v0.2.0，不视为第三轮通过。

## 被测基线与执行顺序

- 产品源码：`857ca5faf29af4c2b861bf4529634ec66e3e13bd`；目标 v0.3.0／versionCode 3／schema 3。
- 安装包：已经静态核对、与原版同签名的本机 APK，SHA-256 为 `12E9D11A03623630F99F4AF35449EB119B5226D866CC0E7E56A1A7A9339DBD05`。
- 连接确认后，先核对原版、原设置和匿名数据计数，再覆盖升级并复查迁移；不卸载或清空产品。
- 用 `QA-R3-` 前缀新建测试事项，核对三类提醒、同刻合并、变更失效、通知跳转及角色；记录实际前后台条件和时间差，不自行推定准时容忍值。
- 原有用户事项不作业务修改；收尾时恢复原业务设置并清理本轮数据。截图及导出仅保留审核后的 QA 范围。

## 起始状态

07:29:58 UTC（本地 00:29:58）首次 ADB 检查未发现已授权设备；Windows 能识别便携媒体接口，但尚未显示 Android／ADB 接口。已提示手机解锁、核对 USB 调试并切换上次可用的 PTP 模式，等待实际连接。此时没有安装或业务测试结果，不提前登记任何用例通过。

## 实际环境、参与和数据保留

荣耀 X50／HONOR ALI-AN00，Android 15／API 35，系统构建 ALI-AN00 9.0.0.230(CNC00E206R8P4)，与上轮已记录 MagicOS 9.0.0 的构建一致。07:32:41 UTC 连接可用，07:33:24—07:33:41 UTC 覆盖升级并核对 v0.3.0／schema 3，最终已装包仍为上表同一哈希。

支盟渊开启 USB 调试、完成通知和精确提醒的系统授权，并点按真实通知。AI 执行代理完成升级及初始权限请求后因选定模型容量限制结束，主线程接手手机，未改模型；另有只读代理审查判定点及素材，文档代理整理阶段结果。没有将辅助代理算作组员或虚构其他队友贡献。

原有事项及附件、标签关系、自定义字段行以同一份本地盐比较前后摘要；盐与原始用户数据库不导出，摘要只支持本次一致性核对。旧 A/C 的状态、修订及四个附件的字节数／SHA-256 在升级前、升级后和清理后相等。非旧 QA、非本轮 QA 的匿名事项数始终为 1。

## 通知与控制样本

初始通知未允许、精确能力未授予；用户授权后，应用显示两项均可用。临近提前量暂设 2 分钟，每日汇总设为北京时间 15:56。五条样本均通过真实界面录入，完成、删除及改期分别在旧 15:56 触发点前保存。

| 观察 | 北京时间计划 | 系统创建时间减计划 | 实际证据 |
| --- | --- | --- | --- |
| 前台 MERGE | 15:55 | +104 ms | 一条 tag，DANGER／DEADLINE 两个键均 SENT |
| 后台每日汇总 | 15:56 | +115 ms | 当日汇总记录；未导出其他事项正文 |
| 后台无速通 NEAR | 15:57 | +211 ms | 仅 DEADLINE，无 DANGER 计划 |
| 后台改期 EDIT | 15:59 | +139 ms | 新 16:01 截止计划 SENT；旧计划无发送尝试 |

07:55:04—08:00:09 UTC 共 58 次轮询，约每 5 秒一次，间隔包含命令耗时。07:55:30 UTC 按 Home 切后台。完成／删除／旧截止计划均没有被观察到对应通知，之后账本核对为 inactive、PENDING、尝试时间为空；四条定时通知在被观察存续期间各自的系统更新时间不变。

上述毫秒数来自手机系统创建字段，不是人眼首次看到或声音时间。手机整数秒时钟比较约比主机慢 1.5—2.6 秒；主机轮询时刻另存，不混作同一时间。采样不是连续跟踪，未约定通用容忍值，不据本次结果承诺所有条件下准点或严格恰好一次。

首个试录样本先用了默认 60 分钟提前量，其即时发送账本单列保存，随后删除并用新 ID 重建。准备阶段在触发前顺延计划，最终配置与时间表一致；未把已进入窗口的即时评估混入未来定时结果。

## 点击、角色与恢复

- 用户曾反馈 NEAR 进入详情，但独立检查时已切走；后续 EDIT 反馈其他页面或通知消失，不据此判定这两个落点通过。用户反馈点“今日 DDL”进入应用，具体列表落点及清筛选尚未独立核对。
- 另建 QA-R3-CLICK，截止 16:32、速通 30 分钟；它用于即时危险提醒和有效期较长的点击验证。08:09:15.720377 UTC 自动观察器确认用户点按后进入对应详情，未用 ADB 输入或显式 Intent 替代点击。该样本不计入前述四次定时差值。
- 原版认真／逾期、生气／危险、快乐／完成画面和署名弹窗已经逐张审阅。完成图只截到快乐卡片部分区域；没有改原用户事项以制造普通状态，也没有执行全局断网步骤。
- 用户明确要求后续改成更可爱的 Q 版，最终名字“蓝色大肥鱼”，见[候选方案](../../character-q-candidates.md)。本机当前仍为原版图片，本批不表示 Q 版替换通过。
- 送达与点击结束后才强停取账本，重开时 CLICK 仍未到期，已 SENT 危险原因没有重新出现。强停会清系统通知，零通知需结合账本判读，不单独当成产品取消能力证明。

## 收尾与剩余边界

通过 QA 前缀筛选，确认批删 4 个待办及 1 个已完成测试项；最终本轮事项为 0，事件计划均 inactive、无待取消，旧数据及附件比较仍一致。普通阈值 1440 分钟、KEEP、三开关开启、每日 08:00、提前 60 分钟均恢复。初始设置表无记录，收尾正常保存默认行，恢复的是有效行为；通知与精确权限保留用户授权。

最终正常首页查询为空，手机和本机临时 UI XML、临时数据库目录已清理，已通知支盟渊可以拔线。未安装额外测试应用，未改系统时钟、网络或省电设置。

锁屏、Doze／省电、重启、完整权限拒绝／撤销／重授、冷点击、汇总具体落点和失效目标等仍待后续验证。已有 116 方法／132 次自动执行结果继续引用原批次，不将手机操作加进自动次数；本批没有新增完整业务用例整例通过声明。OBS-01 仍未定位。

## 证据文件

| 文件 | 用途 |
| --- | --- |
| [summary.json](summary.json) | 派生批次摘要、APK／源码与清理结果 |
| [device-and-install-before.json](device-and-install-before.json)、[device-and-install-after.json](device-and-install-after.json) | 原版及覆盖升级后的版本、系统和安装包哈希 |
| [before-upgrade.json](before-upgrade.json)、[after-upgrade.json](after-upgrade.json)、[final-state.json](final-state.json) | 原数据、四附件及设置比较；仅 QA 内容和匿名摘要 |
| [setup-default-lead.json](setup-default-lead.json) | 初始默认提前量样本的独立账本 |
| [actual-plan.json](actual-plan.json)、[prepared-events.json](prepared-events.json) | 最终未来时间表、实际录入及计划状态 |
| [notification-observations.json](notification-observations.json)、[timed-polls.json](timed-polls.json) | 包过滤原始元数据观察与前后台标记 |
| [notification-summary.json](notification-summary.json)、[clock-comparison.json](clock-comparison.json) | 派生通知差值及独立时钟边界 |
| [after-timing-before-cleanup.json](after-timing-before-cleanup.json) | 发送及失效账本、CLICK 原因和原数据保留核对 |
| [steps.json](steps.json)、[completion.json](completion.json) | 实际步骤、收尾时刻及临时文件清理确认 |
| [evidence-manifest.json](evidence-manifest.json) | 23 个白名单证据的原始字节数与 SHA-256；不含自身、README 和属性文件 |

七张 PNG 均与本机原件字节一致、未经编辑并经主线程逐张审阅：

- [01-permissions-before.png](01-permissions-before.png)：通知不可用、近似能力提示。
- [02-permissions-allowed.png](02-permissions-allowed.png)：两类授权后的可用状态。
- [04-completion-feedback.png](04-completion-feedback.png)：完成状态和部分快乐反馈。
- [05-role-overdue.png](05-role-overdue.png)：QA 搜索范围内的全局逾期反馈。
- [06-role-danger.png](06-role-danger.png)：危险计数及生气表情。
- [07-real-notification-detail.png](07-real-notification-detail.png)：真实点击后的 CLICK 详情。
- [08-character-credits.png](08-character-credits.png)：非官方关系、作者链和许可弹窗。

测试工具的通知帮助查询在荣耀系统返回普通状态，内容未保存或导出；后续仅使用源端包过滤并校验过滤头及所有包标记，保留正常脱敏。[AOSP 对应过滤实现](https://android.googlesource.com/platform/frameworks/base/+/8b1e072210796e9772d3d07bdbae424b38447db6/services/core/java/com/android/server/notification/NotificationManagerService.java)用于核对参数含义。原始数据库、UI XML、盐、设备序列号、无关应用内容和未审阅诊断不入库。
