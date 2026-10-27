# 第二轮测试证据

- 批次：RUN-20261027-002；应用 0.2.0／2，schema 2。
- 被测提交：[6255f3b](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/6255f3bd3c36e2a605f963eb7a8bae9a6ad19842)。
- 最终结果：[summary.json](summary.json)；74 个方法、87 次配置执行全部通过。
- `attempt-01.txt` 与线程快照：首次迁移失败和诊断等待，进程手动停止；没有用残留 XML 冒充本次报告。
- `attempt-02.txt`：完整 74 次执行及 5 条 UI 失败；`attempt-02-results/` 保留其余 9 组 XML，原 UI XML 未保留。
- `attempt-03*`：两图定向失败，明确重命名错误。
- `attempt-04*`：9 条界面定向检查，6 通过、3 失败。
- `attempt-05*`：扩展 API 后全量 87 次，1 处精确文本断言错误。
- `attempt-06.txt`、`final-results/`、`lint-final.xml`：最终全量通过和静态检查；`apk-inspection.txt` 为包信息与签名检查。

日志与 XML 来自实际命令；仅规范化文本编码、结尾换行、本机工作区路径及线程快照的行末空白。文件夹内不同尝试不可相加当成不同测试用例；本地运行也不等于荣耀 X50 实测。详细发现、修复、复测与未覆盖项见[执行记录](../../test-records.md)和[测试报告](../../test-report.md)。

[ci-verification.json](ci-verification.json)记录同源码[CI](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40500794045)成功，并实际核对报告中的 87 个 testcase；CI Lint 0 错误、12 警告。本机结果与 CI 结果独立保存。[APK](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40500794045/artifacts/12733885228)与[报告](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40500794045/artifacts/12733560898)属于该次运行的产物。
