# RUN-20261101-DEMO-STABILITY：PR 合并基线与演示稳定性

更新日期：2026-11-01（America/Los_Angeles），机器时间保留 UTC。**0.3.4 最终本机检查完成：28 份 JUnit XML、179 个不同方法／218 次执行，失败／错误／跳过各 0，Lint 0 错误／16 警告，Debug 构建成功。** 当前源码 [bdcd76f](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/bdcd76f916aae546bd146ce1fb1a14859bcd5eb4)，完整 SHA 为 `bdcd76f916aae546bd146ce1fb1a14859bcd5eb4`。手机仍为已有分项证据的 0.3.3／6，新包没有安装或实机验收。

[交付摘要](delivery.json)绑定源码提交、最终 218 次结果、APK 信息及设备未操作边界。

## 各阶段证据

| 阶段 | 证据 | 实际结果 |
| --- | --- | --- |
| PR #10 审阅合并 | [API 返回](pr10-review-merge.json)、[APPROVE 审阅](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/10#pullrequestreview-5732043244) | head `d4e09428b4deb87110067ff356c78a3cb98f4eac`，merge `c1e94f3d80fcf4aa6d9983c4e3be5e7b4e0a1d48`；7 项规划提案及记录，尚未实现 |
| 合并后本机基线 | [摘要](baseline-summary.json)、[完整基线目录](baseline/)、[执行日志](baseline/gradle.log) | 27 XML／167 方法／194 执行，失败／错误／跳过各 0，Lint 0／16；7 份先前源码与旧 APK 哈希一致 |
| 历史 CI #44 | [CI #44](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41387261004) | `a670` 前缀，completed／success；不覆盖其后失败 |
| 历史 CI #45 | [CI #45](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41387758880)、[解析摘要](ci45-summary.json)、[失败原始 XML](ci45-failed-ui.xml)、[步骤状态](ci45-jobs.json) | `c1e94f3`，169 执行／1 失败／0 错误／0 跳过，Lint 0／17；根因未定 |
| 首轮定向 | [汇总](targeted-01/summary.json)、[源码前](targeted-01/source-before.json)、[源码后](targeted-01/source-after.json)、[日志](targeted-01/gradle.log)、[XML](targeted-01/test-results/) | 8 XML／77 方法／108 执行，2 失败、0 错误／跳过；取消测试前提修正后进入最终全量 |
| 最终完整检查 | [汇总](final/summary.json)、[源码前](final/source-before.json)、[源码后](final/source-after.json)、[日志](final/gradle.log)、[XML](final/test-results/)、[Lint](final/lint-results-debug.xml)、[签名](final/apk-signature.txt)、[归档哈希](final/files-hashes.json) | **28 XML／179 方法／218 执行，失败／错误／跳过各 0，Lint 0／16**，BUILD SUCCESSFUL；源码前后字节一致 |

PR 后此前 7 份源码／测试保存在 `778429e`，后续稳定性源码为 `bdcd76f`。04:25:58 UTC 的远程快照没有开放 PR；这是当次快照。新源码的远程检查按提交在 [main 的 Android Actions](https://github.com/zidodo1124/hust26_software_engineering_homework/actions?query=branch%3Amain) 查看，本页不预填新 CI 编号或通过状态。

## 失败、修订与复审

CI #45 的 `SaverAppFlowTest.characterStylePreviewRequiresSaveAndCanBeSwitchedBackAfterRecreation` 在最后切回经典并保存后 awaitText 首页超时；此前首次保存、数据库核对、两次重建和 Q 版选中断言已通过。原 XML 没有失败瞬间的 ViewModel 状态，根因未定，不归因产品竞态、网络或偶发 CI。本轮增强保存前选中／按钮启用断言及失败诊断，不延长等待、不删测试或迎合测试修改产品；定向及最终本机通过不覆盖原失败。

首轮定向的 2 个失败为新增取消测试在两种 API 下对异常对象使用 assertSame，前提过严。修订后核查取消传播、结果分类与 finish 一次，不改变生产取消语义；原失败、源码快照与日志保留。该首轮 SaverAppFlow 的 18 次执行通过，不与全量重复累计。

静态复核发现同步诊断写盘可能拖延 Receiver 收尾的 P2。改为独立写入器后，独立复审确认原 P2 解除且无新增阻塞。独立单写入器最多保留 6 个待写来源槽和 1 个在途写入，业务恢复与 Receiver 收尾不等待磁盘；同来源中间阶段可合并，进程退出可能丢失未写记录，属于尽力留证，不承诺持久化必达。 BOOT 与后续其他触发分开保存；记录失败不影响恢复业务，完成记录不保证 Alarm 保持或通知送达，无记录也不单独证明广播未交付。

## 最终产物与边界

- 版本：0.3.4／versionCode 7、Room schema 4；包名 `cn.edu.hust.ddlsaver`。
- 运行：2026-11-02 04:35:10.510483—04:38:44.548167 UTC；Gradle 3 分 32 秒，进程测量 214.031 秒。
- 命令：`scripts/gradle.ps1 --no-daemon --console=plain --stacktrace --continue --max-workers=2 :app:cleanTestDebugUnitTest :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`；退出码 0，测试确实执行。
- APK：15,119,011 字节；SHA-256 `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`。
- 签名证书 SHA-256：`978833bcaf0fc001248f5d8501bfa1f9d1273922474ed601f2c60a4548a652b9`，验证成功，与基线一致。
- Lint：警告总数仍为 16：新增恢复诊断 SharedPreferences.edit 的 1 条 UseKtx，原 ReminderRuntime 的 1 条 InlinedApi 消失，不能称为全部既有警告。

已实现来源 SecurityException 的重复制／重选提示、保持草稿与批次副本回滚、调试包有界恢复诊断及重启／强停后主动打开说明。04:32 UTC ADB 没有检测到设备，本轮未安装 0.3.4，不能用手机 0.3.3 结果替代新包验收。OBS-01 授权失效机制及 OBS-02 真实重启自动恢复原因仍开放；完整 DA／TC、自然 Doze／持续锁屏及演练继续待办。

## 归档字节保持

Windows `core.autocrlf` 可能改写已记录哈希的 JSON／日志字节。本次 [.gitattributes](../../../.gitattributes) 仅对四个新证据目录（INDEPENDENT-UI、RECOVERY-DIAGNOSTICS、DEVICE-ACCEPTANCE、DEMO-STABILITY）配置 `-text`，保留原始归档字节；114 项设备安全证据的哈希已全部核对一致，提交时另核对索引字节。没有重写旧原始证据或删除失败结果。

相关入口：[测试报告](../../test-report.md#18-pr-合并基线与-034-演示稳定性2026-11-01)、[执行记录](../../test-records.md#21-run-20261101-demo-stability合并基线与演示稳定性)、[演示准备](../../demo-and-acceptance.md)、[已有 0.3.3 设备证据](../RUN-20261101-DEVICE-ACCEPTANCE/README.md)。
