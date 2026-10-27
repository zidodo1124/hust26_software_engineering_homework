# RUN-20261027-001 首轮验证证据

- 对应源码：`1655a70832f91bf79af157ab4c14d0edd3e23bb1`。
- 最终核对：2026-10-27 03:39:39，America/Los_Angeles（UTC-07:00）。
- 执行环境：Windows 11、JDK 17、Robolectric API 28／35；未连接手机。
- 结论：Debug APK 构建成功，30 项本地自动测试通过，Lint 0 错误、10 条依赖更新提示；不是实机验收结论。

| 文件 | 含义 |
| --- | --- |
| [summary.json](summary.json) | 环境、提交、最终测试数量、Lint 统计与 APK 哈希 |
| [attempt-01-compile-failed.txt](attempt-01-compile-failed.txt) | 首次编译失败，后续修复可空保存状态 |
| [attempt-02-runtime-download-blocked.txt](attempt-02-runtime-download-blocked.txt) | 运行库下载连接失败；为中止重复超时，停止该测试 JVM，不能将进程断开解读为 App 崩溃 |
| [attempt-03-ui-failed.txt](attempt-03-ui-failed.txt)及 [attempt-03-results](attempt-03-results/) | 28 通过、2 失败的原始检查与 JUnit 结果 |
| [diagnosis-ui-overlap.txt](diagnosis-ui-overlap.txt) | 提示条与按钮重叠、点击未触发保存的诊断摘录 |
| [attempt-04-passed.txt](attempt-04-passed.txt)及 [final-results](final-results/) | 修复后完整构建与 30 项测试通过的原始日志及 JUnit XML |
| [lint-final.xml](lint-final.xml) | 最终静态检查结果，保留依赖更新提示 |
| [apk-inspection.txt](apk-inspection.txt) | APK 元数据及签名工具检查，不是安装记录 |
| [ci-verification.json](ci-verification.json) | 同源码在 GitHub Actions 成功、JUnit 数量及产物的独立复核元数据 |

文本证据由实际运行输出复制，工作区绝对路径统一替换为 `<workspace>`；测试结果、异常和时间未改写。诊断文件为明确标注的摘录，其余日志与 XML 保留失败和复测事实。工具缓存、完整本机路径配置、生成的 APK 及 HTML 报告不纳入 Git；APK 的本机位置和 GitHub Actions 下载方法见[开发说明](../../development.md)。

批次解释、问题处理与完整业务用例的对应关系见[测试记录](../../test-records.md)，结论范围见[测试报告](../../test-report.md)。
