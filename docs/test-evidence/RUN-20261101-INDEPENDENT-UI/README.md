# RUN-20261101-INDEPENDENT-UI

2026-11-01 本机回归，基线 `ac1d9785ac9ab74dbd34fc00bc8459eb923002bc` 加两份未提交新增测试。生产代码、依赖和 schema 未改；本批不登记为某位队友的个人新增贡献，也不属于远程 CI。

- 定向：两个新测试类共 5 次执行通过。
- 全量：Debug 构建、测试与 Lint 成功；25 份 JUnit XML、155 个不同方法／174 次执行，失败／错误／跳过各 0；Lint 0 错误／16 警告。
- 5 项定向测试已包含在全量 174 次中。没有失败重试、手机安装或实机验收，OBS-01 仍未定位。

[summary.json](summary.json)记录实际命令、版本、源码哈希、逐套件计数、计数规则、日志哈希、Lint 分组及 APK 标识。[测试报告](../../test-report.md#15-独立本机界面补测2026-11-01)说明 5 项页面流程及剩余边界，[执行记录](../../test-records.md#18-run-20261101-independent-ui独立本机界面回归)保留执行过程。

## 报告副本与脱敏

[targeted-results](targeted-results/)为定向运行的 2 份 JUnit，[full-results](full-results/)为全量运行的 25 份 JUnit 与 Lint XML。它们是**脱敏副本**：移除 JUnit `hostname` 属性，将绝对工作区路径替换为 `[workspace]`；测试节点、结果、时间与诊断输出保持。没有删除 `system-out`／`system-err`。

原始与副本 SHA-256 分别见 [targeted-report-hashes.json](targeted-report-hashes.json)、[full-report-hashes.json](full-report-hashes.json)。本机原始日志与报告位于被 Git 忽略的 `.local-tools/independent-qa/`，未将机器绝对路径或工具缓存加入仓库。

新增源码未提交时，以摘要中的两份文件哈希识别被测状态，不能仅用基线提交号表示新增测试已包含在远程。此前远程 CI 的 169 次执行不覆盖这两份新增测试；本批本机 16 条 Lint 警告也不替换历史 CI 的 17 条。
