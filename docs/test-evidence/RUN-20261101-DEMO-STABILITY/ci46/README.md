# CI #46：0.3.4 远程复核

运行 [41390048583](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41390048583) 对准确提交 `b95b6c6e699d0c0537ff15bc4ec00c50507ad13d` 成功，产品源码对应 `bdcd76f`。原始时间为 2026-11-02 04:43:35—04:48:42 UTC（用户 America/Los_Angeles 的 11 月 1 日）。

- [核验摘要](verified-summary.json)：28 份 JUnit XML、179 个不同方法／218 次实际执行，失败／错误／跳过均为 0。
- [测试 XML](test-results/)与[解析汇总](report-summary.json)：直接来自报告产物 13117366270，不与本机执行累计。
- [步骤](jobs.json)：构建、测试、Lint、APK 上传和报告上传均成功；Gradle 4 分 36 秒、55 项任务实际执行。
- [Lint](lint-results-debug.xml)：0 错误／17 警告；比本机多 1 项 OldTargetApi，其余 16 项一致。没有为消除提示而临时升级依赖或目标 SDK。
- [产物元数据](artifacts.json)与[文件哈希](files-hashes.json)：保存安全元数据及上述原始文件字节，没有归档凭证、签名下载参数或完整 APK。

该 CI 没有手机操作；本机安装包的签名、哈希及后续设备结果须分别核对。CI #45 的一次页面超时原记录和未定根因继续保留。
