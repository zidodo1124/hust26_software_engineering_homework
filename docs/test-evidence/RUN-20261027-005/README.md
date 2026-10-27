# RUN-20261027-005：第三轮提醒与角色本机验证

2026-10-27 建立；小组花园鳗。支盟渊授权继续第三轮，实现通知提醒、角色表情和台词，并同步测试、文档、交互记录与 Git。AI 助手负责实现、执行检查和证据整理；未登记成员领取、用户审核或验收通过。

**本机结论：attempt-06 全量通过。** Gradle 3 分 34 秒、退出 0，Debug 构建及 Lint 完成；17 组真实 JUnit XML 共 116 个不同方法、132 次执行，失败／错误／跳过均 0。Lint 为 0 错误、16 条提示，保留原先失败、诊断和复测记录。

| 标识 | 实际结果 |
| --- | --- |
| 版本 | 0.3.0／versionCode 3，Room schema 3，minSdk 26、targetSdk 35 |
| 被测源码 | [857ca5f](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/857ca5faf29af4c2b861bf4529634ec66e3e13bd)；仅最终 attempt-06 对应该冻结源码，早期尝试为历史工作树 |
| APK 字节数 | 12,590,695 |
| APK SHA-256 | `12E9D11A03623630F99F4AF35449EB119B5226D866CC0E7E56A1A7A9339DBD05` |
| 静态核验 | 无 INTERNET 权限；签名证书与第二轮相同；APK 内七份上游原文件哈希与资源清单一致 |
| 用户交付选择 | “这轮先交付，手机测试下次继续”；第三轮未进行 ADB 或新 APK 安装 |

基线为第二轮实机记录 `114ea44`，启动计划为 `7278d0c`。完整尝试及 BUG-R3-001—003 的修复见[执行记录第 12 节](../../test-records.md#12-run-20261027-005第三轮提醒与角色本机验证)。

## 本批次范围

| 检查方向 | 本机已检查范围 | 用例关联 |
| --- | --- | --- |
| 迁移与持久化 | 从真实 v2 schema 升级，保留事项、分类、字段、图片引用、完成／归档和原设置；新增三类提醒设置默认值正确 | TC-01、TC-19、TC-30 |
| 计划与发送规则 | 北京时间下一次每日时刻、空汇总、今日逾期范围、危险／临近触发点、立即评估、重复回调及同刻合并 | TC-19—TC-25 |
| 状态与失败 | 保存及计划同事务、完成／归档／删除后旧回调失效、取消失败保留快照、权限受限、系统提交与数据库之间的未确定窗口 | TC-23—TC-27、TC-30 |
| 页面与入口 | 三类开关、时间及提前量校验；权限与精确能力分开显示；通知点击有效详情／今日列表及失效目标处理 | TC-19、TC-26、TC-28 |
| 角色与资源 | 四表情原字节／许可核对、离线解码、台词状态规则和生产反馈计时；文字回退及署名代码已审阅，手机显示待验 | TC-08、TC-29 |
| 原流程回归 | 构建 Debug APK、重跑已有规则／Room／图片／Compose 检查及 Android Lint，记录真实错误和维护提示 | TC-01—TC-18 的已实现子场景 |

中央检查采用仓库脚本执行 `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain --continue`。失败、诊断、实际修复与复测按尝试保留；同一方法在不同 API 或环境运行分别记执行次数，不据此增加不同方法数，也不等同于完整业务用例通过。

## 用户交付选择与实机边界

荣耀 X50 已在前批次记录为 HONOR ALI-AN00、Android 15／API 35、MagicOS 9.0.0。支盟渊询问是否仍需手机时，AI 助手答复上一轮已结束、可以拔线，后续通知与角色验证时提前通知连接；第三轮授权也明确要求需要手机时告知。

本批次是开发机验证，不复用 RUN-004 作为 v0.3.0 通知实测。支盟渊明确选择本轮先交付、设备下次继续，故本轮不等待连接也不安装。下次实际连接时再建立设备批次（拟编号 RUN-20261027-006，当前未建立或执行），记录实际权限、精确或近似模式、计划与显示时间、前台／后台／锁屏／重启／强停后重开等独立条件。未确认可接受时间偏差时保留观察，不登记准时通过。

## 证据索引

| 文件 | 对应范围与边界 |
| --- | --- |
| [summary.json](summary.json) | 最终源码、APK、116 方法／132 次执行、Lint、六次尝试及用户设备选择；仅 attempt-06 对应已提交源码 |
| [apk-verification.json](apk-verification.json) | 版本、签名证书、min／target、无 INTERNET 及七份原素材的实际包内核验 |
| [final-results](final-results/) | 最终 17 组原始 JUnit XML，全部通过；不与历史或定向结果累计方法数 |
| [final-lint.xml](final-lint.xml) | 最终本机 0 错误／16 提示的原始 Lint 报告 |
| [ci-verification.json](ci-verification.json) | 源码 857ca5f 的 CI completed／success；17 组 XML 的 132 次执行通过，Lint 0 错误／17 提示，额外一条 OldTargetApi |
| [evidence-manifest.json](evidence-manifest.json) | 本目录证据的原始字节数和 SHA-256；不包含自身、README 或属性文件 |
| [attempt-logs.zip](attempt-logs.zip)、[log-index.json](log-index.json) | 六次检查的原始日志和每件原名／字节数／SHA-256。下载解压即可得到原日志，不改变原始尾空格或换行 |
| [attempt-02 定向 XML](attempt-02-targeted-results/) | 三个重建方法定向通过；早期工作树，不是最终提交全量结果 |
| [attempt-03 失败类 XML](attempt-03-full-results/TEST-cn.edu.hust.ddlsaver.ui.SaverAppFlowTest.xml) | 只导出含两条失败的 UI 类；全轮 125 次统计见摘要，不把该选取类当作全轮 XML |
| [attempt-04 定向 XML](attempt-04-targeted-results/) | 9 次中反馈计时一项失败；其余平台／冷启动／设置入口通过 |
| [attempt-05 单反馈 XML](attempt-05-feedback-results/) | 修正异步观察时间基准后单方法通过 |
| [diagnostic-summary.json](diagnostic-summary.json) | 已核实循环调用链、1.6.1 字节码比较、受控终止和修复的派生摘要；不是原始全文 |

六个原日志各有一处原生尾空格，采用固定 basename 的 ZIP 封装，既保留每件原始字节，又维持代码及普通文本尾空格检查。归档不包含 .local-tools 其他文件；索引标明每次范围，早期尝试不绑定最终源码。完整 jcmd 线程转储和字节码全文留在本机，摘要保留它们的 SHA-256 供复核，没有把摘要伪装为原始记录。

所有已批准 JSON／XML 以原始字节导出；局部 `.gitattributes` 保持跨平台字节稳定并识别 CRLF。证据 SHA 清单已在 CI 状态核对后生成。本目录不收录私人手机资料、全系统日志、凭据或未审阅图片。

[测试执行记录](../../test-records.md)与[测试报告](../../test-report.md)保留跨批次结论；前轮 [RUN-004](../RUN-20261027-004/README.md) 的 v0.2.0 设备事实保持可追溯。


## 同源码 CI 结果

[CI 40530551146](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40530551146) 已核对为 completed／success；实际下载报告解析 17 组 XML，声明与实际均 132 次执行，失败／错误／跳过均 0。CI Lint 0 错误／17 提示，比本机 16 条多一条 OldTargetApi；两边分别保留实际结果，不累计新增方法。 只对应同一 App 源码，不增加不同方法数；本机 APK 与 CI 打包产物分别识别。
